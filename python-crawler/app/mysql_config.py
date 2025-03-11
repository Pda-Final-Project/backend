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

def save_df_to_mysql(df):
    try:
        # DataFrame을 전체 출력하도록 설정
        pd.set_option('display.max_rows', None)
        pd.set_option('display.max_columns', None)
        pd.set_option('display.width', None)
        pd.set_option('display.max_colwidth', None)

        # 현재 시간을 'createdAt' 열에 추가
        df['created_at'] = datetime.now()

        # DataFrame을 MySQL 데이터베이스에 저장
        df.to_sql(name='fillings', con=db_connection, if_exists='append', index=False)
        print(f"DataFrame successfully saved to MySQL table 'fillings'")
    except Exception as e:
        print(f"Error saving DataFrame to MySQL: {e}")
        
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

