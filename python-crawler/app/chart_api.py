import requests
import json
import time
import mysql.connector
from datetime import datetime, timedelta
from stocks_data import stocks
from redis_config import redis_client as r

g_appkey = "PSKJKSd75HbuPliK70C0cUdl4OYAvyIGmROi"
g_appsecret = "ZtH0c5eLI1BJnBZ1f9LVn9ggNT7Af2NHVTOu7dMBdvxCy7OhDxnuBjKU8YkpiYXgxxHJgvuuOSbqKV/HeuQ+smMz0K88mNkiRtXGZASlotr+nMaOPCerfs/fduLBhbimdiimL4IobO6Ne0VPZEPanlFzz0x91nbDi/W8wmLOa5ZkkmRrDus="

TOKEN_KEY = "stock:access_token"
TOKEN_EXPIRATION_KEY = "stock:access_token_expiration"

# 🔍 **토큰 만료 여부 확인**
def is_token_expired():
    """Redis에 저장된 만료 시간이 현재 시간보다 이전이면 True 반환 (즉, 만료됨)"""
    expiration_time = r.get(TOKEN_EXPIRATION_KEY)
    if expiration_time:
        expiration_time = datetime.strptime(expiration_time, "%Y-%m-%d %H:%M:%S")
        return datetime.now() >= expiration_time  # 현재 시간이 만료 시간보다 크거나 같으면 만료됨
    return True  # 만료 시간이 없으면 무조건 새 토큰 발급


# 🔄 **유효한 토큰 가져오기 (토큰 발급 요청 최소화)**
def get_valid_access_token():
    """유효한 토큰을 Redis에서 가져오거나 만료되었을 경우 새로 발급"""
    if not is_token_expired():
        return r.get(TOKEN_KEY)  # 기존 토큰 반환

    # 🔄 새 토큰 발급 시도
    for attempt in range(3):  # 최대 3회 재시도
        new_token = get_access_token(g_appkey, g_appsecret)
        if new_token:
            expiration_time = datetime.now() + timedelta(minutes=59)  # 59분 후 만료
            r.set(TOKEN_KEY, new_token)
            r.set(TOKEN_EXPIRATION_KEY, expiration_time.strftime("%Y-%m-%d %H:%M:%S"))
            print(f"🔑 새 토큰 발급 완료 (유효기간: {expiration_time})")
            return new_token
        
        print(f"⚠️ [ERROR] 토큰 발급 실패 (재시도 {attempt+1}/3) - 60초 후 재시도")
        time.sleep(60)  # API 요청 제한으로 인해 1분 대기
    
    print("❌ [ERROR] 토큰 발급 3회 실패, 요청 중단")
    return None


# 🔑 **토큰 발급 (오류 처리 추가)**
def get_access_token(key, secret):
    """토큰 발급"""
    headers = {"content-type": "application/json"}
    body = {
        "grant_type": "client_credentials",
        "appkey": key, 
        "appsecret": secret
    }
    url = 'https://openapi.koreainvestment.com:9443/oauth2/tokenP'
    
    res = requests.post(url, headers=headers, data=json.dumps(body))
    
    try:
        response_json = res.json()
        print("🔍 API 응답:", response_json)  # 응답 확인

        if "access_token" in response_json:
            return response_json["access_token"]
        elif response_json.get("error_code") == "EGW00133":
            print("⏳ [ERROR] 토큰 발급 요청 제한 (1분 대기 후 재시도)")
            time.sleep(60)  # 1분 대기 후 재시도
            return None
        else:
            print("❌ [ERROR] 응답에 'access_token' 없음")
            return None
    except json.JSONDecodeError:
        print("❌ [ERROR] JSON 파싱 실패:", res.text)
        return None


access_token = get_valid_access_token()

# 환율 변환 함수
def get_exchange_rate():
    exchange_rate = r.get("stock:TSLA:exchange_rate")  # Redis에 저장된 환율 키
    print(exchange_rate)
    return float(exchange_rate) if exchange_rate else 1320.0  # 환율이 없으면 0 반환 (예외 처리)


