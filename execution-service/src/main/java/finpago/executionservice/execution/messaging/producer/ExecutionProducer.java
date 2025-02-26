package finpago.executionservice.execution.messaging.producer;

import finpago.executionservice.OrderStatus;
import finpago.executionservice.OrderType;
import finpago.executionservice.execution.messaging.events.OrderCreateReqEvent;
import finpago.executionservice.execution.messaging.events.TradeMatchingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TRADE_EXECUTION_TOPIC = "trade-execution-topic";
    private static final String FAILED_EXECUTION_TOPIC = "failed-execution-topic";

    public void sendTradeToSettlement(TradeMatchingEvent tradeEvent) {
        kafkaTemplate.send(TRADE_EXECUTION_TOPIC, tradeEvent);
        log.info("체결된 주문을 Settlement 모듈로 전송: {}", tradeEvent);
    }

    public void sendFailedTradeToMatching(OrderCreateReqEvent failedEvent) {
        kafkaTemplate.send(FAILED_EXECUTION_TOPIC, failedEvent);
        log.info("체결 실패 주문을 Matching 모듈로 전송: {}", failedEvent);
    }

}
