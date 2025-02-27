package finpago.dataservice.stock.service;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class StockSseService implements MessageListener {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String data = new String(message.getBody());

        // Redis 이벤트를 SSE로 전송
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("stockUpdate")
                        .data(objectMapper.readValue(data, Map.class)));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        });
    }
}
