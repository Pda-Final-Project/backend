package finpago.userservice.holdings.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.holdings.dto.UserHoldingsDto;
import finpago.userservice.holdings.service.HoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//보유종목 조회 데이터 반환
@Slf4j
@RestController
@RequestMapping("/v1/api/stocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HoldingController {

    private final HoldingService holdingService;

    /**
     * 사용 가능 주식 조회 (해당 종목)
     */
    @GetMapping("/available-stocks/{stockTicker}")
    public ResponseEntity<ApiResponse<Long>> getAvailableStocks(@PathVariable String stockTicker) {
        Long userId = getUserIdFromAuth();
        Long availableStocks = holdingService.getAvailableStocks(userId, stockTicker);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "사용 가능 주식 수 조회 성공", availableStocks));
    }

    /**
     * 보유 주식 조회 (해당 종목)
     */
    @GetMapping("/holdings/{stockTicker}")
    public ResponseEntity<ApiResponse<Long>> getHoldings(@PathVariable String stockTicker) {
        Long userId = getUserIdFromAuth();
        Long holdings = holdingService.getHoldings(userId, stockTicker);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "보유 주식 수 조회 성공", holdings));
    }

    /**
     * 사용자의 보유 주식 정보 조회 (정렬 조건 추가)
     * @param sortBy 정렬 조건 (profit: 수익률순, evaluation: 평가금액순, buyAmount: 매수금액순)
     * @return List<UserHoldingsDto> (보유 주식 정보 리스트)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserHoldingsDto>>> getUserHoldings(
            @RequestParam(required = false, defaultValue = "evaluation") String sortBy) {

        Long userId = getUserIdFromAuth();
        List<UserHoldingsDto> userHoldings = holdingService.getUserHoldings(userId, sortBy);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "보유 주식 정보 조회 성공", userHoldings));
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
