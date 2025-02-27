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
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingSettlementBatchService {

    private final StringRedisTemplate redisTemplate;
    private final AccountRepository accountRepository;
    private final HoldingsRepository holdingsRepository;

    /**
     * 매일 00:00 실행 → 2일 후(D+2)에 업데이트할 데이터 저장 (예약)
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정 실행
    public void schedulePendingUpdates() {
        log.info("[D+2 예약 배치 시작] 사용자 예수금 및 보유주식 예약 업데이트");

        LocalDate executionDate = LocalDate.now().plusDays(2); // D+2 저장
        String pendingUpdateKey = "pending_update:" + executionDate;

        Set<String> balanceKeys = redisTemplate.keys("user:*:batch_balance"); // 배치 계산용 예수금 사용
        if (balanceKeys == null || balanceKeys.isEmpty()) {
            log.warn("예약할 사용자 예수금 데이터 없음");
            return;
        }

        for (String balanceKey : balanceKeys) {
            String userId = balanceKey.split(":")[1]; // user:{userId}:batch_balance
            String balanceStr = redisTemplate.opsForValue().get(balanceKey);

            if (balanceStr != null) {
                String userBalanceKey = pendingUpdateKey + ":balance:" + userId;
                redisTemplate.opsForValue().set(userBalanceKey, balanceStr, 7, TimeUnit.DAYS);
                log.info("사용자 {} 예수금 예약 저장 (D+2): {}", userId, balanceStr);
            }

            Set<String> stockKeys = redisTemplate.keys("user:" + userId + ":holdings:*");
            if (stockKeys != null) {
                for (String stockKey : stockKeys) {
                    String stockTicker = stockKey.split(":")[3];
                    String quantityStr = redisTemplate.opsForValue().get(stockKey);

                    if (quantityStr != null) {
                        String userStockKey = pendingUpdateKey + ":stocks:" + userId + ":" + stockTicker;
                        redisTemplate.opsForValue().set(userStockKey, quantityStr, 7, TimeUnit.DAYS);
                        log.info("사용자 {}의 {} 보유량 예약 저장 (D+2): {}주", userId, stockTicker, quantityStr);
                    }
                }
            }
        }

        log.info("[D+2 예약 배치 완료]");
    }

    /**
     * 매일 00:05 실행 → 2일 전(D-2)에 저장된 데이터로 실제 업데이트 수행
     */
    @Scheduled(cron = "0 5 0 * * ?") // 매일 00:05 실행
    @Transactional
    public void applyPendingUpdates() {
        LocalDate executionDate = LocalDate.now();
        String pendingUpdateKey = "pending_update:" + executionDate;

        log.info("[D+2 반영 배치 시작] {} 데이터 반영 시작", executionDate);

        Set<String> balanceKeys = redisTemplate.keys(pendingUpdateKey + ":balance:*");
        if (balanceKeys == null || balanceKeys.isEmpty()) {
            log.warn("반영할 사용자 예수금 데이터 없음");
            return;
        }

        for (String balanceKey : balanceKeys) {
            String userId = balanceKey.split(":")[2]; // pending_update:{date}:balance:{userId}
            String balanceStr = redisTemplate.opsForValue().get(balanceKey);

            if (balanceStr != null) {
                Long newBalance = Long.parseLong(balanceStr);
                accountRepository.updateAccountWithholding(Long.parseLong(userId), newBalance);
                log.info("사용자 {} 예수금 DB 업데이트 (D+2 반영): {}", userId, newBalance);
            }

            Set<String> stockKeys = redisTemplate.keys(pendingUpdateKey + ":stocks:" + userId + ":*");
            if (stockKeys != null) {
                for (String stockKey : stockKeys) {
                    String stockTicker = stockKey.split(":")[4];
                    String quantityStr = redisTemplate.opsForValue().get(stockKey);

                    if (quantityStr != null) {
                        Long newQuantity = Long.parseLong(quantityStr);
                        Long currentPrice = getCurrentStockPrice(stockTicker);
                        Long totalPrice = newQuantity * currentPrice;

                        holdingsRepository.updateHoldings(Long.parseLong(userId), stockTicker, newQuantity, totalPrice);
                        log.info("사용자 {}의 {} 보유량 DB 업데이트 (D+2 반영): {}주 (총 {}원)", userId, stockTicker, newQuantity, totalPrice);
                    }
                }
            }
        }

        // D-2 데이터 삭제 (불필요한 데이터 정리)
        redisTemplate.delete(balanceKeys);
        log.info("[D+2 반영 배치 완료]");
    }

    /**
     * Redis에서 현재 주가 가져오기
     */
    private Long getCurrentStockPrice(String stockTicker) {
        String stockPriceKey = "stock:" + stockTicker + ":price";
        String stockPriceStr = redisTemplate.opsForValue().get(stockPriceKey);

        if (stockPriceStr != null) {
            return Long.parseLong(stockPriceStr);
        }

        log.warn("Redis에 현재 주가 정보 없음: {}, 기본값(50,000원) 반환", stockTicker);
        return 50000L; // 기본값
    }
}
