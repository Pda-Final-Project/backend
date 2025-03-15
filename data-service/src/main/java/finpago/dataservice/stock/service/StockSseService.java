package finpago.dataservice.stock.service;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StockSseService implements MessageListener {
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet(); // 중복 방지
    // 클라이언트가 SSE를 구독하면 실행됨
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        // 5초마다 Ping 이벤트 전송 (연결 유지)
        new Thread(() -> {
            while (emitters.contains(emitter)) {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
                    Thread.sleep(5000);
                } catch (IOException | IllegalStateException | InterruptedException e) {
                    emitter.complete();
                    emitters.remove(emitter);
                    break;
                }
            }
        }).start();
        return emitter;
    }
    // Redis Pub/Sub에서 메시지를 받을 때 실행됨
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonData = new String(message.getBody());
            System.out.println("Received Stock Update: " + jsonData);
            for (SseEmitter emitter : Set.copyOf(emitters)) { // ConcurrentModificationException 방지
                try {
                    emitter.send(SseEmitter.event()
                            .name("stockUpdate")
                            .data(jsonData));
                } catch (IOException | IllegalStateException e) {
                    emitter.complete();
                    emitters.remove(emitter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}