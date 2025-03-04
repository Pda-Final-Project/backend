//package finpago.executionservice.executionChart.service;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//@Slf4j
//public class SseEmitterService {
//
//    // 주식 티커별 SSE Emitter 저장
//    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
//
//    /**
//     * 주식 티커별 SSE 연결 생성
//     */
//    public SseEmitter createEmitter(String stockTicker) {
//        SseEmitter emitter = new SseEmitter(null);
//        emitters.put(stockTicker, emitter);
//
//        emitter.onCompletion(() -> emitters.remove(stockTicker));
//        emitter.onTimeout(() -> emitters.remove(stockTicker));
//
//        log.info("주식 {} SSE 연결 생성", stockTicker);
//        return emitter;
//    }
//
//    /**
//     * 체결 정보를 주식 티커별로 실시간 전송
//     */
//    public void sendTradeUpdate(String stockTicker, Long tradeQuantity, Long tradePrice) {
//        SseEmitter emitter = emitters.get(stockTicker);
//
//        if (emitter != null) {
//            try {
//                emitter.send(SseEmitter.event()
//                        .name("trade-update")
//                        .data(Map.of(
//                                "tradeQuantity", tradeQuantity,
//                                "tradePrice", tradePrice
//                        )));
//                log.info("주식 {} 체결 정보 전송: 수량 {} | 가격 {}", stockTicker, tradeQuantity, tradePrice);
//            } catch (IOException e) {
//                log.error("주식 {} 체결 정보 전송 실패", stockTicker);
//                emitters.remove(stockTicker);
//            }
//        }
//    }
//
//    @Scheduled(fixedRate = 1000) // 1초마다 실행
//    public void sendPingToClients() {
//        for (SseEmitter emitter : emitters.values()) {
//            try {
//                emitter.send(SseEmitter.event().name("ping").data("💓"));
//            } catch (IOException e) {
//                emitter.complete();
//            }
//        }
//    }
//
//}
