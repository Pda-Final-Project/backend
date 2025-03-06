package finpago.userservice.holdings.service;

import finpago.common.global.messaging.BuyTradeMatchEvent;
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
        String key = "user:" + userId + ":available_stocks:" + stockTicker;
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
     * @return List<UserHoldingsDto> (보유 주식 정보 리스트)
     */
    public List<UserHoldingsDto> getUserHoldings(Long userId) {
        // 사용자의 보유 종목 목록 가져오기
        List<Holdings> userHoldings = holdingsRepository.findByUserId(userId);

        // 각 보유 종목에 대해 DTO 변환
        return userHoldings.stream()
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

                    //매수금액
                    double buyAmount=holding.getHoldingPrice() * holdingQuantity;

                    // 손익등락(KRW) 계산(평가금액-매수금액)
                    double profitChange = evaluationAmount-buyAmount;

                    // 수익률(%) 계산
                    double returnRate = profitChange/buyAmount *100;

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

}
