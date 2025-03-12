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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
     * 특정 유저에게 알림 보내기
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

    private Long getUserIdFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal().equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자");
        }
        return Long.parseLong(authentication.getName());
    }
}
