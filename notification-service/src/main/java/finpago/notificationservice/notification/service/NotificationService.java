package finpago.notificationservice.notification.service;

import finpago.common.global.messaging.*;
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
    public void sendBuyOrderNotification(BuyTradeMatchEvent event) {

        NoticeEvent noticeEvent = new NoticeEvent(
                event.getBuyerUserId(),
                "[해외주식 매수 주문 체결]",
                event.getStockTicker(),
                event.getBuyerOrderQuantity(),
                event.getTradeQuantity(),
                event.getTradePrice()
        );

        notificationProducer.sendNotification(noticeEvent);
        log.info("매수자 알림 발송: {}", noticeEvent);
    }

    /**
     * 매도자 알림 생성 및 발송
     */
    public void sendSellOrderNotification(SellTradeMatchEvent event) {

        NoticeEvent noticeEvent = new NoticeEvent(
                event.getSellerUserId(),
                "[해외주식 매도 주문 체결]",
                event.getStockTicker(),
                event.getSellerOrderQuantity(),
                event.getTradeQuantity(),
                event.getTradePrice()
        );

        notificationProducer.sendNotification(noticeEvent);
        log.info("매도자 알림 발송: {}", noticeEvent);
    }

    /**
     * 공시 알림 생성 및 발송
     */
    public void sendFillingNotice(FillingNoticeEvent event) {
        notificationProducer.sendFillingNotice(event);
        log.info("공시 알림 발송: {}", event);
    }
}
