package finpago.userservice.holdings.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HoldingsTradeService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 매매손익 계산 메서드
     * @param userId 사용자 ID
     * @param stockTicker 주식 티커 (ex. TSLA)
     * @return 매매손익 (KRW)
     */
    public double calculateTradeProfit(Long userId, String stockTicker) {
        // Redis Key 정의
        String holdingsFxKey = "user:" + userId + ":holdings-fx:" + stockTicker;
        String exchangeRateKey = "stock:" + stockTicker + ":exchange_rate";
        String stockInfoKey = "stock:" + stockTicker;
        String holdingsKey = "user:" + userId + ":holdings:" + stockTicker;

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

        // 보유 주식 수량 가져오기
        String holdingsStr = valueOps.get(holdingsKey);
        long holdingsQuantity = holdingsStr != null ? Long.parseLong(holdingsStr) : 0;

        if (holdingsFxList == null || holdingsFxList.isEmpty() || currentExchangeRate == 0.0 || currentPrice == 0.0 || holdingsQuantity == 0) {
            return 0.0; // 데이터 부족 시 0 반환
        }

        double totalBuyAmount = 0.0;
        double totalSellAmount = currentPrice * holdingsQuantity * currentExchangeRate; // 매도금액 (KRW)

        // 매수금액 (KRW) 계산
        for (String entry : holdingsFxList) {
            String[] parts = entry.split(":");
            if (parts.length != 3) continue;
            int quantity = Integer.parseInt(parts[1]); // 매수 수량
            double buyPrice = Double.parseDouble(parts[2]); // 매수 당시 가격 (KRW)

            double buyAmountKRW = buyPrice * quantity ; // 매수 금액 (KRW)
            totalBuyAmount += buyAmountKRW;
        }

        // 매매손익 계산
        double tradeProfit = totalSellAmount - totalBuyAmount;
        return tradeProfit;
    }
}
