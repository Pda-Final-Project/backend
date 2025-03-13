package finpago.userservice.account.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.account.dto.AccountInfoDto;
import finpago.userservice.account.entity.Account;
import finpago.userservice.account.repository.AccountRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/api/account")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

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
     * 사용자 계좌 정보 조회 API
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<AccountInfoDto>> getAccountInfo() {
        Long userId = getUserIdFromAuth();
        AccountInfoDto accountInfo = accountService.getAccountInfo(userId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "계좌 정보 조회 성공", accountInfo));
    }

    /**
     * 간편 비밀번호(PIN) 인증
     */
    @PostMapping("/verify-pin")
    public ResponseEntity<ApiResponse<String>> verifyPin(@RequestBody String pin) {
        Long userId = getUserIdFromAuth();

        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 정보를 찾을 수 없습니다."));

        // 입력한 PIN, 테이블의 해시값 비교
        if (passwordEncoder.matches(pin, account.getAccountPassword())) {
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "인증 성공", "PIN 인증 완료"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."));
        }
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
