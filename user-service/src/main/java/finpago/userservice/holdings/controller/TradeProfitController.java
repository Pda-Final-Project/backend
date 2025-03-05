package finpago.userservice.holdings.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.holdings.dto.TradeProfitDto;
import finpago.userservice.holdings.service.TradeFetchService;
import finpago.userservice.holdings.service.TradeProfitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tradeProfit")
@RequiredArgsConstructor
public class TradeProfitController {

    private final TradeFetchService tradeFetchService;
    private final TradeProfitService tradeProfitService;

    /**
     * 사용자의 매도 손익 내역 리스트 조회
     * @return 매도 손익 내역 리스트 (List<TradeProfitDto>)
     */
    @GetMapping("/sell-history")
    public ResponseEntity<ApiResponse<List<TradeProfitDto>>> getUserSellHistory() {
        Long userId = getUserIdFromAuth();
        List<TradeProfitDto> tradeProfitList = tradeProfitService.getUserSellTradeProfits(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "매도 손익 내역 조회 성공", tradeProfitList));
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
