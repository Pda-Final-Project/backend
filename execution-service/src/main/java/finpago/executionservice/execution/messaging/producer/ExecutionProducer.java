package finpago.executionservice.execution.messaging.producer;

import finpago.executionservice.execution.messaging.events.TradeMatchingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionProducer {

    private final KafkaTemplate<String, TradeMatchingEvent> kafkaTemplate;
    private static final String TRADE_EXECUTION_TOPIC = "trade-execution-topic";

    public void sendTradeToSettlement(TradeMatchingEvent tradeEvent) {
        kafkaTemplate.send(TRADE_EXECUTION_TOPIC, tradeEvent);
        log.info("체결된 주문을 Settlement 모듈로 전송: {}", tradeEvent);
    }
}
