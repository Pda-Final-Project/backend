package finpago.settlementservice.settlement.messaging.producer;

import finpago.common.global.messaging.TradeMatchingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementProducer {

    private final KafkaTemplate<String, TradeMatchingEvent> kafkaTemplate;
    private static final String SETTLEMENT_TOPIC = "settlement-topic";
    private static final String SETTLEMENT_FAILURE_TOPIC = "settlement-failure-topic";

    public void sendSettlementSuccess(TradeMatchingEvent event) {
        kafkaTemplate.send(SETTLEMENT_TOPIC, event);
        log.info("정산 완료 이벤트 발행: {}", event);
    }

    public void sendSettlementFailure(TradeMatchingEvent event) {
        kafkaTemplate.send(SETTLEMENT_FAILURE_TOPIC, event);
        log.warn("정산 실패 이벤트 발행: {}", event);
    }
}
