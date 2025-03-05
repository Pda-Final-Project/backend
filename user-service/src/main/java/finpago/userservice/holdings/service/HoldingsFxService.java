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
            if (parts.length != 3) continue;

            double buyExchangeRate = Double.parseDouble(parts[0]); // 매수 시 환율
            int quantity = Integer.parseInt(parts[1]); // 매수 수량

            double sellAmountUSD = currentPrice * quantity; // (현재가로) 매도 금액 (KRW)
            double fxProfit = (currentExchangeRate - buyExchangeRate) * sellAmountUSD; // 환차손익 계산

            totalFxProfit += fxProfit;
        }

        return totalFxProfit;
    }

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

    /**
     * 매수금액(KRW) 계산 메서드
     * @param userId 사용자 ID
     * @param stockTicker 주식 티커 (ex. TSLA)
     * @return 매수금액 (KRW)
     */
    public double calculatePurchaseAmount(Long userId, String stockTicker) {
        // Redis Key 정의
        String holdingsFxKey = "user:" + userId + ":holdings-fx:" + stockTicker;

        // Redis 데이터 가져오기
        List<String> holdingsFxList = redisTemplate.opsForList().range(holdingsFxKey, 0, -1);
        if (holdingsFxList == null || holdingsFxList.isEmpty()) {
            return 0.0; // 데이터가 없으면 0 반환
        }

        double totalPurchaseAmount = 0.0;

        // 매수금액 (KRW) 계산
        for (String entry : holdingsFxList) {
            String[] parts = entry.split(":");
            if (parts.length != 3) continue;

            double buyExchangeRate = Double.parseDouble(parts[0]); // 매수 당시 환율
            int quantity = Integer.parseInt(parts[1]); // 매수 수량
            double buyPrice = Double.parseDouble(parts[2]); // 매수 당시 가격 (USD)

            double purchaseAmountKRW = buyPrice * quantity * buyExchangeRate; // 매수 금액 (KRW)
            totalPurchaseAmount += purchaseAmountKRW;
        }

        return totalPurchaseAmount;
    }

    /**
     * 손익등락(KRW) 계산 메서드
     * @param userId 사용자 ID
     * @param stockTicker 주식 티커 (ex. TSLA)
     * @return 손익등락 (KRW)
     */
    public double calculateTotalProfit(Long userId, String stockTicker) {
        // 매매손익 가져오기
        double tradeProfit = calculateTradeProfit(userId, stockTicker);

        // 환차손익 가져오기
        double fxProfit = calculateFxProfit(userId, stockTicker);

        // 손익등락 = 매매손익 + 환차손익
        return tradeProfit + fxProfit;
    }

    /**
     * 평가금액(KRW) 계산 메서드
     * @param userId 사용자 ID
     * @param stockTicker 주식 티커 (ex. TSLA)
     * @return 평가금액 (KRW)
     */
    public double calculateEvaluationAmount(Long userId, String stockTicker) {
        // 매수금액(KRW) 가져오기
        double purchaseAmount = calculatePurchaseAmount(userId, stockTicker);

        // 매매손익(KRW) 가져오기
        double tradeProfit = calculateTradeProfit(userId, stockTicker);

        // 환차손익(KRW) 가져오기
        double fxProfit = calculateFxProfit(userId, stockTicker);

        // 평가금액 계산
        return purchaseAmount + tradeProfit + fxProfit;
    }

    /**
     * 수익률(%) 계산 메서드
     * @param userId 사용자 ID
     * @param stockTicker 주식 티커 (ex. TSLA)
     * @return 수익률 (%)
     */
    public double calculateReturnRate(Long userId, String stockTicker) {
        // 매수금액(KRW) 가져오기
        double purchaseAmount = calculatePurchaseAmount(userId, stockTicker);

        // 매수금액이 0이면 수익률을 계산할 수 없음 (0으로 나누기 방지)
        if (purchaseAmount == 0.0) {
            return 0.0;
        }

        // 손익등락(KRW) 가져오기
        double totalProfit = calculateTotalProfit(userId, stockTicker);

        // 수익률(%) 계산
        return (totalProfit / purchaseAmount) * 100;
    }
}
