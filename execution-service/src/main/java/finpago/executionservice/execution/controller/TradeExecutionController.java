package finpago.executionservice.execution.controller;

import finpago.executionservice.execution.service.TradeSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("v1/api/trades")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TradeExecutionController {

    private final TradeSseService tradeSseService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 특정 종목의 최신 20개 체결 내역을 조회하는 API
     */
    @GetMapping("/latest")
    public ResponseEntity<List<String>> getLatestTrades(@RequestParam String symbol) {
        String redisKey = "stock:" + symbol + ":purchase";
        List<String> latestTrades = redisTemplate.opsForList().range(redisKey, 0, 19);
        return ResponseEntity.ok(latestTrades);
    }

//    // SSE 연결 생성
//    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public SseEmitter streamTradeUpdates() {
//        System.out.println("============ 새로운 체결창 SSE 클라이언트 연결됨 =============");
//        return tradeSseService.createEmitter();
//    }

    /**
     * 특정 종목의 체결 내역 SSE 스트리밍
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTradeUpdates(@RequestParam String symbol) {
        System.out.println("SSE 연결됨 - 종목: " + symbol);
        return tradeSseService.createEmitter(symbol);
    }
}
