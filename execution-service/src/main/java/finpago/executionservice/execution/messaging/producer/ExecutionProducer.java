package finpago.executionservice.execution.messaging.producer;

import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionProducer {

    private final KafkaTemplate<String, TradeMatchingEvent> tradeMatchingKafkaTemplate;
    private final KafkaTemplate<String, OrderCreateReqEvent> orderCreateKafkaTemplate;
    private static final String TRADE_EXECUTION_TOPIC = "trade-execution-topic";
    private static final String FAILED_EXECUTION_TOPIC = "failed-execution-topic";

    public void sendTradeToSettlement(TradeMatchingEvent tradeEvent) {
        tradeMatchingKafkaTemplate.send(TRADE_EXECUTION_TOPIC, tradeEvent);
        log.info("체결된 주문을 Settlement 모듈로 전송: {}", tradeEvent);
    }

    public void sendFailedTradeToMatching(OrderCreateReqEvent failedEvent) {
        orderCreateKafkaTemplate.send(FAILED_EXECUTION_TOPIC, failedEvent);
        log.info("체결 실패 주문을 Matching 모듈로 전송: {}", failedEvent);
    }

}
