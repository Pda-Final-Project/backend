package finpago.userservice.account.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.account.dto.BalanceDto;
import finpago.userservice.account.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/balance")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    /**
     * 예수금 상세 조회 (출금 가능 금액, D+1, D+2 예수금 포함)
     */
    @GetMapping("/details")
    public ResponseEntity<ApiResponse<BalanceDto>> getBalanceDetails() {
        Long userId = getUserIdFromAuth();
        BalanceDto balanceDto = balanceService.getUserBalance(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "예수금 상세 조회 성공", balanceDto));
    }

    /**
     * 인증된 사용자 ID 가져오기
     */
    private Long getUserIdFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증되지 않은 사용자");
        }

        return Long.parseLong(authentication.getName());
    }
}
