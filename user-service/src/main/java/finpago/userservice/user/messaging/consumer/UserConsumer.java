package finpago.userservice.user.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.enums.TradeStatus;
import finpago.common.global.messaging.FillingNoticeEvent;
import finpago.common.global.messaging.NoticeEvent;
import finpago.userservice.pinnedStock.repository.PinnedStockRepository;
import finpago.userservice.user.controller.SSEController;
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

    @KafkaListener(topics = NOTICE_TOPIC, groupId = "user-notice-group")
    public void consumeExecutionNotification(ConsumerRecord<String, String> record) {
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

            log.info("Kafka 체결 알림 수신: {}", noticeEvent);

            // 현재 SSE에 연결된 유저 목록 가져오기
            List<Long> connectedUsers = SSEController.getConnectedUsers();

            // 연결된 사용자 중 해당 알림을 받을 대상 필터링
            List<Long> targetUsers = connectedUsers.stream()
                    .filter(userId -> userId.equals(noticeEvent.getUserId())) // 해당 알림 대상 유저만 필터링
                    .collect(Collectors.toList());

            if (!targetUsers.isEmpty()) {
                log.info("체결 알림 SSE 전송 - 사용자: {}, 메시지: {}", targetUsers, noticeEvent.getTitle());
                SSEController.broadcastNotification(noticeEvent.getTitle(), targetUsers);
            } else {
                log.info("SSE에 연결된 사용자가 없어 체결 알림 미전송 - 사용자 ID: {}", noticeEvent.getUserId());
            }

        } catch (Exception e) {
            log.error("Kafka 체결 메시지 변환 오류: {}", e.getMessage());
        }
    }

    /**
     * 관심 종목에 해당하는 공시만 알림 전송
     */
    @KafkaListener(topics = FILLING_NOTICE_TOPIC, groupId = "user-filling-group")
    public void consumeFilteredNotification(ConsumerRecord<String, String> record) {
        try {
            // JSON 문자열 NoticeEvent 객체로 변환
            FillingNoticeEvent fillingNoticeEvent = objectMapper.readValue(record.value(), FillingNoticeEvent.class);
            log.info("공시 알림 수신: {}", fillingNoticeEvent);

            String stockTicker = fillingNoticeEvent.getTicker();

            // MySQL에서 관심 종목이 해당 티커인 유저목록 조회
            List<Long> userIds = pinnedStockRepository.findByStockTicker(stockTicker)
                    .stream()
                    .map(pinnedStock -> pinnedStock.getUser().getUserId()) // 관심종목 등록한 userId 리스트
                    .collect(Collectors.toList());

            // 현재 SSE 연결된 유저중 관심종목에 해당하는 유저만 필터링
            List<Long> connectedUsers = userIds.stream()
                    .filter(SSEController.getConnectedUsers()::contains) // SSE에 연결된 유저만 필터링
                    .collect(Collectors.toList());

            // 필터링된 유저에게만 알림 전송
            if (!connectedUsers.isEmpty()) {
                log.info("관심 종목 공시 알림 전송: {}", stockTicker);
                SSEController.broadcastNotification(  fillingNoticeEvent.getTicker()+"의 새로운 공시가 발행되었습니다!", connectedUsers);
            } else {
                log.info("관심 종목이지만 SSE 연결된 사용자가 없어 알림 제외됨: {}", stockTicker);
            }

        } catch (Exception e) {
            log.error("공시 메시지 변환 오류: {}", e.getMessage());
        }
    }
}
