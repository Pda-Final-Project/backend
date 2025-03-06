package finpago.userservice.user.messaging.consumer;

import finpago.common.global.messaging.NoticeEvent;
import finpago.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserConsumer {

    private final UserService userService;

    @KafkaListener(topics = "notice-topic", groupId = "user-service-group"
            ,containerFactory = "noticeKafkaRetryListenerContainerFactory")
    public void consumeNotification(NoticeEvent event) {
        log.info("알림 수신 - 사용자 {}: {}", event.getUserId(), event);
        userService.saveNotification(event);
    }
}
