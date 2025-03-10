package finpago.orderservice.order.messaging.consumer;
import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.orderservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "unmatched-orders-topic", groupId = "order-service-group")
    public void handleUnmatchedOrder(OrderCreateReqEvent event) {
        log.warn("미체결 주문 수신 - 다시 매칭 모듈로 전송 준비: {}", event);
        orderService.retryUnmatchedOrder(event);
    }
}
