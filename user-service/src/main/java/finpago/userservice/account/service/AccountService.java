package finpago.userservice.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

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
}
