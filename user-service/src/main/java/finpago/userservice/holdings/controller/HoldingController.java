package finpago.userservice.holdings.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.holdings.service.HoldingService;
import finpago.userservice.holdings.service.HoldingsFxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/api/stocks")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingService holdingService;
    private final HoldingsFxService holdingsFxService;

    /**
     * 사용 가능 주식 조회 (해당 종목)
     */
    @GetMapping("/available-stocks/{stockTicker}")
    public ResponseEntity<ApiResponse<Long>> getAvailableStocks(@PathVariable String stockTicker) {
        Long userId = getUserIdFromAuth();
        Long availableStocks = holdingService.getAvailableStocks(userId, stockTicker);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "사용 가능 주식 조회 성공", availableStocks));
    }

    /**
     * 보유 주식 조회 (해당 종목)
     */
    @GetMapping("/holdings/{stockTicker}")
    public ResponseEntity<ApiResponse<Long>> getHoldings(@PathVariable String stockTicker) {
        Long userId = getUserIdFromAuth();
        Long holdings = holdingService.getHoldings(userId, stockTicker);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "보유 주식 조회 성공", holdings));
    }

    /**
     * 환차손익 조회 API
     * @param stockTicker 주식 티커 (ex. TSLA)
     * @return 환차손익 (KRW)
     */
    @GetMapping("/{stockTicker}/fx-profit")
    public ResponseEntity<ApiResponse<Double>> getFxProfit(@PathVariable String stockTicker) {
        Long userId = getUserIdFromAuth(); // 인증된 사용자 ID 가져오기
        double fxProfit = holdingsFxService.calculateFxProfit(userId, stockTicker);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "환차손익 조회 성공", fxProfit));
    }


    /**
     * 인증된 사용자 ID 가져오기
     */
    private Long getUserIdFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == "anonymousUser") {
            throw new IllegalStateException("인증되지 않은 사용자");
        }

        return Long.parseLong(authentication.getName());
    }


}
