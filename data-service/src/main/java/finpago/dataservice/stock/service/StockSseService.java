package finpago.dataservice.stock.service;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class StockSseService implements MessageListener {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 클라이언트가 SSE를 구독하면 실행됨
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        // 클라이언트가 연결을 종료하면 리스트에서 제거
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    // Redis Pub/Sub에서 메시지를 받을 때 실행됨
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonData = new String(message.getBody());
            System.out.println("Received Stock Update: " + jsonData);

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("stockUpdate")
                            .data(jsonData));
                } catch (IOException e) {
                    emitter.complete();
                    emitters.remove(emitter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
