package finpago.userservice.account.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.account.service.AccountService;
import finpago.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/api/account")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;

    /**
     * 사용 가능 예수금 조회
     */
    @GetMapping("/available-balance")
    public ResponseEntity<ApiResponse<Long>> getAvailableBalance() {
        Long userId = getUserIdFromAuth();
        Long availableBalance = accountService.getAvailableBalance(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "사용 가능 예수금 조회 성공", availableBalance));
    }

    /**
     * 실제 예수금 조회 (출금 가능 예수금)
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Long>> getBalance() {
        Long userId = getUserIdFromAuth();
        Long balance = accountService.getBalance(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "실제 예수금 조회 성공", balance));
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
