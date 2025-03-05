package finpago.userservice.holdings.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.holdings.service.HoldingsTotalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holdings/total")
@RequiredArgsConstructor
public class HoldingsTotalController {

    private final HoldingsTotalService holdingsTotalService;

    /**
     * 전체 평가금액 조회 API
     */
    @GetMapping("/evaluation-amount")
    public ResponseEntity<ApiResponse<Double>> getTotalEvaluationAmount() {
        Long userId = getUserIdFromAuth();
        double totalEvaluationAmount = holdingsTotalService.calculateTotalEvaluationAmount(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "전체 평가금액 조회 성공", totalEvaluationAmount));
    }

    /**
     * 전체 손익등락 조회 API
     */
    @GetMapping("/total-profit")
    public ResponseEntity<ApiResponse<Double>> getTotalProfit() {
        Long userId = getUserIdFromAuth();
        double totalProfit = holdingsTotalService.calculateTotalProfit(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "전체 손익등락 조회 성공", totalProfit));
    }

    /**
     * 전체 수익률 조회 API (가중 평균)
     */
    @GetMapping("/return-rate")
    public ResponseEntity<ApiResponse<Double>> getWeightedReturnRate() {
        Long userId = getUserIdFromAuth();
        double returnRate = holdingsTotalService.calculateWeightedAverageReturnRate(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "전체 가중 평균 수익률 조회 성공", returnRate));
    }

    /**
     * 전체 매수금액 조회 API
     */
    @GetMapping("/purchase-amount")
    public ResponseEntity<ApiResponse<Double>> getTotalPurchaseAmount() {
        Long userId = getUserIdFromAuth();
        double purchaseAmount = holdingsTotalService.calculateTotalPurchaseAmount(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "전체 매수금액 조회 성공", purchaseAmount));
    }

    /**
     * 전체 매매손익 조회 API
     */
    @GetMapping("/trade-profit")
    public ResponseEntity<ApiResponse<Double>> getTotalTradeProfit() {
        Long userId = getUserIdFromAuth();
        double tradeProfit = holdingsTotalService.calculateTotalTradeProfit(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "전체 매매손익 조회 성공", tradeProfit));
    }

    /**
     * 전체 환차손익 조회 API
     */
    @GetMapping("/fx-profit")
    public ResponseEntity<ApiResponse<Double>> getTotalFxProfit() {
        Long userId = getUserIdFromAuth();
        double fxProfit = holdingsTotalService.calculateTotalFxProfit(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "전체 환차손익 조회 성공", fxProfit));
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
