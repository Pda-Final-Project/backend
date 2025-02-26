package finpago.settlementservice.settlement.messaging.consumer;

import finpago.settlementservice.settlement.messaging.events.TradeMatchingEvent;
import finpago.settlementservice.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementConsumer {

    private final SettlementService settlementService;

    @KafkaListener(topics = "trade-execution-topic", groupId = "settlement-service-group",
            containerFactory = "kafkaRetryListenerContainerFactory")
    public void handleTradeExecution(TradeMatchingEvent event) {
        log.info("체결된 주문 정산 모듈에서 수신: {}", event);
        settlementService.processSettlement(event);
    }
}
