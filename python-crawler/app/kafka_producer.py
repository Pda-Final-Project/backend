from kafka import KafkaProducer
import json
import time

# ✅ Kafka 설정
KAFKA_BROKER = "kafka.kafka.svc.cluster.local:9092"  # Kafka 브로커 주소
TOPIC = "notice-topic"  # ✅ 공시 알림용 Kafka 토픽
MAX_RETRIES = 5  # ✅ 최대 재시도 횟수
RETRY_DELAY = 5  # ✅ 재시도 대기 시간 (초)

# ✅ Kafka Producer 설정
producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

def send_kafka_notification(ticker, filling_type):
    """📩 Kafka로 종목과 공시 유형 전송 (Producer) - 재시도 기능 포함"""
    message = {
        "ticker": ticker,
        "filling_type": filling_type
    }

    for attempt in range(MAX_RETRIES):
        try:
            producer.send(TOPIC, message)
            producer.flush()
            print(f"📩 Kafka 메시지 전송 완료: {message}")
            return  # ✅ 성공하면 함수 종료

        except Exception as e:
            print(f"❌ Kafka 메시지 전송 실패 (시도 {attempt + 1}/{MAX_RETRIES}): {e}")

            if attempt < MAX_RETRIES - 1:  # 마지막 재시도 전까지만 대기
                print(f"⏳ {RETRY_DELAY}초 후 재시도...")
                time.sleep(RETRY_DELAY)
            else:
                print("🚨 최대 재시도 횟수 초과! Kafka 메시지 전송 실패.")

# ✅ 예제 실행
send_kafka_notification("AAPL", "8-K")
