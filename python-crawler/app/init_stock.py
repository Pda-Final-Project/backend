'''
처음 주식 정보 초기화 후, redis에 넣는 코드
'''
from redis_config import redis_client
from stocks_data import stocks

# Redis에 저장하는 함수
def store_stocks_in_redis(redis_client, stocks):
    for stock in stocks:
        key = f"stock:{stock['ticker']}"
        data = {
            "name": stock["name"],
            "current_price": 0,  # 기본값 설정
            "change_rate": "0",     # 기본값 설정
            "volume": 0
        }
        
        # Redis 3.x 이하에서 사용할 수 있는 hmset을 사용
        redis_client.hmset(key, data)

    print("init stock success!")

# 저장 실행
store_stocks_in_redis(r, stocks)