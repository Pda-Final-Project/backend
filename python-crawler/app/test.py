from kafka_producer import send_kafka_notification

# ✅ 테스트 데이터 (예: 테슬라 8-K 공시)
send_kafka_notification("TSLA", "8-K")
send_kafka_notification("AAPL", "10-Q")
