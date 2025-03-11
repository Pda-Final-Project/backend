package finpago.userservice.account.service;

import finpago.userservice.account.dto.AccountInfoDto;
import finpago.userservice.account.entity.Account;
import finpago.userservice.account.repository.AccountRepository;
import finpago.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import finpago.userservice.user.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private static final long DEFAULT_BALANCE = 10000000; // 기본 예수금
    private static final long DEFAULT_STOCKS = 100; // 기본 보유 주식 수량 // 기본 예수금


    /**
     * 사용 가능 예수금 조회
     */
    public Long getAvailableBalance(Long userId) {
        String key = "user:" + userId + ":available_balance";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : DEFAULT_BALANCE;
    }

    /**
     * 실제 예수금 조회
     */
    public Long getBalance(Long userId) {
        String key = "user:" + userId + ":balance";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : DEFAULT_BALANCE;
    }

    @Transactional(readOnly = true)
    public AccountInfoDto getAccountInfo(Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 계좌 정보가 존재하지 않습니다."));

        return new AccountInfoDto(
                user.getUserName(),
                account.getAccountName(),
                account.getAccountNumber()
        );
    }
}
