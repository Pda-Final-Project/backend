package finpago.notificationservice.notification.messaging.consumer;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDLTConsumer {

    private static final String BUY_DLT_TOPIC = "buy-notification-dlt-topic";
    private static final String SELL_DLT_TOPIC = "sell-notification-dlt-topic";

    @KafkaListener(topics = BUY_DLT_TOPIC, groupId = "notification-service-group")
    public void handleFailedBuyTradeNotification(BuyTradeMatchEvent event) {
        log.warn("매수 정산 알림 실패 (DLT) - 재시도 또는 로그 저장: {}", event);
    }

    @KafkaListener(topics = SELL_DLT_TOPIC, groupId = "notification-service-group")
    public void handleFailedSellTradeNotification(SellTradeMatchEvent event) {
        log.warn("매도 정산 알림 실패 (DLT) - 재시도 또는 로그 저장: {}", event);
    }
}
