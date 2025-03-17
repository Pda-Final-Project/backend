package finpago.userservice.holdings.service;

import finpago.userservice.holdings.dto.TradeProfitDto;
import finpago.userservice.holdings.dto.TradeProfitSumDto;
import finpago.userservice.holdings.entity.Holdings;
import finpago.userservice.holdings.repository.HoldingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//매도 손익 내역 List관련 메소드들
@Service
@RequiredArgsConstructor
public class TradeProfitService {

    private final HoldingsRepository holdingsRepository;
    private final TradeFetchService tradeFetchService;
    private final HoldingsFxService holdingsFxService;

    /**
     * 사용자의 매도 손익 내역을 계산하여 DTO 리스트로 반환
     * @param userId 사용자 ID
     * @return List<TradeProfitDto> (매도 손익 내역 리스트)
     */
    public List<TradeProfitDto> getUserSellTradeProfits(Long userId) {
        // 사용자의 전체 매도 체결 내역 가져오기
        List<TradeFetchService.SellTrade> sellTrades = tradeFetchService.getUserSellTrades(userId);

        // 각 Trade 객체에 대해 손익 데이터를 계산하고 DTO로 변환
        return sellTrades.stream()
                .map(trade -> TradeProfitDto.builder()
                        .sellDateTime(trade.getTradeDate())
                        .stockTicker(trade.getTradeTicker())
                        .realizedProfit(calculateRealizedProfit(userId, trade))
                        .returnRate(calculateTradeReturnRate(userId, trade))
                        .sellAveragePrice(calculateAverageTradePrices(userId, trade)[0])
                        .buyAveragePrice(calculateAverageTradePrices(userId, trade)[1])
                        .sellAmount(calculateTradeVolumes(userId, trade)[0])
                        .buyAmount(calculateTradeVolumes(userId, trade)[1])
                        .sellQuantity(calculateTradeVolumes(userId, trade)[2])
                        .buyQuantity(calculateTradeVolumes(userId, trade)[3])
                        .sellExchangeRate(calculateTradeExchangeRates(userId, trade)[0])
                        .buyExchangeRate(calculateTradeExchangeRates(userId, trade)[1])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 전체 매도 손익 내역을 합산하여 하나의 DTO로 반환
     * @param userId 사용자 ID
     * @return TradeProfitSumDto (전체 합산된 값)
     */
    public TradeProfitSumDto getUserSellTradeProfitsSum(Long userId) {
        // 사용자의 전체 매도 체결 내역 가져오기
        List<TradeFetchService.SellTrade> sellTrades = tradeFetchService.getUserSellTrades(userId);

        // 합산 값 초기화
        double totalRealizedProfit = 0.0;
        double totalSellBuyProfit = 0.0;
        double totalSellAmount = 0.0;
        double totalBuyAmount = 0.0;
        double totalFxProfit = 0.0;

        // 모든 Trade를 순회하면서 합산
        for (TradeFetchService.SellTrade trade : sellTrades) {
            double sellAmount = calculateTradeVolumes(userId, trade)[0]; // 매도 금액
            double buyAmount = calculateTradeVolumes(userId, trade)[1]; // 매수 금액
            double sellBuyProfit = sellAmount - buyAmount; // 매매손익
            double realizedProfit = calculateRealizedProfit(userId, trade); // 실현 손익
            double fxProfit = holdingsFxService.calculateFxProfit(userId, trade); // 환차손익

            totalRealizedProfit += realizedProfit;
            totalSellBuyProfit += sellBuyProfit;
            totalSellAmount += sellAmount;
            totalBuyAmount += buyAmount;
            totalFxProfit += fxProfit;
        }

        // DTO 생성 및 반환
        return TradeProfitSumDto.builder()
                .realizedProfit(totalRealizedProfit)
                .sellbuyProfit(totalSellBuyProfit)
                .sellAmount(totalSellAmount)
                .buyAmount(totalBuyAmount)
                .fxProfit(totalFxProfit)
                .build();
    }

    /**
     * 실현 손익(KRW) 계산 메서드 (매매손익 + 환차손익)
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return 실현 손익 (KRW)
     */
    public double calculateRealizedProfit(Long userId, TradeFetchService.SellTrade trade) {

        if (!trade.getSellerUserId().equals(userId)) {
            return 0.0; // 매도한 거래가 아니면 0 반환
        }

        // 매도 금액(KRW) 계산
        double sellAmount = trade.getTradePrice() * trade.getTradeQuantity();

        // 매도한 종목의 holdings 데이터 가져오기
        Optional<Holdings> holdingsOptional = holdingsRepository.findByUserIdAndStockTicker(userId, trade.getTradeTicker());
        if (holdingsOptional.isEmpty()) {
            return 0.0; // holdings 데이터가 없으면 0 반환
        }

        Holdings holdings = holdingsOptional.get();

        // 매수 금액(KRW) 계산
        double buyAmount = ((double) holdings.getHoldingPrice()) * ((double) trade.getTradeQuantity() / (double) holdings.getHoldingQuantity());

        // 매매손익(KRW) 계산
        double tradeProfit = sellAmount - buyAmount;

        // 환차손익(KRW) 계산
        double fxProfit = holdingsFxService.calculateFxProfit(userId, trade);

        // 실현 손익(KRW) = 매매손익 + 환차손익
        return tradeProfit + fxProfit;
    }

    /**
     * 손익률(%) 계산 메서드 (단일 Trade 객체)
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return 손익률 (%)
     */
    public double calculateTradeReturnRate(Long userId,TradeFetchService.SellTrade trade) {
        if (!trade.getSellerUserId().equals(userId)) {
            return 0.0; // 매도한 거래가 아니면 0 반환
        }

        // 실현 손익(KRW) 계산
        double realizedProfit = calculateRealizedProfit(userId, trade);

        // 매도한 종목의 holdings 데이터 가져오기
        Optional<Holdings> holdingsOptional = holdingsRepository.findByUserIdAndStockTicker(userId, trade.getTradeTicker());
        if (holdingsOptional.isEmpty()) {
            return 0.0; // 해당 종목에 대한 holdings 데이터가 없으면 0 반환
        }

        Holdings holdings = holdingsOptional.get();

        // 매수 금액(KRW) 계산
        double buyAmount = holdings.getHoldingPrice() * holdings.getHoldingQuantity();

        // 손익률(%) 계산 (0으로 나누기 방지)
        return (buyAmount == 0) ? 0.0 : (realizedProfit / buyAmount) * 100;
    }

    /**
     * 매도 평균가(KRW) 및 매수 평균가(KRW) 계산 메서드
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return [매도 평균가(KRW), 매수 평균가(KRW)] 배열
     */
    public double[] calculateAverageTradePrices(Long userId, TradeFetchService.SellTrade trade) {
        if (!trade.getSellerUserId().equals(userId)) {
            return new double[]{0.0, 0.0}; // 매도한 거래가 아니면 0 반환
        }

        // 매도 평균가(KRW) = tradePrice
        double sellAveragePrice = trade.getTradePrice();

        // 매도한 종목의 holdings 데이터 가져오기
        Optional<Holdings> holdingsOptional = holdingsRepository.findByUserIdAndStockTicker(userId, trade.getTradeTicker());
        if (holdingsOptional.isEmpty()) {
            return new double[]{sellAveragePrice, 0.0}; // holdings 데이터가 없으면 매수 평균가는 0
        }

        Holdings holdings = holdingsOptional.get();

        // 매수 평균가(KRW) = holdingPrice
        double buyAveragePrice = holdings.getHoldingPrice();

        return new double[]{sellAveragePrice, buyAveragePrice};
    }

    /**
     * 매도 금액(USD), 매수 금액(USD), 매도 수량, 매수 수량 계산 메서드
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return [매도 금액(USD), 매수 금액(USD), 매도 수량, 매수 수량] 배열
     */
    public double[] calculateTradeVolumes(Long userId, TradeFetchService.SellTrade trade) {
        if (!trade.getSellerUserId().equals(userId)) {
            return new double[]{0.0, 0.0, 0.0, 0.0}; // 매도한 거래가 아니면 0 반환
        }

        // 매도 금액(USD) = tradePrice * tradeQuantity
        double sellAmount = trade.getTradePrice() * trade.getTradeQuantity();

        // 매도 수량 = tradeQuantity
        double sellQuantity = trade.getTradeQuantity();

        // 매도한 종목의 holdings 데이터 가져오기
        Optional<Holdings> holdingsOptional = holdingsRepository.findByUserIdAndStockTicker(userId, trade.getTradeTicker());
        if (holdingsOptional.isEmpty()) {
            return new double[]{sellAmount, 0.0, sellQuantity, 0.0}; // holdings 데이터가 없으면 매수 관련 정보는 0
        }

        Holdings holdings = holdingsOptional.get();

        // 매수 금액(USD) = holdingPrice * holdingQuantity
        double buyAmount = holdings.getHoldingPrice() * holdings.getHoldingQuantity();

        // 매수 수량 = holdingQuantity
        double buyQuantity = holdings.getHoldingQuantity();

        return new double[]{sellAmount, buyAmount, sellQuantity, buyQuantity};
    }

    /**
     * 매도 환율(KRW/USD), 매수 환율(KRW/USD) 계산 메서드
     * @param userId 사용자 ID
     * @param trade 단일 Trade 객체
     * @return [매도 환율(KRW/USD), 매수 환율(KRW/USD)] 배열
     */
    public double[] calculateTradeExchangeRates(Long userId, TradeFetchService.SellTrade trade) {
        if (!trade.getSellerUserId().equals(userId)) {
            return new double[]{0.0, 0.0}; // 매도한 거래가 아니면 0 반환
        }

        // 매도 환율(KRW/USD) = tradeExchangeRate
        double sellExchangeRate = trade.getTradeExchangeRate();

        // 매도한 종목의 holdings 데이터 가져오기
        Optional<Holdings> holdingsOptional = holdingsRepository.findByUserIdAndStockTicker(userId, trade.getTradeTicker());
        if (holdingsOptional.isEmpty()) {
            return new double[]{sellExchangeRate, 0.0}; // holdings 데이터가 없으면 매수 환율은 0
        }

        Holdings holdings = holdingsOptional.get();

        // 매수 환율(KRW/USD) = holdings의 exchangeRate
        double buyExchangeRate = holdings.getExchangeRate();

        return new double[]{sellExchangeRate, buyExchangeRate};
    }

}
