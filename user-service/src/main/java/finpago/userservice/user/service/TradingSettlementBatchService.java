package finpago.userservice.user.service;

import finpago.userservice.account.repository.AccountRepository;
import finpago.userservice.holdings.repository.HoldingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingSettlementBatchService {

    private final StringRedisTemplate redisTemplate;
    private final AccountRepository accountRepository;
    private final HoldingsRepository holdingsRepository;

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정 실행
    @Transactional
    public void updateUserBalancesAndHoldings() {
        log.info("[배치 시작] 사용자 예수금 및 보유주식 업데이트");

        // Redis에서 모든 사용자 조회 (예: user:{userId}:balance 키 조회)
        Set<String> balanceKeys = redisTemplate.keys("user:*:balance");
        if (balanceKeys == null || balanceKeys.isEmpty()) {
            log.warn("Redis에 저장된 사용자 예수금 데이터 없음");
            return;
        }

        for (String balanceKey : balanceKeys) {
            String userId = balanceKey.split(":")[1]; // user:{userId}:balance

            // 예수금 업데이트
            String balanceStr = redisTemplate.opsForValue().get(balanceKey);
            if (balanceStr != null) {
                Long newBalance = Long.parseLong(balanceStr);
                accountRepository.updateAccountWithholding(Long.parseLong(userId), newBalance);
                log.info("사용자 {} 예수금 업데이트: {}", userId, newBalance);
            }

            // 보유 주식 업데이트
            Set<String> stockKeys = redisTemplate.keys("user:" + userId + ":stocks:*");
            if (stockKeys != null) {
                for (String stockKey : stockKeys) {
                    String stockTicker = stockKey.split(":")[3]; // user:{userId}:stocks:{ticker}
                    String quantityStr = redisTemplate.opsForValue().get(stockKey);

                    if (quantityStr != null) {
                        Long newQuantity = Long.parseLong(quantityStr);
                        Long currentPrice = getCurrentStockPrice(stockTicker);
                        Long totalPrice = newQuantity * currentPrice;

                        holdingsRepository.updateHoldings(Long.parseLong(userId), stockTicker, newQuantity, totalPrice);
                        log.info("사용자 {}의 {} 보유량 업데이트: {}주 (총 {}원)", userId, stockTicker, newQuantity, totalPrice);
                    }
                }
            }
        }

        log.info("[배치 완료] 사용자 예수금 및 보유주식 업데이트 성공");
    }

    private Long getCurrentStockPrice(String stockTicker) {
        String stockPriceKey = "stock:" + stockTicker + ":price";
        String stockPriceStr = redisTemplate.opsForValue().get(stockPriceKey);

        if (stockPriceStr != null) {
            return Long.parseLong(stockPriceStr);
        }

        log.warn("Redis에 현재 주가 정보 없음: {}, 기본값(50,000원) 반환", stockTicker);
        return 50000L; // 기본값: 50,000원
    }

}
