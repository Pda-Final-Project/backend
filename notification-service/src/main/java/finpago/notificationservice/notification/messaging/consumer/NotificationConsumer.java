package finpago.notificationservice.notification.messaging.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.NoticeEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.notificationservice.notification.controller.SSEController;
import finpago.notificationservice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;
    private static final String BUY_SETTLEMENT_TOPIC = "buy-settlement-topic";
    private static final String SELL_SETTLEMENT_TOPIC = "sell-settlement-topic";
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = BUY_SETTLEMENT_TOPIC, groupId = "notification-service-group",
            containerFactory = "buyTradeRetryListenerContainerFactory")
    public void handleBuyTradeSettlement(BuyTradeMatchEvent event) {
        log.info("매수 정산 완료 이벤트 수신: {}", event);
        notificationService.sendBuyOrderNotification(event);
    }

    @KafkaListener(topics = SELL_SETTLEMENT_TOPIC, groupId = "notification-service-group",
            containerFactory = "sellTradeRetryListenerContainerFactory")
    public void handleSellTradeSettlement(SellTradeMatchEvent event) {
        log.info("매도 정산 완료 이벤트 수신: {}", event);
        notificationService.sendSellOrderNotification(event);
    }

    @KafkaListener(topics = "notice-topic", groupId = "notification-group")
    public void consumeNotification(ConsumerRecord<String, String> record) {
        try {
            // JSON 문자열을 NoticeEvent 객체로 변환
            NoticeEvent noticeEvent = objectMapper.readValue(record.value(), NoticeEvent.class);

            log.info("Kafka 알림 수신: {}", noticeEvent);

            // SSE -> 클라이언트로 알림 전송
            SSEController.sendNotification(noticeEvent.getUserId(), noticeEvent.getTitle());
        } catch (Exception e) {
            log.error("Kafka 메시지 변환 오류: {}", e.getMessage());
        }
    }
}
