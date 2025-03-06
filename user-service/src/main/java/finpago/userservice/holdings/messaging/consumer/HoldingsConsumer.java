package finpago.userservice.holdings.messaging.consumer;

import finpago.common.global.messaging.BuyTradeMatchEvent;
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
    private static final String BUY_SETTLEMENT_TOPIC = "buy-settlement-topic";

    @KafkaListener(topics = BUY_SETTLEMENT_TOPIC, groupId = "holding-service-group",
            containerFactory = "buyTradeKafkaRetryListenerContainerFactory")
    public void handleTradeExecution(BuyTradeMatchEvent event) {
        log.info("매수 체결된 주문 유저 모듈에서 수신: {}", event);
        holdingService.updateHoldings(event);
    }
}
