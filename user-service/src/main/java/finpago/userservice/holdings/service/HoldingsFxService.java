package finpago.userservice.holdings.service;

import finpago.userservice.holdings.dto.TradeSummaryDto;
import finpago.userservice.holdings.entity.Holdings;
import finpago.userservice.holdings.repository.HoldingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

//해외주식 잔고 (원화기준)
@Service
@RequiredArgsConstructor
public class HoldingsFxService {

    private final HoldingsRepository holdingsRepository;
    private final StringRedisTemplate redisTemplate;
    private final TradeFetchService tradeFetchService;

    /**
     * 사용자의 전체 매도 내역을 합산하여 하나의 DTO로 반환
     * @param userId 사용자 ID
     * @return TradeSummaryDto (전체 합산된 값)
     */
    public TradeSummaryDto getUserTradeSummary(Long userId) {
        // 사용자의 전체 매도 체결 내역 가져오기
        List<TradeFetchService.SellTrade> sellTrades = tradeFetchService.getUserSellTrades(userId);
        System.out.println("sell trades " + sellTrades);

        // 합산 값 초기화
        double totalEvaluationAmount = 0.0; //평가금액
        double totalProfitChange = 0.0; //손익등락
        double totalBuyAmount = 0.0; //매수금액
        double totalTradeProfit = 0.0; //매매손익
        double totalFxProfit = 0.0; //환차손익

        // 모든 Trade를 순회하면서 합산
        for (TradeFetchService.SellTrade trade : sellTrades) {
            totalEvaluationAmount += calculateEvaluationAmount(userId, trade);
            totalProfitChange += calculateProfitChange(userId, trade);
            totalBuyAmount += calculateBuyAmount(userId, trade.getTradeTicker());
            totalTradeProfit += calculateTradeProfit(userId, trade);
            totalFxProfit += calculateFxProfit(userId, trade);
        }

        // 수익률 계산 (가중 평균 수익률)
        double totalReturnRate = (totalBuyAmount == 0) ? 0.0 : (totalProfitChange / totalBuyAmount) * 100;

        // DTO 생성 및 반환
        return TradeSummaryDto.builder()
                .evaluationAmount(totalEvaluationAmount)
                .profitChange(totalProfitChange)
                .returnRate(totalReturnRate)
                .buyAmount(totalBuyAmount)
                .tradeProfit(totalTradeProfit)
                .fxProfit(totalFxProfit)
                .build();
    }

    /**
     * 환차손익(KRW) 계산 메서드 (단일 Trade 객체)
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return 환차손익 (KRW)
     */
    public double calculateFxProfit(Long userId, TradeFetchService.SellTrade trade) {
        if (!trade.getSellerUserId().equals(userId)) {
            return 0.0; // 매도한 거래가 아니면 0 반환
        }

        // 매도 시 환율(KRW/USD) 가져오기
        double sellExchangeRate = trade.getTradeExchangeRate();

        // 매도한 종목의 holdings 데이터 가져오기
        Optional<Holdings> holdingsOptional = holdingsRepository.findByUserIdAndStockTicker(userId, trade.getTradeTicker());
        if (holdingsOptional.isEmpty()) {
            return 0.0; // holdings 데이터가 없으면 환차손익 0 반환
        }

        Holdings holdings = holdingsOptional.get();

        // 매수 시 환율(KRW/USD) 가져오기
        double buyExchangeRate = holdings.getExchangeRate();

        // Redis에서 현재가 가져오기
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        String stockInfoKey = "stock:" + trade.getTradeTicker();
        String currentPriceStr = hashOps.get(stockInfoKey, "current_price");
        double currentPrice = currentPriceStr != null ? Double.parseDouble(currentPriceStr) : 0.0;

        // 현재가로 매도 금액(KRW) 계산
        double sellAmountCurrentPrice = currentPrice * trade.getTradeQuantity();

        // 환차손익(KRW) 계산
        return (sellExchangeRate - buyExchangeRate) * sellAmountCurrentPrice;
    }

    /**
     * 매매손익(KRW) 계산 메서드 (단일 Trade 객체)
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return 매매손익 (KRW)
     */
    public double calculateTradeProfit(Long userId, TradeFetchService.SellTrade trade) {
        if (!trade.getSellerUserId().equals(userId)) {
            return 0.0; // 매도한 거래가 아니면 0 반환
        }

        // 매도 금액(KRW) 계산
        double sellAmount = trade.getTradePrice() * trade.getTradeQuantity();

        // 매도한 종목의 holdings 데이터 가져오기
        Optional<Holdings> holdingsOptional = holdingsRepository.findByUserIdAndStockTicker(userId, trade.getTradeTicker());
        if (holdingsOptional.isEmpty()) {
            return 0.0; // holdings 데이터가 없으면 매수 금액을 구할 수 없으므로 0 반환
        }

        Holdings holdings = holdingsOptional.get();

        // 매수 금액(KRW) 계산
        double buyAmount = holdings.getHoldingPrice() * holdings.getHoldingQuantity();

        // 매매손익(KRW) 계산
        return sellAmount - buyAmount;
    }

    /**
     * 매수금액(KRW) 계산 메서드
     * @param userId 사용자 ID
     * @param stockTicker 종목 코드
     * @return 매수금액 (KRW)
     */
    public double calculateBuyAmount(Long userId, String stockTicker) {
        // 매도한 종목의 holdings 데이터 가져오기
        System.out.println("들어옵니다111");
        Optional<Holdings> holdingsOptional = holdingsRepository.findByUserIdAndStockTicker(userId, stockTicker);
        System.out.println("holdings: " + holdingsOptional);
        if (holdingsOptional.isEmpty()) {
            return 0.0; // holdings 데이터가 없으면 0 반환
        }

        Holdings holdings = holdingsOptional.get();

        // 매수금액(KRW) 계산
        return holdings.getHoldingPrice() * holdings.getHoldingQuantity();
    }

    /**
     * 손익 등락(KRW) 계산 메서드
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return 손익 등락 (KRW)
     */
    public double calculateProfitChange(Long userId, TradeFetchService.SellTrade trade) {
        // 매매손익(KRW) 계산
        double tradeProfit = calculateTradeProfit(userId, trade);

        // 환차손익(KRW) 계산
        double fxProfit = calculateFxProfit(userId, trade);

        // 손익 등락(KRW) 계산
        return tradeProfit + fxProfit;
    }

    /**
     * 평가금액(KRW) 계산 메서드
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return 평가금액 (KRW)
     */
    public double calculateEvaluationAmount(Long userId, TradeFetchService.SellTrade trade) {
        System.out.println("trade " + trade.getTradeTicker());
        // 매수금액(KRW) 계산
        double buyAmount = calculateBuyAmount(userId, trade.getTradeTicker());

        // 매매손익(KRW) 계산
        double tradeProfit = calculateTradeProfit(userId, trade);

        // 환차손익(KRW) 계산
        double fxProfit = calculateFxProfit(userId, trade);

        // 평가금액(KRW) 계산
        return buyAmount + tradeProfit + fxProfit;
    }

    /**
     * 수익률(%) 계산 메서드
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return 수익률 (%)
     */
    public double calculateReturnRate(Long userId, TradeFetchService.SellTrade trade) {
        // 손익 등락(KRW) 계산
        double profitChange = calculateProfitChange(userId, trade);

        // 매수 금액(KRW) 계산
        double buyAmount = calculateBuyAmount(userId, trade.getTradeTicker());

        // 수익률(%) 계산 (0으로 나누기 방지)
        return (buyAmount == 0) ? 0.0 : (profitChange / buyAmount) * 100;
    }
}
