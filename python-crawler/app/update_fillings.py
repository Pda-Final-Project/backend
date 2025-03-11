import requests
import pandas as pd
import time
import redis
from s3 import check_s3_file_exists, upload_translated_document_to_s3, upload_json_to_s3
from thread import translate_html
from json_10q import get_filtered_10q_data
from mysql_config import save_df_to_mysql, get_latest_filing_date_from_mysql
from redis_config import redis_client

redis_client = RedisCluster(startup_nodes=startup_nodes, decode_responses=True)

# S3 버킷 설정
bucket_name = 'finpago-bucket'

# HTTP 요청 헤더 설정
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36',
    'Accept-Language': 'ko,en-US;q=0.9,en;q=0.8',
    'Accept-Encoding': 'gzip, deflate, br, zstd',
    'Connection': 'keep-alive',
    'Cache-Control': 'max-age=0',
    'Priority': 'u=0, i',
    'Sec-CH-UA': '"Not(A:Brand";v="99", "Google Chrome";v="133", "Chromium";v="133"',
    'Sec-CH-UA-Mobile': '?0',
    'Sec-CH-UA-Platform': '"Windows"',
    'Sec-Fetch-Dest': 'document',
    'Sec-Fetch-Mode': 'navigate',
    'Sec-Fetch-Site': 'none',
    'Sec-Fetch-User': '?1',
    'Upgrade-Insecure-Requests': '1'
}

# 모니터링할 종목 리스트
tickers = ['TSLA', 'AAPL', 'NVDA']

# TICKER와 CIK 매칭 데이터 수집
tickers_cik = requests.get("https://www.sec.gov/files/company_tickers.json", headers=headers)
df_ticker = pd.json_normalize(pd.json_normalize(tickers_cik.json(), max_level=0).values[0])
df_ticker["cik_str"] = df_ticker["cik_str"].astype(str).str.zfill(10)

# 처리할 공시 유형
valid_forms = ["SCHEDULE 13G", "SCHEDULE 13D", "Form S-1", "S-1", "SC 13D", "SC 13G",
               "Form S-1MEF", "S-1MEF", "10-Q", "10-QT", "8-K", "Form 4", "4"]

# 공시 유형 설명
filling_names = {
    "SC 13D": "지분 공시", "SCHEDULE 13D": "지분 공시",
    "SC 13G": "지분 공시", "SCHEDULE 13G": "지분 공시",
    "Form S-1": "예비 증권 거래 신고서", "S-1": "예비 증권 거래 신고서",
    "Form S-1MEF": "예비 증권 거래 신고서", "S-1MEF": "예비 증권 거래 신고서",
    "10-Q": "분기 보고서", "10-QT": "분기 보고서",
    "8-K": "수시 보고서",
    "Form 4": "내부자 거래 공시", "4": "내부자 거래 공시"
}

for tic in tickers:
cik = df_ticker[df_ticker['ticker'] == tic]['cik_str'].iloc[0]

# Redis에서 최신 공시 날짜 가져오기
redis_key = f"stock:{tic}:latest_filing_date"
latest_filing_date = redis_client.get(redis_key)

# Redis에 값이 없으면 MySQL에서 가져와 저장
if latest_filing_date is None:
    print(f"[{tic}] Redis에 저장된 공시 날짜 없음, MySQL에서 조회 중...")
    latest_filing_date = get_latest_filing_date_from_mysql(tic)

    if latest_filing_date:
        redis_client.set(redis_key, latest_filing_date)
        print(f"[{tic}] MySQL에서 가져온 최신 공시 날짜를 Redis에 저장: {latest_filing_date}")
    else:
        latest_filing_date = "2025-03-01T17:15:26.000Z"
        redis_client.set(redis_key, latest_filing_date)
        print(f"[{tic}] MySQL에서도 공시 데이터 없음, 기본값 '2025-03-01T17:15:26.000Z' 설정")
else:
    print(f"[{tic}] Redis에서 최신 공시 날짜 가져옴: {latest_filing_date}")

# SEC에서 최신 공시 가져오기
url = f"https://data.sec.gov/submissions/CIK{cik}.json"
response = requests.get(url, headers=headers)
response.raise_for_status()
data = response.json()

df_filing = pd.DataFrame(data['filings']['recent'])

# 필터링: 유효한 공시 유형만 남김
df_filing = df_filing[df_filing['form'].isin(valid_forms)]

# 📌 최신 공시 시간 이후의 공시만 필터링
df_filing = df_filing[pd.to_datetime(df_filing['acceptanceDateTime']) > pd.to_datetime(latest_filing_date)]