#mysql에 저장
def save_to_mysql(data_list, stock_ticker, chart_type):
    """MySQL에 데이터를 저장하는 함수"""
    create_charts_table_if_not_exists()  # 테이블 생성
    exchange_rate = get_exchange_rate()  # 환율 가져오기

    try:
        conn = mysql.connector.connect(
            host="executiondb-cluster.db.svc.cluster.local",
            user="root",
            password="admin",
            database="finpagodb",
            port=3306
        )
        cursor = conn.cursor()

        # 📌 중복 확인 쿼리 (WHERE 절로 중복 여부 확인)
        check_query = """
        SELECT COUNT(*) FROM charts 
        WHERE report_date = %s AND stock_ticker = %s AND chart_type = %s
        """

        # 📌 실제 데이터 삽입 쿼리
        insert_query = """
        INSERT INTO charts (report_date, stock_ticker, chart_type, chart_open, chart_high, chart_low, chart_close, chart_volume, created_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NOW())
        """

        new_data_count = 0
        for record in data_list:
            report_date = datetime.strptime(record["stck_bsop_date"], "%Y%m%d")  # 날짜 변환
            values = (
                report_date,
                stock_ticker,
                chart_type,
                int(float(record["ovrs_nmix_oprc"]) * exchange_rate),
                int(float(record["ovrs_nmix_hgpr"]) * exchange_rate),
                int(float(record["ovrs_nmix_lwpr"]) * exchange_rate),
                int(float(record["ovrs_nmix_prpr"]) * exchange_rate),
                int(record["acml_vol"])
            )

            # 📌 중복 데이터 체크
            cursor.execute(check_query, (report_date, stock_ticker, chart_type))
            count = cursor.fetchone()[0]  # 중복 데이터 개수 가져오기

            if count == 0:  # 데이터가 없으면 새로 삽입
                cursor.execute(insert_query, values)
                new_data_count += 1

        conn.commit()
        print(f"✅ {new_data_count} new records inserted into MySQL for {stock_ticker} ({chart_type})")

    except mysql.connector.Error as err:
        print(f"❌ MySQL Error: {err}")
    finally:
        cursor.close()
        conn.close()

def create_charts_table_if_not_exists():
    """MySQL에 charts 테이블이 없으면 생성"""
    try:
        conn = mysql.connector.connect(
            host="executiondb-cluster.db.svc.cluster.local",
            user="root",
            password="admin",
            database="finpagodb",
            port=3306
        )
        cursor = conn.cursor()
        
        create_table_query = """
        CREATE TABLE IF NOT EXISTS charts (
            id INT AUTO_INCREMENT PRIMARY KEY,
            report_date DATE NOT NULL,
            stock_ticker VARCHAR(10) NOT NULL,
            chart_type ENUM('D', 'W', 'M') NOT NULL,
            chart_open INT NOT NULL,
            chart_high INT NOT NULL,
            chart_low INT NOT NULL,
            chart_close INT NOT NULL,
            chart_volume BIGINT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_chart (report_date, stock_ticker, chart_type)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """
        
        cursor.execute(create_table_query)
        conn.commit()
        print("✅ MySQL: 'charts' 테이블 확인 완료 (없으면 자동 생성됨)")

    except mysql.connector.Error as err:
        print(f"❌ MySQL Error (테이블 생성 실패): {err}")

    finally:
        cursor.close()
        conn.close()

# 주식 api 요청
def get_overseas_stock_price(g_appkey, g_appsecret, access_token, ticker, start_date, end_date, period_code):
    url = "https://openapi.koreainvestment.com:9443/uapi/overseas-price/v1/quotations/inquire-daily-chartprice"
    
    headers = {
        "content-type": "application/json; charset=utf-8",
        "authorization": f"Bearer {access_token}",
        "appkey": g_appkey,
        "appsecret": g_appsecret,
        "tr_id": "FHKST03030100"
    }
    
    params = {
        "FID_COND_MRKT_DIV_CODE": "N",
        "FID_INPUT_ISCD": ticker,
        "FID_INPUT_DATE_1": start_date,
        "FID_INPUT_DATE_2": end_date,
        "FID_PERIOD_DIV_CODE": period_code
    }
    
    response = requests.get(url, headers=headers, params=params)
    
    if response.status_code == 200:
        return response.json()
    else:
        return {"error": f"Failed to fetch data. Status Code: {response.status_code}", "details": response.text}

# 주식 차트 날짜 계산
def calculate_date_ranges(period_code):
    """현재 날짜 기준으로 200개의 데이터를 가져올 수 있도록 100개 단위로 분할"""
    today = datetime.today()
    date_ranges = []
    
    if period_code == "D":  # 일별 데이터 (300일 전까지 요청)
        previous_start_date = today
        for i in range(0, 300, 100):
            start_date = today - timedelta(days=i + 150)
            end_date = previous_start_date - timedelta(days=1)  # 이전 요청의 start_date - 1일
            previous_start_date = start_date  # 다음 요청을 위해 start_date 업데이트
            date_ranges.append((start_date.strftime("%Y%m%d"), end_date.strftime("%Y%m%d")))
    elif period_code == "W":  # 주별 데이터 (200주 전까지 요청)
        previous_start_date = today
        for i in range(0, 200, 100):
            start_date = today - timedelta(weeks=i + 100)
            end_date = previous_start_date - timedelta(days=1)  # 이전 요청의 start_date - 1일
            previous_start_date = start_date
            date_ranges.append((start_date.strftime("%Y%m%d"), end_date.strftime("%Y%m%d")))
    elif period_code == "M":  # 월별 데이터 (200개월 전까지 요청)
        previous_start_date = today
        for i in range(0, 200, 100):
            years, months = divmod(today.month - (i + 100), 12)
            start_date = today.replace(year=today.year + years, month=months if months > 0 else 12)
            
            end_date = previous_start_date - timedelta(days=1)  # 이전 요청의 start_date - 1일
            previous_start_date = start_date
            date_ranges.append((start_date.strftime("%Y%m%d"), end_date.strftime("%Y%m%d")))
    else:
        raise ValueError("Invalid period_code. Use 'D' (Daily), 'W' (Weekly), or 'M' (Monthly).")
    
    return date_ranges

