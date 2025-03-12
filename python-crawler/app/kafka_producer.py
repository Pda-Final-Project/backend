from kafka import KafkaProducer
import json

# ✅ Kafka 설정
KAFKA_BROKER = "kafka.kafka.svc.cluster.local:9092"  # Kafka 브로커 주소 (컨테이너 내부 사용)
TOPIC = "notice-topic"  # 공시 알림용 Kafka 토픽

# Kafka Producer 설정
producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

def send_kafka_notification(ticker, filling_type):
    """Kafka로 종목과 공시 유형만 전송"""
    message = {
        "ticker": ticker,
        "filling_type": filling_type
    }
    try:
        producer.send(TOPIC, message)
        producer.flush()
        print(f"📩 Kafka 메시지 전송 완료: {message}")
    except Exception as e:
        print(f"❌ Kafka 메시지 전송 실패: {e}")
