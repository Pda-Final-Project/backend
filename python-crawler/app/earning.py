'''
어닝콜 크롤링 및 레디스 저장
'''

import requests
import redis
import json
import time
from stocks_data import stocks
from redis_config import redis_client

# None 값을 "null"로 변환하는 함수
def safe_value(value):
    return str(value) if value is not None else "null"

# API에서 데이터 가져오기 및 Redis 저장 함수
def fetch_and_store_earnings(symbol):
    url = f"https://earnings.kr/api/earnings/{symbol}?type=results"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36",
        "Accept": "*/*",
        "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
        "Cache-Control": "no-cache",
        "Pragma": "no-cache",
        "Sec-Fetch-Dest": "empty",
        "Sec-Fetch-Mode": "cors",
        "Sec-Fetch-Site": "same-origin"
    }

    response = requests.get(url, headers=headers)

    if response.status_code != 200:
        print(f"Failed to fetch earnings for {symbol}: {response.status_code}")
        return

    earnings_data = response.json()

    if not earnings_data:
        print(f"No earnings data available for {symbol}")
        return

    # 최대 5개 데이터만 저장하도록 슬라이싱
    earnings_data = earnings_data[2:7]

    redis_key = f"stock:{symbol}:earnings"  # List 저장용 Key
    
    redis_client.delete(redis_key)

    for entry in earnings_data:
        earnings_dict = {
            "date": entry.get("date", "unknown"),
            "eps": safe_value(entry.get("eps")),
            "eps_estimated": safe_value(entry.get("epsEstimated")),
            "revenue": safe_value(entry.get("revenue")),
            "revenue_estimated": safe_value(entry.get("revenueEstimated")),
            "time": safe_value(entry.get("time")),
        }

        # JSON 형식으로 변환 후 Redis List에 추가
        redis_client.lpush(redis_key, json.dumps(earnings_dict))
        print(f"{symbol} earnings for {earnings_dict['date']} stored in Redis")


def get_earnings_for_all_stocks(stocks):
    # 모든 주식에 대해 크롤링 실행 & Redis 저장
    for stock in stocks:
        ticker = stock["ticker"]
        print(f"==========Fetching earnings data for {ticker}...===========")
        fetch_and_store_earnings(stock["ticker"])
        time.sleep(2)  # 서버 부하 방지

if __name__ == "__main__":
    get_earnings_for_all_stocks(stocks)