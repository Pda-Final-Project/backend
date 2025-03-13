from kafka import KafkaProducer
import json
import time

# ✅ Kafka 설정
KAFKA_BROKER = "kafka.kafka.svc.cluster.local:9092"  # Kafka 브로커 주소
TOPIC = "filling-notice"  # ✅ 공시 알림용 Kafka 토픽
RETRY_DELAY = 5  # ✅ 재시도 대기 시간 (초)


def create_kafka_producer():
    """📌 Kafka Producer 생성 (무한 재시도)"""
    while True:
        try:
            producer = KafkaProducer(
                bootstrap_servers=[KAFKA_BROKER],
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )
            print("✅ Kafka Producer 연결 성공!")
            return producer

        except Exception as e:
            print(f"❌ Kafka Producer 연결 실패: {e}")
            print(f"⏳ {RETRY_DELAY}초 후 다시 시도...")
            time.sleep(RETRY_DELAY)


# ✅ Kafka Producer 생성 (무한 재시도)
producer = create_kafka_producer()


def send_kafka_notification(ticker, filling_type):
    """📩 Kafka로 종목과 공시 유형 전송 (Producer) - 무한 재시도 기능 포함"""
    message = {
        "ticker": ticker,
        "filling_type": filling_type
    }

    while True:
        try:
            producer.send(TOPIC, message)
            producer.flush()
            print(f"📩 Kafka 메시지 전송 완료: {message}")
            return  # ✅ 성공하면 함수 종료

        except Exception as e:
            print(f"❌ Kafka 메시지 전송 실패: {e}")
            print(f"⏳ {RETRY_DELAY}초 후 다시 시도...")
            time.sleep(RETRY_DELAY)


# ✅ 예제 실행
send_kafka_notification("AAPL", "8-K")
