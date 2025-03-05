package finpago.userservice.holdings.service;


import finpago.userservice.holdings.entity.Holdings;
import finpago.userservice.holdings.repository.HoldingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HoldingsTotalService {

    private final HoldingsRepository holdingsRepository;
    private final HoldingsFxService holdingsFxService;
    private final HoldingsRepository HoldingsRepository;


    /**
     * 전체 보유 주식의 총 평가금액 계산
     */
    public double calculateTotalEvaluationAmount(Long userId) {
        List<Holdings> holdingsList = holdingsRepository.findByUserId(userId);
        return holdingsList.stream()
                .mapToDouble(h -> holdingsFxService.calculateEvaluationAmount(userId, h.getStockTicker()))
                .sum();
    }

    /**
     * 전체 보유 주식의 총 손익등락 계산
     */
    public double calculateTotalProfit(Long userId) {
        List<Holdings> holdingsList = holdingsRepository.findByUserId(userId);
        return holdingsList.stream()
                .mapToDouble(h -> holdingsFxService.calculateTotalProfit(userId, h.getStockTicker()))
                .sum();
    }

    /**
     * 전체 보유 주식의 가중 평균 수익률 계산
     */
    public double calculateWeightedAverageReturnRate(Long userId) {
        List<Holdings> holdingsList = holdingsRepository.findByUserId(userId);

        double totalWeightedSum = 0.0;
        double totalInvestmentValue = holdingsList.stream()
                .mapToDouble(h -> h.getHoldingQuantity() * h.getHoldingPrice()) // 가중치 기준값: 투자금액(수량 * 매수가)
                .sum();

        if (totalInvestmentValue == 0) return 0.0; // 투자금이 없으면 수익률 0

        for (Holdings holdings : holdingsList) {
            double investmentValue = holdings.getHoldingQuantity() * holdings.getHoldingPrice();
            double weight = investmentValue / totalInvestmentValue;
            double stockReturnRate = holdingsFxService.calculateReturnRate(userId, holdings.getStockTicker());

            totalWeightedSum += weight * stockReturnRate;
        }

        return totalWeightedSum; // 가중 평균 수익률 반환
    }

    /**
     * 전체 보유 주식의 총 매수금액 계산
     */
    public double calculateTotalPurchaseAmount(Long userId) {
        List<Holdings> holdingsList = holdingsRepository.findByUserId(userId);
        return holdingsList.stream()
                .mapToDouble(h -> holdingsFxService.calculatePurchaseAmount(userId, h.getStockTicker()))
                .sum();
    }

    /**
     * 전체 보유 주식의 총 매매손익 계산
     */
    public double calculateTotalTradeProfit(Long userId) {
        List<Holdings> holdingsList = holdingsRepository.findByUserId(userId);
        return holdingsList.stream()
                .mapToDouble(h -> holdingsFxService.calculateTradeProfit(userId, h.getStockTicker()))
                .sum();
    }

    /**
     * 전체 보유 주식의 총 환차손익 계산
     */
    public double calculateTotalFxProfit(Long userId) {
        List<Holdings> holdingsList = holdingsRepository.findByUserId(userId);
        return holdingsList.stream()
                .mapToDouble(h -> holdingsFxService.calculateFxProfit(userId, h.getStockTicker()))
                .sum();
    }
}
