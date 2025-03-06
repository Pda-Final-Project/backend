package finpago.executionservice.execution.messaging.consumer;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
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

    @KafkaListener(topics = "buy-execution-dlt-topic", groupId = "execution-service-group")
    public void handleBuyExecutionDLT(BuyTradeMatchEvent event) {
        log.error("매수 체결 메시지 DLT 처리: {}", event);
        executionService.sendFailedBuyTradeToMatching(event);
    }

    @KafkaListener(topics = "sell-execution-dlt-topic", groupId = "execution-service-group")
    public void handleSellExecutionDLT(SellTradeMatchEvent event) {
        log.error("매도 체결 메시지 DLT 처리: {}", event);
        executionService.sendFailedSellTradeToMatching(event);
    }
}
