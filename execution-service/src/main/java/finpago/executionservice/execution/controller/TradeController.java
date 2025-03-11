package finpago.executionservice.execution.controller;

import finpago.common.global.common.ApiResponse;
import finpago.executionservice.execution.dto.TradeDto;
import finpago.executionservice.execution.service.ExecutionService;
import finpago.executionservice.execution.service.TradeViewService;
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
@CrossOrigin(origins = "*")
public class TradeController {

    private final ExecutionService executionService;
    private final TradeViewService tradeViewService;


    /**
     * 유저의 체결 내역 조회 API (SUCCESS, PENDING)
     */
    @GetMapping("/trades")
    public ResponseEntity<ApiResponse<List<TradeDto>>> getSuccessfulOrPendingTrades() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자"));
        }

        Long userId = Long.parseLong(authentication.getName());
        List<TradeDto> trades = tradeViewService.getSuccessfulOrPendingTrades(userId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "체결 내역 조회 성공", trades));
    }

    /**
     * 유저의 체결 내역 조회 API (UNFILLED)
     */
    @GetMapping("/trades/unfilled")
    public ResponseEntity<ApiResponse<List<TradeDto>>> getFailedTrades() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자"));
        }

        Long userId = Long.parseLong(authentication.getName());
        List<TradeDto> trades = tradeViewService.getUnfilledTrades(userId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "미체결 내역 조회 성공", trades));
    }

    /**
     * 유저의 전체 체결 내역 조회 API (SUCCESS, PENDING, FAILED 포함)
     */
    @GetMapping("/trades/all")
    public ResponseEntity<ApiResponse<List<TradeDto>>> getAllTrades() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자"));
        }

        Long userId = Long.parseLong(authentication.getName());
        List<TradeDto> trades = tradeViewService.getAllTrades(userId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "전체 체결 내역 조회 성공", trades));
    }
}
