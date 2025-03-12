package finpago.notificationservice.notification.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

//    public void sendNotification(NoticeEvent noticeEvent) {
//        kafkaTemplate.send(NOTICE_TOPIC, noticeEvent);
//        log.info("알림 메시지 발행 완료: {}", noticeEvent);
//    }
    public void sendNotification(NoticeEvent noticeEvent) {
        try {
            // NoticeEvent 객체를 JSON으로 직렬화하여 Kafka로 전송
            String message = objectMapper.writeValueAsString(noticeEvent);
            kafkaTemplate.send(NOTICE_TOPIC, noticeEvent);
            log.info("알림 메시지 발행 완료: {}", message);
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 직렬화 오류: {}", e.getMessage());
        }
    }
}
