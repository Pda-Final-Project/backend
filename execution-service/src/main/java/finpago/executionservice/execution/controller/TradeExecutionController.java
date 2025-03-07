package finpago.executionservice.execution.controller;

import finpago.executionservice.execution.service.TradeSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("v1/api/trades")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TradeExecutionController {

    private final TradeSseService tradeSseService;

    // SSE 연결 생성
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTradeUpdates() {
        System.out.println("============ 새로운 체결창 SSE 클라이언트 연결됨 =============");
        return tradeSseService.createEmitter();
    }
}
