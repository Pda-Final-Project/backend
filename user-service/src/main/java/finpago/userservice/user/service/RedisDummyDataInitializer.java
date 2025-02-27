package finpago.userservice.user.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDummyDataInitializer {

    private final StringRedisTemplate redisTemplate;

    private static final long DEFAULT_BALANCE = 1_000_000L; // 기본 예수금
    private static final long DEFAULT_STOCKS = 100L; // 기본 보유 주식 수량
    private static final long DEFAULT_PRICE = 50_000L; // 기본 종목 가격

    @PostConstruct
    public void initializeRedisDummyData() {
        log.info("[Redis 초기 더미 데이터 삽입 시작]");

        // 현재 날짜 및 D+2 날짜 계산
        String executionDate = LocalDate.now().toString();

        // 사용자 예수금 더미 데이터 (사용자 ID: 9,10)
        String balanceKey1 = "pending_update:" + executionDate + ":balance:9";
        String balanceKey2 = "pending_update:" + executionDate + ":balance:10";
        redisTemplate.opsForValue().set(balanceKey1, String.valueOf(DEFAULT_BALANCE), 10, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(balanceKey2, String.valueOf(DEFAULT_BALANCE + 500000), 10, TimeUnit.DAYS);

        // 사용자 보유 주식 더미 데이터 (TSLA, AAPL 보유)
        String stockKey1 = "pending_update:" + executionDate + ":stocks:123:TSLA";
        String stockKey2 = "pending_update:" + executionDate + ":stocks:123:AAPL";
        redisTemplate.opsForValue().set(stockKey1, String.valueOf(DEFAULT_STOCKS), 10, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(stockKey2, String.valueOf(DEFAULT_STOCKS - 10), 10, TimeUnit.DAYS);

        // 종목 가격 더미 데이터
        redisTemplate.opsForValue().set("stock:TSLA:price", String.valueOf(DEFAULT_PRICE), 30, TimeUnit.DAYS);
        redisTemplate.opsForValue().set("stock:AAPL:price", String.valueOf(DEFAULT_PRICE + 20000), 30, TimeUnit.DAYS);

        log.info("[Redis 초기 더미 데이터 삽입 완료]");
    }
}
