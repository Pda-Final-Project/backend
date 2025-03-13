package finpago.userservice.user.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.messaging.NoticeEvent;
import finpago.userservice.user.controller.SSEController;
import finpago.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserConsumer {

    private final UserService userService;
    private static final String NOTICE_TOPIC = "notice-topic";
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = NOTICE_TOPIC, groupId = "user-service-group"
            ,containerFactory = "noticeKafkaRetryListenerContainerFactory")
    public void consumeNotification(NoticeEvent event) {
        log.info("알림 수신 - 사용자 {}: {}", event.getUserId(), event);
        userService.saveNotification(event);
    }

    @KafkaListener(topics = "notice-topic", groupId = "notification-group")
    public void consumeNotification(ConsumerRecord<String, String> record) {
        try {
            String message = record.value();

            if (message == null || message.trim().isEmpty()) {
                log.warn("수신된 Kafka 메시지가 비어 있음");
                return;
            }

            // JSON 문자열을 NoticeEvent 객체로 변환
            NoticeEvent noticeEvent = objectMapper.readValue(record.value(), NoticeEvent.class);

            if (noticeEvent.getUserId() == null) {
                log.warn("userId가 null입니다. 데이터 확인 필요: {}", message);
                return;
            }

            log.info("Kafka 알림 수신: {}", noticeEvent);

            // SSE -> 클라이언트로 알림 전송
            SSEController.sendNotification(noticeEvent.getUserId(), noticeEvent.getTitle());
        } catch (Exception e) {
            log.error("Kafka 메시지 변환 오류: {}", e.getMessage());
        }
    }
}
