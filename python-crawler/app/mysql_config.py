import pandas as pd
from sqlalchemy import create_engine, text
import json
from datetime import datetime

# JSON 파일에서 MySQL 자격 증명 불러오기
with open('mysql_credentials.json') as f:
    mysql_credentials = json.load(f)

# MySQL DB 연결 설정
db_connection_str = f"mysql+pymysql://{mysql_credentials['username']}:{mysql_credentials['password']}@{mysql_credentials['host']}/{mysql_credentials['database']}"
db_connection = create_engine(db_connection_str)

def create_finpagodb_table():
    """테이블이 존재하지 않으면 생성하는 함수"""
    create_table_sql = text("""
    CREATE TABLE IF NOT EXISTS fillings (
        filling_id VARCHAR(255) PRIMARY KEY,
        filling_title VARCHAR(255),
        filling_type VARCHAR(50),
        filling_ticker VARCHAR(50),
        filling_url TEXT,
        filling_file_type VARCHAR(50),
        filling_summary_content_url TEXT,
        filling_translated_content_url TEXT,
        filling_10q_json_url TEXT,
        submit_timestamp DATETIME,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )
    """)

    with db_connection.begin() as conn:
        conn.execute(create_table_sql)
        print("✅ 'fillings' 테이블 확인 완료 (없으면 생성됨).")

def save_df_to_mysql(df):
    try:
        # ✅ 테이블이 존재하는지 확인 후 생성
        create_finpagodb_table()

        # ✅ created_at, updated_at 컬럼 추가
        current_time = datetime.now()
        df["created_at"] = current_time
        df["updated_at"] = current_time

        # ✅ DataFrame을 딕셔너리 리스트로 변환
        data_to_insert = df.to_dict(orient="records")

        # ✅ MySQL에 INSERT IGNORE로 저장 (중복된 filling_id 무시)
        sql = text("""
        INSERT IGNORE INTO fillings 
        (filling_id, filling_title, filling_type, filling_ticker, filling_url, 
         filling_file_type, filling_summary_content_url, filling_translated_content_url, 
         filling_10q_json_url, submit_timestamp, created_at, updated_at)
        VALUES (:filling_id, :filling_title, :filling_type, :filling_ticker, :filling_url, 
                :filling_file_type, :filling_summary_content_url, :filling_translated_content_url, 
                :filling_10q_json_url, :submit_timestamp, :created_at, :updated_at)
        """)

        with db_connection.begin() as conn:
            conn.execute(sql, data_to_insert)

        print(f"✅ {len(df)}개의 데이터를 MySQL에 삽입 시도 (중복 데이터는 무시됨).")

    except Exception as e:
        print(f"❌ MySQL 저장 중 오류 발생: {e}")
        
def get_latest_filing_date_from_mysql(ticker):
    """ 특정 종목의 최신 공시 날짜를 MySQL에서 가져오기 (날짜 값만 반환) """
    try:
        with db_connection.connect() as conn:
            sql = text("SELECT MAX(submit_timestamp) FROM fillings WHERE filling_ticker = :ticker")
            result = conn.execute(sql, {"ticker": ticker}).fetchone()
            print(f"[{result[0]}] MySQL에서 최신 공시 날짜 조회")
            if result and result[0]:  # ✅ MySQL 결과가 정상적인 날짜인지 확인
                latest_date = str(result[0])  # ✅ 날짜를 문자열로 변환하여 반환
                print(f"[{ticker}] 최신 공시 날짜: {latest_date}")
                return latest_date
            else:
                print(f"[{ticker}] MySQL에서 최신 공시 날짜 없음, 기본값 설정")
                return "2025-03-01T17:15:26.000Z"  # 기본값 설정

    except Exception as e:
        print(f"🔴 [ERROR] MySQL 조회 실패: {e}")
        return "2025-03-01T17:15:26.000Z"  # 기본값 반환

