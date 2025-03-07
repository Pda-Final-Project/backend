package finpago.executionservice.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@Service
public class TradeSseService implements MessageListener {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // SSE 구독 요청 시 실행
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        // 클라이언트가 연결을 종료하면 리스트에서 제거
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    // Redis Pub/Sub 메시지 수신
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonData = new String(message.getBody());
            System.out.println("🔄 Received Trade Update: " + jsonData);

            // JSON 파싱하여 필요한 필드만 추출
            JsonNode jsonNode = objectMapper.readTree(jsonData);
            double price = jsonNode.get("price").asDouble();
            long volume = jsonNode.get("volume").asLong();
            String time = jsonNode.get("time").asText();

            // 클라이언트에게 SSE 전송
            String tradeUpdate = objectMapper.writeValueAsString(
                    new TradeUpdate(price, volume, time)
            );

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("tradeUpdate")
                            .data(tradeUpdate));
                } catch (IOException e) {
                    emitter.complete();
                    emitters.remove(emitter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 체결 업데이트 DTO
    private static class TradeUpdate {
        public double price;
        public long volume;
        public String time;

        public TradeUpdate(double price, long volume, String time) {
            this.price = price;
            this.volume = volume;
            this.time = time;
        }
    }
}
