package finpago.notificationservice.notification.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.messaging.FillingNoticeEvent;
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
    private static final String FILLING_NOTICE_TOPIC = "filling-notice-topic";
    private final KafkaTemplate<String, FillingNoticeEvent> fillingkafkaTemplate;
    private final KafkaTemplate<String, NoticeEvent> kafkaTemplate;

    public void sendNotification(NoticeEvent noticeEvent) {
        kafkaTemplate.send(NOTICE_TOPIC, noticeEvent);
        log.info("알림 메시지 발행 완료: {}", noticeEvent);
    }

    public void sendFillingNotice(FillingNoticeEvent event) {
        fillingkafkaTemplate.send(FILLING_NOTICE_TOPIC, event);
        log.info("공시 알림 메시지 발행 완료: {}", event);
    }

}
