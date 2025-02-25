package finpago.settlementservice.settlement.messaging.consumer;

import finpago.settlementservice.settlement.messaging.events.TradeMatchingEvent;
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

    private static final String DLT_TOPIC = "settlement-dlt-topic";

    @KafkaListener(topics = DLT_TOPIC, groupId = "settlement-service-group")
    public void handleFailedSettlement(TradeMatchingEvent event) {
        log.warn("정산 실패 (DLT) - 체결 모듈로 롤백 요청: {}", event);
        settlementProducer.sendSettlementFailure(event);
    }
}
