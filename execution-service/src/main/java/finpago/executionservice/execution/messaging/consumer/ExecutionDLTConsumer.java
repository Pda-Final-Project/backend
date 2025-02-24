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
public class ExecutionDLTConsumer {

    private final ExecutionService executionService;
    @KafkaListener(topics = "execution-dlt-topic", groupId = "execution-service-group")
    public void handleExecutionDLT(TradeMatchingEvent event) {
        log.error("체결 메시지 DLT 처리: {}", event);

        executionService.handleSettlementFailure(event);
    }
}