def get_latest_date_range(period_code):
    """각 기간별 최신 데이터 1개만 가져오기 위한 start_date, end_date 계산"""
    today = datetime.today()
    today = today - timedelta(days=1)
    
    if period_code == "D":  # 일별 (오늘 데이터)
        start_date = end_date = today
    elif period_code == "W":  # 주별 (이번 주 월요일 ~ 오늘)
        start_date = today - timedelta(days=today.weekday())  # 이번 주 월요일
        end_date = today
    elif period_code == "M":  # 월별 (이번 달 1일 ~ 오늘)
        start_date = today.replace(day=1)
        end_date = today
    else:
        raise ValueError("Invalid period_code. Use 'D' (Daily), 'W' (Weekly), or 'M' (Monthly).")
    
    return [(start_date.strftime("%Y%m%d"), end_date.strftime("%Y%m%d"))]


def get_chart_data(appkey, appsecret, access_token, fid_input_iscd, period_code, mode):
    if mode == "latest":
        date_ranges = get_latest_date_range(period_code)
    elif mode == "history":
        date_ranges = calculate_date_ranges(period_code)
    else:
        raise ValueError("Invalid mode. Use 'latest' or 'history'")
    data_list = []
    
    print(f"Date ranges: {date_ranges}")  # 날짜 범위 확인

    for start_date, end_date in date_ranges:
        print(f"🔍 Requesting data from {start_date} to {end_date}...")

        retry_count = 0
        max_retries = 5  # 최대 5번 재시도
        while retry_count < max_retries:
            response = get_overseas_stock_price(appkey, appsecret, access_token, fid_input_iscd, start_date, end_date, period_code)

            # API 요청 실패 시 재시도
            if "error" in response and "EGW00201" in response.get("details", ""):
                retry_count += 1
                wait_time = retry_count * 1  # 1초, 2초, 3초 점진적 증가
                print(f"⚠️ 초당 요청 제한 초과, {wait_time}초 후 재시도 ({retry_count}/{max_retries})...")
                time.sleep(wait_time)
                continue  # 재시도
            
            # 다른 오류 발생 시 즉시 중단
            if "error" in response:
                print(f"❌ API 요청 실패: {response['error']} - {response['details']}")
                break

            # 정상 응답 처리
            if "output2" in response:
                data_list.extend(response["output2"])
                print(f"✅ Fetched {len(response['output2'])} records from {start_date} to {end_date}")
            else:
                print(f"⚠️ No data returned from {start_date} to {end_date}")

            break  # 정상 요청이면 루프 종료

        # 요청 간격을 추가하여 API 제한 방지
        time.sleep(0.5)  
    # print(fid_input_iscd, data_list)
    return data_list

# 가장 초기에 실행(과거 데이터까지 모두 가져옴)
def init_chart_data():
    for stock in stocks:
        for period_code in ["D", "W", "M"]: #D,W,M
            data = get_chart_data(g_appkey, g_appsecret, access_token, stock["ticker"], period_code, 'history')
            if data:
                save_to_mysql(data, stock["ticker"], period_code)
            time.sleep(0.5)  # API 요청 간격 조절

# 가장 최근 데이터 하나만 가져와서 추가
def update_chart_data():
    for stock in stocks:
        for period_code in ["D", "W", "M"]: #D,W,M
            data = get_chart_data(g_appkey, g_appsecret, access_token, stock["ticker"], period_code, 'latest')
            if data:
                save_to_mysql(data, stock["ticker"], period_code)
            time.sleep(0.5)  # API 요청 간격 조절

# 사용 예제 (필요한 정보를 본인의 것으로 대체해야 합니다)
if __name__ == "__main__":
    access_token = get_access_token(g_appkey, g_appsecret)
    # access_token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6IjE5YWZlYWRiLTJiNzgtNDA3NC04NTk4LTQ0NDdlMTA3YjQ5YyIsInByZHRfY2QiOiIiLCJpc3MiOiJ1bm9ndyIsImV4cCI6MTc0MTQ5Nzc3MiwiaWF0IjoxNzQxNDExMzcyLCJqdGkiOiJQU0tKS1NkNzVIYnVQbGlLNzBDMGNVZGw0T1lBdnlJR21ST2kifQ.lan9spU1VN1zqnAb0J9BGzuvRopQbw65K1yS5BJ176WA9s61rNFUBeJ0JFniykGlcJw5blEAhX4MW5jG4Ur9rg"
    print('access_token', access_token)
    fid_input_iscd = "NVDA"  # 예: 특정 종목 코드
    start_date = "20170101"  # 조회 시작 날짜 (YYYYMMDD 형식)
    end_date = "20250301"  # 조회 종료 날짜 (YYYYMMDD 형식)
    period_code = "W"  # 일(D), 주(W), 월(M), 년(Y)

    # 최신 데이터 1개만 업데이트
    # update_chart_data(g_appkey, g_appsecret, access_token, stocks)
    
    # 전체 데이터 초기에 가져오기
    init_chart_data(g_appkey, g_appsecret, access_token, stocks)