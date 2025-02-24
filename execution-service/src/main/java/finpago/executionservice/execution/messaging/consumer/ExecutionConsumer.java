package finpago.executionservice.execution.messaging.consumer;

import finpago.executionservice.execution.messaging.events.TradeMatchingEvent;
import finpago.executionservice.execution.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionConsumer {

    private final ExecutionService executionService;

    @KafkaListener(topics = "trade-matching-topic", groupId = "execution-service-group")
    public void handleTradeMatching(TradeMatchingEvent event) {
        log.info("체결 요청 수신: {}", event);
        executionService.processTrade(event);
    }

    @KafkaListener(topics = "settlement-failure-topic", groupId = "execution-service-group")
    public void handleFailedSettlement(TradeMatchingEvent event) {
        log.warn("⚠️ 정산 실패 처리: {}", event);
        executionService.handleSettlementFailure(event); // ✅ 이벤트 전체 전달
    }

}
