package finpago.notificationservice.notification.messaging.consumer;


import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.notificationservice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "settlement-topic", groupId = "notification-service-group")
    public void handleTradeSettlement(TradeMatchingEvent event) {
        log.info("정산 완료 이벤트 수신: {}", event);

        // 매수자 알림 생성
        notificationService.sendBuyOrderNotification(event);

        // 매도자 알림 생성
        notificationService.sendSellOrderNotification(event);
    }
}
