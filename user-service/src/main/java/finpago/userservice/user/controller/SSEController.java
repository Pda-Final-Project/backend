package finpago.userservice.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/sse")
@RequiredArgsConstructor
@Slf4j
public class SSEController {

    private static final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 토큰 기반 SSE 구독
     */
    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        Long userId = getUserIdFromAuth();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userId, emitter);
        log.info("SSE 연결됨 - 사용자 ID: {}", userId);

        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE 연결 성공"));
        } catch (IOException e) {
            log.error("SSE 초기 연결 실패 - 사용자 ID: {}", userId);
        }

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));

        return emitter;
    }

    /**
     * 현재 SSE에 연결된 사용자 목록 가져오기
     */
    public static List<Long> getConnectedUsers() {
        return List.copyOf(emitters.keySet());
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    public static void sendNotification(Long userId, String message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(message));
                log.info("SSE 알림 전송 - 사용자 ID: {}, 메시지: {}", userId, message);
            } catch (IOException e) {
                log.error("SSE 전송 실패 - 사용자 ID: {}", userId);
                emitters.remove(userId);
            }
        } else {
            log.warn("SSE 연결 없음 - 사용자 ID: {}", userId);
        }
    }

    /**
     * SSE 연결된 모든 사용자에게 알림 전송
     */
    public static void broadcastNotification(String message, List<Long> userIds) {
        List<Long> connectedUsers = userIds.stream()
                .filter(emitters::containsKey) // 현재 SSE 연결된 사용자만 필터링
                .collect(Collectors.toList());

        if (connectedUsers.isEmpty()) {
            log.info("SSE 연결된 사용자가 없어 알림 전송 안 함");
            return;
        }

        log.info("SSE 공시 알림 전송 - 대상 사용자: {}", connectedUsers);
        connectedUsers.forEach(userId -> sendNotification(userId, message));
    }

    private Long getUserIdFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal().equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자");
        }
        return Long.parseLong(authentication.getName());
    }
}
