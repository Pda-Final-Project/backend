package finpago.orderservice.order.messaging.producer;


import finpago.orderservice.order.messaging.events.OrderCreateReqEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, OrderCreateReqEvent> kafkaTemplate;
    private static final String ORDER_TOPIC = "order-topic";

    public void sendOrder(OrderCreateReqEvent orderMessage) {
        kafkaTemplate.send(ORDER_TOPIC, orderMessage);
    }
}
