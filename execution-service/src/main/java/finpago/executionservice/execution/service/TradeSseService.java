package finpago.executionservice.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@Service
public class TradeSseService implements MessageListener {

    private final Map<String, List<SseEmitter>> emittersByStock = new ConcurrentHashMap<>();
//    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutionService executionService;

//    /**
//     * 특정 종목에 대한 SSE 구독
//     */
//    public SseEmitter createEmitter(String stockTicker) {
//        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
//        emitters.add(emitter);
//        // 클라이언트가 연결을 종료하면 리스트에서 제거
//        emitter.onCompletion(() -> emitters.remove(emitter));
//        emitter.onTimeout(() -> emitters.remove(emitter));
//
//        return emitter;
//    }
    /**
     * 특정 종목에 대한 SSE 구독
     */
    public SseEmitter createEmitter(String stockTicker) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emittersByStock.computeIfAbsent(stockTicker, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(stockTicker, emitter));
        emitter.onTimeout(() -> removeEmitter(stockTicker, emitter));

        return emitter;
    }

    private void removeEmitter(String stockTicker, SseEmitter emitter) {
        emittersByStock.getOrDefault(stockTicker, new ArrayList<>()).remove(emitter);
    }

    /**
     * Redis Pub/Sub 메시지 수신 (새로운 체결 데이터가 들어오면 SSE 전송)
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String tradeJson = new String(message.getBody());
            JsonNode jsonNode = objectMapper.readTree(tradeJson);

            String stockTicker = jsonNode.get("trade_ticker").asText(); // 종목 티커 추출
            double currentPrice = jsonNode.get("current_price").asDouble();
            long volume = jsonNode.get("volume").asLong();
            String time = jsonNode.get("time").asText();
            String orderType = jsonNode.get("trade_type").asText();// 매수,매도 구분
            long tradeVolume = jsonNode.get("trade_volume").asLong();

            // 클라이언트에게 SSE 전송
            String tradeUpdate = objectMapper.writeValueAsString(
                    new TradeUpdate(stockTicker,currentPrice, volume, time,orderType,tradeVolume)
            );

            sendTradeUpdate(stockTicker, tradeUpdate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * SSE로 체결 내역 전송 (종목별)
     */
    public void sendTradeUpdate(String stockTicker, String tradeJson) {
        List<SseEmitter> emitters = emittersByStock.getOrDefault(stockTicker, new ArrayList<>());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("tradeUpdate")
                        .data(tradeJson));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }
//
//    // Redis Pub/Sub 메시지 수신
//    @Override
//    public void onMessage2(Message message, byte[] pattern) {
//        try {
//            String jsonData = new String(message.getBody());
//            System.out.println("🔄 Received Trade Update: " + jsonData);
//
//            // JSON 파싱하여 필요한 필드만 추출
//            JsonNode jsonNode = objectMapper.readTree(jsonData);
//            double currentPrice = jsonNode.get("current_price").asDouble();
//            long volume = jsonNode.get("volume").asLong();
//            String time = jsonNode.get("time").asText();
//            String orderType = jsonNode.get("order_type").asText();
//
//            // 클라이언트에게 SSE 전송
//            String tradeUpdate = objectMapper.writeValueAsString(
//                    new TradeUpdate(currentPrice, volume, time,orderType)
//            );
//
//            for (SseEmitter emitter : emitters) {
//                try {
//                    emitter.send(SseEmitter.event()
//                            .name("tradeUpdate")
//                            .data(tradeUpdate));
//                } catch (IOException e) {
//                    emitter.complete();
//                    emitters.remove(emitter);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    // 체결 업데이트 DTO
    private static class TradeUpdate {
        public String stockTicker;
        public double currentPrice;
        public long volume;
        public String time;
        public String orderType;
        public long tradeVolume;

        public TradeUpdate(String stockTicker, double currentPrice, long volume, String time,String orderType,long tradeVolume) {
            this.stockTicker = stockTicker;
            this.currentPrice = currentPrice;
            this.volume = volume;
            this.time = time;
            this.orderType=orderType;
            this.tradeVolume = tradeVolume;
        }
    }
}
