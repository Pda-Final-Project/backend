package finpago.userservice.holdings.messaging.consumer;

import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.userservice.holdings.service.HoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldingsConsumer {

    private final HoldingService holdingService;

    @KafkaListener(topics = "trade-execution-topic", groupId = "holding-service-group",
            containerFactory = "kafkaRetryListenerContainerFactory")
    public void handleTradeExecution(TradeMatchingEvent event) {
        log.info("체결된 주문 유저 모듈에서 수신: {}", event);
        holdingService.updateHoldings(event);
    }
}
