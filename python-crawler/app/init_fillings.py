import requests
import pandas as pd
import time

from kafka_producer import send_kafka_notification
from stocks_data import stocks
import json
from s3 import check_s3_file_exists, upload_translated_document_to_s3, upload_json_to_s3
from thread import translate_html
from json_10q import get_filtered_10q_data
from mysql_config import save_df_to_mysql
from chatGpt import get_summary_as_json

# S3 버킷 이름 설정
bucket_name = 'finpago-bucket'

headers = {
    "User-Agent": "MyApp/1.0 (myemail@example.com)",
    "Referer": "https://www.sec.gov/",
    "Accept": "application/json",
    "Accept-Encoding": "gzip, deflate, br",
    "Cache-Control": "no-cache",
    "Pragma": "no-cache",
    "Connection": "keep-alive"
}

try:
    # TICKER와 CIK 매칭을 위한 데이터 수집
    tickers_cik = requests.get("https://www.sec.gov/files/company_tickers.json", headers=headers)
    df_ticker = pd.json_normalize(pd.json_normalize(tickers_cik.json(), max_level=0).values[0])

    df_ticker["cik_str"] = df_ticker["cik_str"].astype(str).str.zfill(10)

    for stock in stocks:
        tic = stock["ticker"]
        cik = df_ticker[df_ticker['ticker'] == tic]['cik_str'].iloc[0]

        # 공시 리스트 수집
        url = f"https://data.sec.gov/submissions/CIK{cik}.json"
        response = requests.get(url, headers=headers)
        response.raise_for_status()  # 요청이 실패하면 예외 발생
        data = response.json()
        
        df_filing = pd.DataFrame(data['filings']['recent'])

        # form의 값이 특정 목록에 있는지 확인하고 없는 경우 제거
        valid_forms = [
            "SCHEDULE 13G", 
            # "SCHEDULE 13G/A",
            #  "SCHEDULE 13D/A", 
             "SCHEDULE 13D", 
             # "Form S-1",
             # "S-1",
            #  "SC 13D/A", 
             "SC 13D", 
             "SC 13G", 
            #  "SC 13G/A",
            # "Form S-1MEF",
            # "S-1MEF",
            "10-Q",
            "10-QT",
            "8-K", 
            "8-K/A",
            "Form 4", 
            "4"
            , "4/A"
        ]
        
        # # form 값에 따라 필터링
        df_filing = df_filing[df_filing['form'].isin(valid_forms)]

        # # 새로운 fillingName 열 추가
        filling_names = {
            "SC 13D/A": "지분 공시(수정)",
            "SC 13D": "지분 공시",
            "SCHEDULE 13D/A": "지분 공시(수정)",
            "SCHEDULE 13D": "지분 공시",
            "SC 13G/A": "지분 공시(수정)",
            "SC 13G": "지분 공시",
            "SCHEDULE 13G/A": "지분 공시(수정)",
            "SCHEDULE 13G": "지분 공시",
            "10-Q": "분기 보고서",
            "10-QT": "분기 보고서",
            "8-K": "수시 보고서",
            "8-K/A": "수시 보고서(수정)",
            "Form 4": "내부자 거래 공시",
            "4": "내부자 거래 공시",
            "4/A": "내부자 거래 공시(수정)"
        }

        # fillingName 열 추가
        df_filing['filling_title'] = df_filing['form'].map(filling_names)

        df_filing.insert(0, 'filling_ticker', tic)  # 가장 앞에 'ticker' 열 추가

        df_filing['filling_url'] = df_filing.apply(
            lambda row: f"https://www.sec.gov/Archives/edgar/data/{int(cik)}/{row['accessionNumber'].replace('-', '')}/{row['primaryDocument']}",
            axis=1
        )

        # 열 이름 변경
        df_filing.rename(columns={
            'accessionNumber': 'filling_id',
            'acceptanceDateTime': 'submit_timestamp',
            'form': 'filling_type'
        }, inplace=True)

        # 파일 형식 추가
        df_filing['filling_file_type'] = df_filing['primaryDocument'].apply(lambda x: x.split('.')[-1].lower())

        # 번역된 내용을 저장할 열 추가
        df_filing['filling_translated_content_url'] = None  # 새로운 열 추가

        # 요약된 내용을 저장할 열 추가
        df_filing['filling_summary_content_url'] = None  # 새로운 열 추가
        
        # 10-Q 재무재표 json 위치 열
        df_filing['filling_10q_json_url'] = None  # 새로운 열 추가

        df_filing['created_at'] = None  # 새로운 열 추가
        df_filing['updated_at'] = None  # 새로운 열 추가

        # 중복되지 않는 form 값들에 대해 각각 하나의 행만 선택
        # df_filing = df_filing.drop_duplicates(subset=['filling_type'])
        
        # fillingDate 기준으로 내림차순 정렬
        df_filing = df_filing.sort_values(by="submit_timestamp", ascending=False)

        # 각 filling_type 별로 3개씩만 유지 / 10Q,10QT는 하나만 유지
        df_filing = (
            df_filing.groupby("filling_type", group_keys=False)
            .apply(lambda x: x.head(1) if x['filling_type'].iloc[0] in ["10-Q", "10-QT"] else x.head(3))
        )

        # 최근 100개만 남기고 나머지 삭제
        # df_filing = df_filing.head(100)

        df_filing = df_filing[['filling_id', 'filling_title', 'filling_type', 'filling_ticker', 'filling_url', 'filling_file_type', 'filling_summary_content_url', 'filling_translated_content_url', 'filling_10q_json_url', 'submit_timestamp', 'created_at', 'updated_at']]

        # 번역된 파일 형식 추적
        translated_files = {
            "xml": False, "htm": False  # 각 파일 형식별 번역 여부 저장
        }

        for index, row in df_filing.iterrows():
            file_type = row['filling_file_type']  # 확장자 확인

            # 번역 대상이 아닌 경우 건너뜀
            if file_type not in translated_files:
                continue

            # S3에 요약 파일이 이미 존재하는지 확인
            s3_key = f"fillings/summary/{row['filling_id']}.json"
            if not check_s3_file_exists(s3_key):
                print(f"요약 파일이 존재하지 않음: {s3_key}")
                filling_url = row['filling_url']
                summary_json = get_summary_as_json(filling_url,row['filling_type'], headers)
                file_url = upload_json_to_s3(summary_json, s3_key)
                df_filing.at[index, 'filling_summary_content_url'] = file_url
            else:
                df_filing.at[index, 'filling_summary_content_url'] = f"https://{bucket_name}.s3.amazonaws.com/{s3_key}"


            # S3에 번역 파일이 이미 존재하는지 확인
            s3_key = f"fillings/{row['filling_id']}.html"
            if not check_s3_file_exists(s3_key):
                filling_url = row['filling_url']
                # HTML 파일 처리 로직
                translated_html = translate_html(filling_url, headers)
                html_url = upload_translated_document_to_s3(s3_key, translated_html)
                # html_url = "번역 초과로 인한 처리 불가"
                df_filing.at[index, 'filling_translated_content_url'] = html_url
            else:
                df_filing.at[index, 'filling_translated_content_url'] = f"https://{bucket_name}.s3.amazonaws.com/{s3_key}"

            # filling_type이 10-Q 또는 10-QT인 경우 XBRL API를 사용하여 JSON 파일 생성 및 업로드
            json_file_name = f"{row['filling_id']}.json"
            json_key = f"fillings/json/{json_file_name}"
            s3_key = "fillings/json/" + row['filling_id'] + ".json"
            if row['filling_type'] in ['10-Q', '10-QT']:
                try:
                    # S3에 JSON 파일이 이미 존재하는지 확인
                    if not check_s3_file_exists(json_key):
                        json_1oq = get_filtered_10q_data(row['filling_id'])
                        json_url = upload_json_to_s3(json_1oq, s3_key)
                        # json_url = "sec api 초과로 인한 10-Q JSON 처리 불가"
                        # df_filing.at[index, 'filling_10q_json_url'] = json_url
                    else:
                        df_filing.at[index, 'filling_10q_json_url'] = f"https://{bucket_name}.s3.amazonaws.com/{s3_key}"

                except Exception as e:
                    print(f"An error occurred while processing 10-Q/10-QT: {e}")

        # ✅ Kafka 알림 메시지 전송
        for index, row in df_filing.iterrows():
            send_kafka_notification(row['filling_ticker'], row['filling_type'])  # ✅ Kafka 토픽 전송

        # MySQL 저장
        save_df_to_mysql(df_filing)

except requests.exceptions.RequestException as e:
    print(f"Request failed: {e}")
except ValueError as e:
    print(f"JSON decode error: {e}")
except Exception as e:
    print(f"An error occurred: {e}")