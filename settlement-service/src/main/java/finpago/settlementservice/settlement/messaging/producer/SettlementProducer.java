package finpago.settlementservice.settlement.messaging.producer;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementProducer {

    private final KafkaTemplate<String, BuyTradeMatchEvent> buyTradeKafkaTemplate;
    private final KafkaTemplate<String, SellTradeMatchEvent> sellTradeKafkaTemplate;

    private static final String BUY_SETTLEMENT_TOPIC = "buy-settlement-topic";
    private static final String SELL_SETTLEMENT_TOPIC = "sell-settlement-topic";
    private static final String BUY_SETTLEMENT_FAILURE_TOPIC = "buy-settlement-failure-topic";
    private static final String SELL_SETTLEMENT_FAILURE_TOPIC = "sell-settlement-failure-topic";

    public void sendBuySettlementSuccess(BuyTradeMatchEvent event) {
        buyTradeKafkaTemplate.send(BUY_SETTLEMENT_TOPIC, event);
        log.info("매수 정산 완료 이벤트 발행: {}", event);
    }

    public void sendSellSettlementSuccess(SellTradeMatchEvent event) {
        sellTradeKafkaTemplate.send(SELL_SETTLEMENT_TOPIC, event);
        log.info("매도 정산 완료 이벤트 발행: {}", event);
    }

    public void sendBuySettlementFailure(BuyTradeMatchEvent event) {
        buyTradeKafkaTemplate.send(BUY_SETTLEMENT_FAILURE_TOPIC, event);
        log.warn("매수 정산 실패 이벤트 발행: {}", event);
    }

    public void sendSellSettlementFailure(SellTradeMatchEvent event) {
        sellTradeKafkaTemplate.send(SELL_SETTLEMENT_FAILURE_TOPIC, event);
        log.warn("매도 정산 실패 이벤트 발행: {}", event);
    }
}
