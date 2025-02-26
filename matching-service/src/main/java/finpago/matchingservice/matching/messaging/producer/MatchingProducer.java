package finpago.matchingservice.matching.messaging.producer;

import finpago.common.global.messaging.TradeMatchingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingProducer {

    private final KafkaTemplate<String, TradeMatchingEvent> kafkaTemplate;
    private static final String TRADE_MATCHING_TOPIC = "trade-matching-topic";

    public void sendTradeToExecution(TradeMatchingEvent tradeEvent) {
        kafkaTemplate.send(TRADE_MATCHING_TOPIC, tradeEvent);
        log.info("매칭된 주문을 Execution 모듈로 전송: {}", tradeEvent);
    }
}
