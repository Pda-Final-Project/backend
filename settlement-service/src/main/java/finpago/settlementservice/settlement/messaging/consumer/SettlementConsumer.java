package finpago.settlementservice.settlement.messaging.consumer;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.settlementservice.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementConsumer {

    private final SettlementService settlementService;
    private static final String BUY_TRADE_EXECUTION_TOPIC = "buy-trade-execution-topic";
    private static final String SELL_TRADE_EXECUTION_TOPIC = "sell-trade-execution-topic";

    @KafkaListener(topics = BUY_TRADE_EXECUTION_TOPIC, groupId = "settlement-service-group",
            containerFactory = "buyTradeRetryListenerContainerFactory")
    public void handleTradeExecution(BuyTradeMatchEvent event) {
        log.info("매수 체결된 주문 정산 모듈에서 수신: {}", event);
        settlementService.processBuySettlement(event);
    }

    @KafkaListener(topics = SELL_TRADE_EXECUTION_TOPIC, groupId = "settlement-service-group",
            containerFactory = "sellTradeRetryListenerContainerFactory")
    public void handleTradeExecution(SellTradeMatchEvent event) {
        log.info("매도 체결된 주문 정산 모듈에서 수신: {}", event);
        settlementService.processSellSettlement(event);
    }
}
