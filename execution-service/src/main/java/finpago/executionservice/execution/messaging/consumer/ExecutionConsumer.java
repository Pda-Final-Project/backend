package finpago.executionservice.execution.messaging.consumer;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.executionservice.execution.service.ExecutionService;
//import finpago.executionservice.executionChart.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionConsumer {

    private final ExecutionService executionService;
    private static final String SELL_TRADE_MATCHING_TOPIC = "sell-trade-matching";
    private static final String BUY_TRADE_MATCHING_TOPIC = "buy-trade-matching";

    private static final String BUY_SETTLEMENT_FAILURE_TOPIC = "buy-settlement-failure-topic";
    private static final String SELL_SETTLEMENT_FAILURE_TOPIC = "sell-settlement-failure-topic";

//    private final SseEmitterService sseEmitterService;

//    @KafkaListener(topics = "trade-matching-topic", groupId = "execution-service-group",
//            containerFactory = "kafkaRetryListenerContainerFactory")
//    public void handleTradeMatching(TradeMatchingEvent event) {
//        log.info("매칭 건 체결 모듈에서 수신: {}", event);
//        executionService.processTrade(event);
//    }

    @KafkaListener(topics = BUY_TRADE_MATCHING_TOPIC, groupId = "execution-service-group",
            containerFactory = "buyTradeRetryListenerContainerFactory")
    public void handleBuyTradeMatching(BuyTradeMatchEvent event) {
        log.info("매수 체결 이벤트 수신: {}", event);
        executionService.processBuyTrade(event);
    }

    @KafkaListener(topics = SELL_TRADE_MATCHING_TOPIC, groupId = "execution-service-group",
            containerFactory = "sellTradeRetryListenerContainerFactory")
    public void handleSellTradeMatching(SellTradeMatchEvent event) {
        log.info("매도 체결 이벤트 수신: {}", event);
        executionService.processSellTrade(event);
    }

    @KafkaListener(topics = BUY_SETTLEMENT_FAILURE_TOPIC, groupId = "execution-service-group",
            containerFactory = "buyTradeRetryListenerContainerFactory")
    public void sendFailedBuyTradeToMatching(BuyTradeMatchEvent event) {
        log.warn("매수 체결 실패 - Matching 모듈로 재전송: {}", event);
        executionService.handleBuySettlementFailure(event);
    }

    @KafkaListener(topics = SELL_SETTLEMENT_FAILURE_TOPIC, groupId = "execution-service-group",
            containerFactory = "sellTradeRetryListenerContainerFactory")
    public void sendFailedSellTradeToMatching(SellTradeMatchEvent event) {
        log.warn("매도 체결 실패 - Matching 모듈로 재전송: {}", event);
        executionService.handleSellSettlementFailure(event);
    }

//    @KafkaListener(topics = "settlement-failure-topic", groupId = "execution-service-group",
//            containerFactory = "kafkaRetryListenerContainerFactory")
//    public void handleFailedSettlement(TradeMatchingEvent event) {
//        log.warn("정산 실패 처리: {}", event);
//        executionService.handleSettlementFailure(event);
//    }

//    @KafkaListener(topics = "settlement-topic", groupId = "execution-service-group",
//            containerFactory = "kafkaRetryListenerContainerFactory")
//    public void consumeSettlementEvent(TradeMatchingEvent event) {
//        log.info("[체결창] : 체결 이벤트 수신 - 주식 {}: {}", event.getStockTicker(), event);
//
//        log.info("[체결창] : 체결 금액은용11 {}", event.getTradePrice());
//        // 주식 티커별 SSE 전송 (tradeQuantity, tradePrice만 포함)
//        sseEmitterService.sendTradeUpdate(event.getStockTicker(), event.getTradeQuantity(), event.getTradePrice());
//        log.info("[체결창] : 체결 금액은용22 {}", event.getTradePrice());
//    }

}
