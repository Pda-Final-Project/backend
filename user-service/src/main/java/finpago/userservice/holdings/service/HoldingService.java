package finpago.userservice.holdings.service;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.userservice.holdings.dto.UserHoldingsDto;
import finpago.userservice.holdings.entity.Holdings;
import finpago.userservice.holdings.repository.HoldingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoldingService {

    private final HoldingsRepository holdingsRepository;
    private final StringRedisTemplate redisTemplate;
    private final HoldingsFxService holdingsFxService;
    private static final long DEFAULT_BALANCE = 10000000; // 기본 예수금
    private static final long DEFAULT_STOCKS = 100; // 기본 보유 주식 수량


    /**
     * 사용 가능 주식 조회
     */
    public Long getAvailableStocks(Long userId, String stockTicker) {
        String key = "user:" + userId + ":holdings:" + stockTicker;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : DEFAULT_STOCKS;
    }

    /**
     * 보유 주식 조회
     */
    public Long getHoldings(Long userId, String stockTicker) {
        String key = "user:" + userId + ":holdings:" + stockTicker;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : DEFAULT_STOCKS;
    }

    /**
     * 사용자의 보유 주식 정보 조회
     * @param userId 사용자 ID
     * @param sortBy 정렬 기준 (profit: 수익률순, evaluation: 평가금액순, buyAmount: 매수금액순)
     * @return List<UserHoldingsDto> (보유 주식 정보 리스트)
     */
    public List<UserHoldingsDto> getUserHoldings(Long userId, String sortBy) {
        // 사용자의 보유 종목 목록 가져오기
        List<Holdings> userHoldings = holdingsRepository.findByUserId(userId);

        // 각 보유 종목에 대해 DTO 변환
        List<UserHoldingsDto> holdingsDtoList = userHoldings.stream()
                .map(holding -> {
                    String stockTicker = holding.getStockTicker();

                    // Redis에서 현재가 가져오기
                    HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
                    String stockInfoKey = "stock:" + stockTicker;
                    String currentPriceStr = hashOps.get(stockInfoKey, "current_price");
                    double currentPrice = currentPriceStr != null ? Double.parseDouble(currentPriceStr) : 0.0;

                    // 보유 수량
                    long holdingQuantity = holding.getHoldingQuantity();

                    // 평가 금액(KRW) = 보유 수량 * 현재가
                    double evaluationAmount = holdingQuantity * currentPrice;

                    // 매수 금액
                    double buyAmount = holding.getHoldingPrice() * holdingQuantity;

                    // 손익등락(KRW) 계산(평가금액 - 매수금액)
                    double profitChange = evaluationAmount - buyAmount;

                    // 수익률(%) 계산 (매수 금액이 0이 아닐 때만 계산)
                    double returnRate = (buyAmount > 0) ? (profitChange / buyAmount) * 100 : 0.0;

                    return UserHoldingsDto.builder()
                            .stockTicker(stockTicker)
                            .buyAmount(buyAmount)
                            .buyAveragePrice(holding.getHoldingPrice())
                            .currentPrice(currentPrice)
                            .holdingQuantity(holdingQuantity)
                            .evaluationAmount(evaluationAmount)
                            .profitChange(profitChange)
                            .returnRate(returnRate)
                            .build();
                }).collect(Collectors.toList());

        // 정렬 로직 적용
        return sortHoldings(holdingsDtoList, sortBy);
    }

    /**
     * 보유 주식 리스트 정렬
     * @param holdingsDtoList 보유 주식 리스트
     * @param sortBy 정렬 기준 (profit: 수익률순, evaluation: 평가금액순, buyAmount: 매수금액순)
     * @return 정렬된 보유 주식 리스트
     */
    private List<UserHoldingsDto> sortHoldings(List<UserHoldingsDto> holdingsDtoList, String sortBy) {
        switch (sortBy) {
            case "profit":
                return holdingsDtoList.stream()
                        .sorted(Comparator.comparingDouble(UserHoldingsDto::getReturnRate).reversed()) // 수익률 내림차순
                        .collect(Collectors.toList());
            case "evaluation":
                return holdingsDtoList.stream()
                        .sorted(Comparator.comparingDouble(UserHoldingsDto::getEvaluationAmount).reversed()) // 평가 금액 내림차순
                        .collect(Collectors.toList());
            case "buyAmount":
                return holdingsDtoList.stream()
                        .sorted(Comparator.comparingDouble(UserHoldingsDto::getBuyAmount).reversed()) // 매수 금액 내림차순
                        .collect(Collectors.toList());
            default:
                return holdingsDtoList; // 기본적으로 정렬하지 않고 반환
        }
    }

    //평단가,환율,수량 업데이트 (없다면 holding객체 생성)
    @Transactional
    public void updateHoldings(BuyTradeMatchEvent event) {
        Optional<Holdings> existingHolding = holdingsRepository.findByUserIdAndStockTicker(
                event.getBuyerUserId(), event.getStockTicker());

        if (existingHolding.isPresent()) {
            Holdings holdings = existingHolding.get();
            holdings.updateHoldings(event.getTradeQuantity(), event.getTradePrice(), event.getExchangeRate());
            holdingsRepository.save(holdings);
        } else {
            Holdings newHoldings = Holdings.builder()
                    .userId(event.getBuyerUserId())
                    .stockTicker(event.getStockTicker())
                    .holdingQuantity(event.getTradeQuantity())
                    .holdingPrice(event.getTradePrice())
                    .holdingTotalPrice(event.getTradePrice() * event.getTradeQuantity())
                    .exchangeRate(event.getExchangeRate())
                    .build();
            holdingsRepository.save(newHoldings);
        }
    }


    /**
     * 매도 체결 시 보유 주식 업데이트 (매도 체결 시 주식 감소)
     */
    public void updateHoldingsForSell(SellTradeMatchEvent event) {
        log.info("매도 처리 들어옵니다~~");
        log.info("매도 요청: userId={}, stockTicker={}", event.getSellerUserId(), event.getStockTicker());

        Optional<Holdings> existingHolding = holdingsRepository.findHoldingByUserIdAndStockTicker(
                event.getSellerUserId(), event.getStockTicker());

        log.info("존재하는 보유주식: {}", existingHolding);

        if (existingHolding.isEmpty()) {
            throw new IllegalArgumentException("매도할 보유 주식이 존재하지 않습니다.");
        }

        Holdings holdings = existingHolding.get();
        log.info("매도 처리하기전 보유주식 상태: {}", holdings);

        holdings.updateHoldingsForSell(event.getTradeQuantity(), event.getTradePrice(), event.getExchangeRate());

        // 남은 수량이 0이면 삭제
        if (holdings.getHoldingQuantity() == 0) {
            holdingsRepository.delete(holdings);
        } else {
            holdingsRepository.save(holdings);
        }
    }

}