if df_filing.empty:
    print(f"[{tic}] 새로운 공시 없음")
    continue

# 데이터 처리 및 저장
df_filing['filling_title'] = df_filing['form'].map(filling_names)
df_filing.insert(0, 'filling_ticker', tic)
df_filing['filling_url'] = df_filing.apply(
    lambda row: f"https://www.sec.gov/Archives/edgar/data/{int(cik)}/{row['accessionNumber'].replace('-', '')}/{row['primaryDocument']}",
    axis=1
)
df_filing.rename(columns={'accessionNumber': 'filling_id', 'filingDate': 'submit_timestamp', 'form': 'filling_type'}, inplace=True)
df_filing['filling_file_type'] = df_filing['primaryDocument'].apply(lambda x: x.split('.')[-1].lower())
df_filing['filling_translated_content_url'] = None
df_filing['filling_10q_json_url'] = None
df_filing['filling_8k_json_url'] = None
df_filing['created_at'] = None  # 새로운 열 추가
df_filing['updated_at'] = None  # 새로운 열 추가
df_filing['filling_summary_content_url'] = None  # 새로운 열 추가

df_filing = df_filing[['filling_id', 'filling_title', 'filling_type', 'filling_ticker', 'filling_url', 'filling_file_type', 'filling_summary_content_url', 'filling_translated_content_url', 'filling_10q_json_url', 'filling_8k_json_url', 'submit_timestamp', 'created_at', 'updated_at']]

for index, row in df_filing.iterrows():
    # 번역 처리
    file_type = row['filling_file_type']
    if file_type in ["xml", "htm", "txt"]:
        # S3에 요약 파일이 이미 존재하는지 확인
        s3_key = f"fillings/summary/{row['filling_id']}.json"
        if not check_s3_file_exists(s3_key):
            filling_url = row['filling_url']
            summary_json = get_summary_as_json(filling_url,row['filling_type'], headers)
            file_url = upload_json_to_s3(summary_json, s3_key)
            df_filing.at[index, 'filling_summary_content_url'] = file_url
        else:
            df_filing.at[index, 'filling_summary_content_url'] = f"https://{bucket_name}.s3.amazonaws.com/{s3_key}"
        
        s3_key = f"fillings/{row['filling_id']}.html"
        if not check_s3_file_exists(s3_key):
            filling_url = row['filling_url']
            if file_type == 'txt':
                # txt 파일 처리 로직 추가
                response = requests.get(filling_url, headers=headers)
                response.raise_for_status()
                original_text = response.text
                translated_text = translate_texts_google([original_text])
                translated_html = f"<html><body><pre>{translated_text[0]}</pre></body></html>"
                html_url = upload_translated_document_to_s3(s3_key, translated_html)
                # html_url = "번역 초과로 인한 처리 불가"
            else:
                # HTML 파일 처리 로직
                translated_html = translate_html(filling_url, headers)
                html_url = upload_translated_document_to_s3(s3_key, translated_html)
                # html_url = "번역 초과로 인한 처리 불가"
        else:
            df_filing.at[index, 'filling_translated_content_url'] = f"https://{bucket_name}.s3.amazonaws.com/{s3_key}"

    # 10-Q JSON 처리
    json_key = f"fillings/json/{row['filling_id']}.json"
    if row['filling_type'] in ['10-Q', '10-QT']:
        if not check_s3_file_exists(json_key):
            json_10q = get_filtered_10q_data(row['filling_id'])
            json_url = upload_json_to_s3(json_10q, json_key)
            df_filing.at[index, 'filling_10q_json_url'] = json_url
            # df_filing.at[index, 'filling_10q_json_url'] = "SEC API 초과로 인한 10-Q JSON 처리 불가"
            # print(f"SEC API 초과로 인한 10-Q JSON 처리 불가")
        else:
            df_filing.at[index, 'filling_10q_json_url'] = f"https://{bucket_name}.s3.amazonaws.com/{json_key}"

# MySQL 저장
save_df_to_mysql(df_filing)

latest_datetime = df_filing['submit_timestamp'].max()
redis_client.set(redis_key, latest_datetime)

print(f"✅ [{tic}] 새로운 공시 {len(df_filing)}건 저장 완료")

except requests.exceptions.RequestException as e:
    print(f"🔴 [ERROR] 요청 실패: {e}")
except ValueError as e:
    print(f"🔴 [ERROR] JSON 파싱 오류: {e}")
except Exception as e:
    print(f"🔴 [ERROR] 일반 오류 발생: {e}")
