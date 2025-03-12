'''
처음 주식 정보 초기화 후, redis에 넣는 코드
'''
import redis
from stock_data_fetcher.services.stocks_data import stocks

# Redis 연결
r = redis.Redis(host='localhost', port=6379, decode_responses=True)

# Redis에 저장하는 함수
def store_stocks_in_redis(redis_client, stocks):
    for stock in stocks:
        key = f"stock:{stock['ticker']}"
        data = {
            "name": stock["name"],
            "current_price": 0,  # 기본값 설정
            "change_rate": "0",  # 기본값 설정
            "volume": 0  # 기본값 설정
        }

        for field, value in data.items():
            redis_client.hset(key, field, value)

    print("데이터 저장 완료!")

# 저장 실행
store_stocks_in_redis(r, stocks)
