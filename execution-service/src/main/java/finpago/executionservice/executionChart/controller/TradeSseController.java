//package finpago.executionservice.executionChart.controller;
//
//import finpago.executionservice.executionChart.service.SseEmitterService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//
//@RestController
//@RequestMapping("/v1/api/trade")
//@RequiredArgsConstructor
//@Slf4j
//public class TradeSseController {
//
//    private final SseEmitterService sseEmitterService;
//
//    /**
//     * 특정 주식 티커의 실시간 체결 정보를 SSE로 구독
//     */
//    @CrossOrigin(origins = "*")
//    @GetMapping("/updates/{stockTicker}")
//    public SseEmitter getTradeUpdates(@PathVariable String stockTicker) {
//        log.info("📡 SSE 연결 요청: {}", stockTicker);
//
//        try {
//            // 타임아웃을 null로 설정하여 무제한 유지
//            SseEmitter emitter = sseEmitterService.createEmitter(stockTicker);
//            return emitter;
//        } catch (Exception e) {
//            log.error("❌ SSE 요청 중 예외 발생: {}", e.getMessage());
//
//            // 클라이언트가 SSE 오류를 감지할 수 있도록 직접 전송 후 complete()
//            SseEmitter errorEmitter = new SseEmitter(0L);
//            try {
//                errorEmitter.send("event: error\ndata: SSE 요청 중 오류 발생 - " + e.getMessage() + "\n\n");
//                errorEmitter.complete();
//            } catch (IOException ioException) {
//                errorEmitter.completeWithError(ioException);
//            }
//            return errorEmitter;
//        }
//    }
//}
