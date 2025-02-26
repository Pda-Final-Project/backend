package finpago.matchingservice.matching.messaging.consumer;

import finpago.common.global.messaging.OrderCreateReqEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingDLTConsumer {

    @KafkaListener(topics = "matching-dlt-topic", groupId = "matching-service-group")
    public void handleFailedOrder(OrderCreateReqEvent failedOrder) {
        log.error("DLT에서 주문 복구 시도: {}", failedOrder);
    }
}
