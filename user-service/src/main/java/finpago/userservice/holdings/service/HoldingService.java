package finpago.userservice.holdings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoldingService {

    private final StringRedisTemplate redisTemplate;
    private static final long DEFAULT_BALANCE = 10000000; // 기본 예수금
    private static final long DEFAULT_STOCKS = 100; // 기본 보유 주식 수량

    /**
     * 사용 가능 주식 조회
     */
    public Long getAvailableStocks(Long userId, String stockTicker) {
        String key = "user:" + userId + ":available_stocks:" + stockTicker;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : DEFAULT_STOCKS;
    }

    /**
     * 보유 주식 조회
     */
    public Long getHoldings(Long userId, String stockTicker) {
        String key = "user:" + userId + ":holdings:" + stockTicker;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : DEFAULT_STOCKS;
    }
}
