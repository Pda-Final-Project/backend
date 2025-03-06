package finpago.notificationservice.notification.service;

import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.common.global.messaging.NoticeEvent;
import finpago.notificationservice.notification.client.UserClient;
import finpago.notificationservice.notification.messaging.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationProducer notificationProducer;
    private final UserClient userClient;

    /**
     * 매수자 알림 생성 및 발송
     */
    public void sendBuyOrderNotification(TradeMatchingEvent event) {
        boolean isNotificationEnabled = userClient.getNotificationSetting(event.getBuyerUserId());

        if (!isNotificationEnabled) {
            log.info("매수자({})의 알림 설정이 OFF 상태이므로 알림을 발송하지 않음.", event.getBuyerUserId());
            return;
        }

        NoticeEvent noticeEvent = new NoticeEvent(
                event.getBuyerUserId(),
                "[해외주식 매수 주문 체결]",
                event.getStockTicker(),
                event.getBuyerOrderQuentity(),
                event.getTradeQuantity(),
                event.getTradePrice()
        );

        notificationProducer.sendNotification(noticeEvent);
        log.info("매수자 알림 발송: {}", noticeEvent);
    }

    /**
     * 매도자 알림 생성 및 발송
     */
    public void sendSellOrderNotification(TradeMatchingEvent event) {
        boolean isNotificationEnabled = userClient.getNotificationSetting(event.getSellerUserId());

        if (!isNotificationEnabled) {
            log.info("매도자({})의 알림 설정이 OFF 상태이므로 알림을 발송하지 않음.", event.getSellerUserId());
            return;
        }

        NoticeEvent noticeEvent = new NoticeEvent(
                event.getSellerUserId(),
                "[해외주식 매도 주문 체결]",
                event.getStockTicker(),
                event.getSellerOrderQuentity(),
                event.getTradeQuantity(),
                event.getTradePrice()
        );

        notificationProducer.sendNotification(noticeEvent);
        log.info("매도자 알림 발송: {}", noticeEvent);
    }
}
