'''
처음 주식 정보 초기화 후, redis에 넣는 코드
'''
from redis_config import redis_client
from stocks_data import stocks

# Redis에 저장하는 함수
def store_stocks_in_redis(stocks):
    for stock in stocks:
        key = f"stock:{stock['ticker']}"
        data = {
            "name": stock["name"],
            "current_price": 0,  # 기본값 설정
            "change_rate": "0",     # 기본값 설정
            "volume": 0
        }

        redis_client.hset(key, data)

    print(stocks)
    print("init stock success!")

if __name__ == "__main__":
    store_stocks_in_redis(stocks)
