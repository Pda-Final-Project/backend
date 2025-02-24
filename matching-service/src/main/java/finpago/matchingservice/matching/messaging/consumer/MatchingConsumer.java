package finpago.matchingservice.matching.messaging.consumer;

import finpago.matchingservice.matching.messaging.events.OrderCreateReqEvent;
import finpago.matchingservice.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingConsumer {

    private final MatchingService matchingService;

    @KafkaListener(topics = "order-topic", groupId = "matching-service-group")
    public void handleNewOrder(OrderCreateReqEvent order) {
        log.info("새 주문 수신: {}", order);
        matchingService.processOrder(order);
    }

    @KafkaListener(topics = "failed-execution-topic", groupId = "matching-service-group")
    public void handleFailedTrade(OrderCreateReqEvent failedOrder) {
        log.warn("체결 실패 주문 재매칭: {}", failedOrder);
        matchingService.processOrder(failedOrder); // 실패 주문을 다시 매칭 큐에 삽입
    }
}
