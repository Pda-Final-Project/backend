package finpago.userservice.holdings.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.holdings.dto.TradeSummaryDto;
import finpago.userservice.holdings.service.HoldingsFxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

//해외주식 잔고조회(원화기준)
@RestController
@RequestMapping("/api/holdings/total")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HoldingsTotalController {

    private final HoldingsFxService holdingsFxService;

    /**
     * 사용자의 전체 매도 내역 합산 정보 반환
     * @return TradeSummaryDto (합산된 손익 정보)
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TradeSummaryDto>> getUserTradeSummary() {
        Long userId = getUserIdFromAuth();
        TradeSummaryDto tradeSummary = holdingsFxService.getUserTradeSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "전체 매도 내역 합산 조회 성공", tradeSummary));
    }

    /**
     * 인증된 사용자 ID 가져오기
     */
    private Long getUserIdFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal().equals("anonymousUser")) {
            throw new IllegalStateException("인증되지 않은 사용자");
        }

        return Long.parseLong(authentication.getName());
    }
}
