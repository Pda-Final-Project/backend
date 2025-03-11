package finpago.userservice.user.service;

import finpago.userservice.account.repository.AccountRepository;
import finpago.userservice.holdings.repository.HoldingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingSettlementBatchService {

    private final StringRedisTemplate redisTemplate;
    private final AccountRepository accountRepository;
    private static final long EXPIRATION_DAYS = 30;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 스케줄러: 오늘 날짜의 pending_date 데이터를 적용
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 00:00 실행
    public void applyPendingUpdates() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        Set<String> keys = redisTemplate.keys("user:*:batch_balance:" + today);

        if (keys == null || keys.isEmpty()) {
            log.info("[배치 처리] 오늘({}) 적용할 예수금 데이터 없음.", today);
            return;
        }

        for (String key : keys) {
            String userId = key.split(":")[1];
            String balanceStr = redisTemplate.opsForValue().get(key);
            String newBalance = balanceStr;
            accountRepository.updateAccountWithholding(Long.parseLong(userId), Long.parseLong(newBalance));
            log.info("사용자 {} 예수금 DB 업데이트 (D+2 반영): {}", userId, newBalance);

            String userBalanceKey = "user:" + userId + ":balance";
            redisTemplate.opsForValue().set(userBalanceKey, newBalance, EXPIRATION_DAYS, TimeUnit.DAYS);
            log.info("Redis 사용자 {} 실제 예수금 업데이트: {}", userId, newBalance);

            log.info("[배치 적용 완료] 사용자ID: {}, 적용 예수금: {}", userId, newBalance);
        }
    }
}
