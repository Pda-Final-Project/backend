package finpago.notificationservice.notification.messaging.consumer;


import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
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
    private static final String BUY_SETTLEMENT_TOPIC = "buy-settlement-topic";
    private static final String SELL_SETTLEMENT_TOPIC = "sell-settlement-topic";

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
}
