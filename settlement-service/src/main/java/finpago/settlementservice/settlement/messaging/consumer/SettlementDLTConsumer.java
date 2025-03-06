package finpago.settlementservice.settlement.messaging.consumer;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.settlementservice.settlement.messaging.producer.SettlementProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementDLTConsumer {

    private final SettlementProducer settlementProducer;

    private static final String BUY_DLT_TOPIC = "buy-settlement-dlt-topic";
    private static final String SELL_DLT_TOPIC = "sell-settlement-dlt-topic";

    @KafkaListener(topics = BUY_DLT_TOPIC, groupId = "settlement-service-group")
    public void handleFailedBuySettlement(BuyTradeMatchEvent event) {
        log.warn("매수 정산 실패 (DLT) - 체결 모듈로 롤백 요청: {}", event);
        settlementProducer.sendBuySettlementFailure(event);
    }

    @KafkaListener(topics = SELL_DLT_TOPIC, groupId = "settlement-service-group")
    public void handleFailedSellSettlement(SellTradeMatchEvent event) {
        log.warn("매도 정산 실패 (DLT) - 체결 모듈로 롤백 요청: {}", event);
        settlementProducer.sendSellSettlementFailure(event);
    }
}
