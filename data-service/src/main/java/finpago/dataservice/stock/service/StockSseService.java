package finpago.dataservice.stock.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class StockSseService implements MessageListener {

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet(); // 중복 방지

    public SseEmitter createEmitter() {
        // 기존 Emitter 삭제 (중복 방지)
        for (SseEmitter existingEmitter : emitters) {
            existingEmitter.complete();
            emitters.remove(existingEmitter);
            log.info("[SSE] 기존 Emitter 삭제됨.");
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        log.info("[SSE] 새로운 클라이언트 연결됨. 현재 활성 Emitter 개수: {}", emitters.size());

        emitter.onCompletion(() -> {
            emitter.complete();
            emitters.remove(emitter);
            log.info("[SSE] 클라이언트 연결 종료됨. 현재 활성 Emitter 개수: {}", emitters.size());
        });

        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
            log.warn("[SSE] 클라이언트 연결 타임아웃됨. 현재 활성 Emitter 개수: {}", emitters.size());
        });

        // 5초마다 Ping 이벤트 전송 (연결 유지)
        new Thread(() -> {
            while (emitters.contains(emitter)) {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
                    log.debug("[SSE] Ping 전송 - 클라이언트 유지 중. 현재 활성 Emitter 개수: {}", emitters.size());
                    Thread.sleep(5000);
                } catch (IOException | IllegalStateException | InterruptedException e) {
                    emitter.complete();
                    emitters.remove(emitter);
                    log.warn("[SSE] Ping 전송 실패 - 클라이언트 연결이 끊어짐. 현재 활성 Emitter 개수: {}", emitters.size());
                    break;
                }
            }
        }).start();

        return emitter;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonData = new String(message.getBody());
            log.info("[SSE] Stock Update 수신: {}", jsonData);

            for (SseEmitter emitter : Set.copyOf(emitters)) { // ConcurrentModificationException 방지
                try {
                    emitter.send(SseEmitter.event().name("stockUpdate").data(jsonData));
                    log.debug("[SSE] Stock Update 전송 성공 - 데이터: {}", jsonData);
                } catch (IOException | IllegalStateException e) {
                    emitter.complete();
                    emitters.remove(emitter);
                    log.error("[SSE] 전송 실패 - Emitter 제거됨. 현재 활성 Emitter 개수: {}", emitters.size(), e);
                }
            }
        } catch (Exception e) {
            log.error("[SSE] 메시지 처리 중 오류 발생", e);
        }
    }
}