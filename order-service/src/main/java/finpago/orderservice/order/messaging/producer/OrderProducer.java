package finpago.orderservice.order.messaging.producer;

import finpago.orderservice.order.messaging.events.OrderCreateReqEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {

    private static final String ORDER_TOPIC = "order-topic";
    private final KafkaTemplate<String, OrderCreateReqEvent> kafkaTemplate;

    public void sendOrder(OrderCreateReqEvent order) {
        kafkaTemplate.send(ORDER_TOPIC, order.getOfferNumber().toString(), order)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 메시지 발행 실패: {}", order.getOfferNumber(), ex);
                    } else {
                        log.info("Kafka 메시지 발행 성공: {}, 파티션: {}",
                                order.getOfferNumber(), result.getRecordMetadata().partition());
                    }
                });
    }
}

