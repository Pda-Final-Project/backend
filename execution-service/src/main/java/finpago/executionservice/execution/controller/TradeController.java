package finpago.executionservice.execution.controller;

import finpago.common.global.common.ApiResponse;
import finpago.executionservice.execution.entity.Trade;
import finpago.executionservice.execution.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/execution")
@RequiredArgsConstructor
public class TradeController {

    private final ExecutionService executionService;

    /**
     * 유저의 체결 내역 조회 API
     */
    @GetMapping("/trades")
    public ResponseEntity<ApiResponse<List<Trade>>> getUserTrades() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자"));
        }

        Long userId = Long.parseLong(authentication.getName());
        List<Trade> trades = executionService.getUserTrades(userId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "체결 내역 조회 성공", trades));
    }
}
