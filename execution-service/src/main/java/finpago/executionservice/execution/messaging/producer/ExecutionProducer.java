package finpago.executionservice.execution.messaging.producer;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionProducer {

    private final KafkaTemplate<String, BuyTradeMatchEvent> buyTradeKafkaTemplate;
    private final KafkaTemplate<String, SellTradeMatchEvent> sellTradeKafkaTemplate;
    private final KafkaTemplate<String, OrderCreateReqEvent> orderCreateKafkaTemplate;

    private static final String FAILED_EXECUTION_TOPIC = "failed-execution-topic";
    private static final String BUY_TRADE_EXECUTION_TOPIC = "buy-trade-execution-topic";
    private static final String SELL_TRADE_EXECUTION_TOPIC = "sell-trade-execution-topic";

    public void sendBuyTradeToSettlement(BuyTradeMatchEvent tradeEvent) {
        buyTradeKafkaTemplate.send(BUY_TRADE_EXECUTION_TOPIC, tradeEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("매수 체결 주문 Settlement 모듈 전송 실패: {}", ex.getMessage());
                    } else {
                        log.info("매수 체결 주문을 Settlement 모듈로 전송 완료: {}, 파티션: {}",
                                tradeEvent, result.getRecordMetadata().partition());
                    }
                });
    }

    public void sendSellTradeToSettlement(SellTradeMatchEvent tradeEvent) {
        sellTradeKafkaTemplate.send(SELL_TRADE_EXECUTION_TOPIC, tradeEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("매도 체결 주문 Settlement 모듈 전송 실패: {}", ex.getMessage());
                    } else {
                        log.info("매도 체결 주문을 Settlement 모듈로 전송 완료: {}, 파티션: {}",
                                tradeEvent, result.getRecordMetadata().partition());
                    }
                });
    }

    public void sendFailedTradeToMatching(OrderCreateReqEvent failedEvent) {
        orderCreateKafkaTemplate.send(FAILED_EXECUTION_TOPIC, failedEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("체결 실패 주문 Matching 모듈 전송 실패: {}", ex.getMessage());
                    } else {
                        log.info("체결 실패 주문을 Matching 모듈로 전송 완료: {}, 파티션: {}",
                                failedEvent, result.getRecordMetadata().partition());
                    }
                });
    }


}
