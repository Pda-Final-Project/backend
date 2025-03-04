package finpago.userservice.holdings.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HoldingsFxService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 환차손익 계산 메서드
     * @param userId 사용자 ID
     * @param stockTicker 주식 티커 (ex. TSLA)
     * @return 환차손익 (KRW)
     */
    public double calculateFxProfit(Long userId, String stockTicker) {
        // Redis Key 정의
        String holdingsFxKey = "user:" + userId + ":holdings-fx:" + stockTicker;
        String exchangeRateKey = "stock:" + stockTicker + ":exchange_rate";
        String stockInfoKey = "stock:" + stockTicker;

        // Redis 데이터 가져오기
        List<String> holdingsFxList = redisTemplate.opsForList().range(holdingsFxKey, 0, -1);
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

        // 현재 환율 가져오기
        String exchangeRateStr = valueOps.get(exchangeRateKey);
        double currentExchangeRate = exchangeRateStr != null ? Double.parseDouble(exchangeRateStr) : 0.0;

        // 현재가 가져오기
        String currentPriceStr = hashOps.get(stockInfoKey, "current_price");
        double currentPrice = currentPriceStr != null ? Double.parseDouble(currentPriceStr) : 0.0;

        if (holdingsFxList == null || holdingsFxList.isEmpty() || currentExchangeRate == 0.0 || currentPrice == 0.0) {
            return 0.0; // 데이터 부족 시 0 반환
        }

        double totalFxProfit = 0.0;

        // 환차손익 계산
        for (String entry : holdingsFxList) {
            String[] parts = entry.split(":");
            if (parts.length != 2) continue;

            double buyExchangeRate = Double.parseDouble(parts[0]); // 매수 시 환율
            int quantity = Integer.parseInt(parts[1]); // 매수 수량

            double sellAmountUSD = currentPrice * quantity; // (현재가로) 매도 금액 (USD)
            double fxProfit = (currentExchangeRate - buyExchangeRate) * sellAmountUSD; // 환차손익 계산

            totalFxProfit += fxProfit;
        }

        return totalFxProfit;
    }
}
