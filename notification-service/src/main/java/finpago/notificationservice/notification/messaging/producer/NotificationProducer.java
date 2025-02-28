package finpago.notificationservice.notification.messaging.producer;

import finpago.common.global.messaging.NoticeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private static final String NOTICE_TOPIC = "notice-topic";
    private final KafkaTemplate<String, NoticeEvent> kafkaTemplate;

    public void sendNotification(NoticeEvent noticeEvent) {
        kafkaTemplate.send(NOTICE_TOPIC, noticeEvent);
        log.info("알림 메시지 발행 완료: {}", noticeEvent);
    }
}
