package finpago.userservice.user.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.messaging.FillingNoticeEvent;
import finpago.common.global.messaging.NoticeEvent;
import finpago.userservice.pinnedStock.repository.PinnedStockRepository;
import finpago.userservice.user.controller.SSEController;
import finpago.userservice.user.entity.User;
import finpago.userservice.user.repository.UserRepository;
import finpago.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserConsumer {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PinnedStockRepository pinnedStockRepository;
    private static final String NOTICE_TOPIC = "notice-topic";
    private static final String FILLING_NOTICE_TOPIC = "filling-notice-topic";
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = NOTICE_TOPIC, groupId = "user-service-group"
            ,containerFactory = "noticeKafkaRetryListenerContainerFactory")
    public void consumeNotification(NoticeEvent event) {
        log.info("알림 수신 - 사용자 {}: {}", event.getUserId(), event);
        userService.saveNotification(event);
    }


    @KafkaListener(topics = FILLING_NOTICE_TOPIC, groupId = "user-service-group",
            containerFactory = "fillingNoticeRetryListenerContainerFactory")
    public void consumeFillingNotice(FillingNoticeEvent event) {
        log.info("공시 알림 수신 - 티커 {}: {}", event.getTicker(), event);
        userService.saveFillingNotice(event);
    }


    @KafkaListener(topics = NOTICE_TOPIC, groupId = "user-service-group")
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

    /**
     * 관심 종목에 해당하는 공시만 알림 전송 (기존 consumeNotification과 분리)
     */
    @KafkaListener(topics = NOTICE_TOPIC, groupId = "notification-group")
    public void consumeFilteredNotification(ConsumerRecord<String, String> record) {
        try {
            // JSON 문자열을 NoticeEvent 객체로 변환
            NoticeEvent noticeEvent = objectMapper.readValue(record.value(), NoticeEvent.class);

            log.info("공시 알림 수신: {}", noticeEvent);

            Long userId = noticeEvent.getUserId();
            String stockTicker = noticeEvent.getStockTicker();

            // 사용자 정보 조회 (유효한 사용자 확인)
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("존재하지 않는 사용자 ID: {}", userId);
                return;
            }

            // MySQL에서 관심 종목 조회
            List<String> userPinnedTickers = pinnedStockRepository.findByUser(user)
                    .stream()
                    .map(pinnedStock -> pinnedStock.getStockTicker()) // 관심 종목 티커 리스트 생성
                    .collect(Collectors.toList());

            // 관심 종목에 해당하는 공시만 SSE로 전송
            if (userPinnedTickers.contains(stockTicker)) {
                log.info("관심 종목 공시 알림 전송: 사용자 {} - {}", userId, stockTicker);
                SSEController.sendNotification(userId, "[공시] " + noticeEvent.getTitle());
            } else {
                log.info("관심 종목에 해당하지 않아 알림 제외됨: 사용자 {} - {}", userId, stockTicker);
            }

        } catch (Exception e) {
            log.error("공시 메시지 변환 오류: {}", e.getMessage());
        }
    }
}
