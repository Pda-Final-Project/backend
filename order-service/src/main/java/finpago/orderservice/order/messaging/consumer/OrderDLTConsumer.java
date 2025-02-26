package finpago.orderservice.order.messaging.consumer;

import finpago.common.global.enums.OrderStatus;
import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.orderservice.order.entity.Order;
import finpago.orderservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDLTConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "order-dlt-topic", groupId = "order-service-group")
    public void handleFailedOrder(OrderCreateReqEvent event) {
        log.warn("DLT에서 주문 복구 시도: {}", event.getOfferNumber());

        UUID orderId = event.getOfferNumber();
        Optional<Order> orderOptional = orderRepository.findById(orderId);

        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            order.setOfferStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            log.warn("Kafka 메시지 발행 실패: 주문 상태 FAILED로 변경됨: {}", orderId);
        } else {
            log.error("DLT에서 처리 실패: 해당 주문을 찾을 수 없음: {}", orderId);
        }
    }
}
