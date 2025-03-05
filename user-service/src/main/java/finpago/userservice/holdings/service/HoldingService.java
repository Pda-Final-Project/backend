package finpago.userservice.holdings.service;

import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.userservice.holdings.entity.Holdings;
import finpago.userservice.holdings.repository.HoldingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoldingService {

    private final HoldingsRepository holdingsRepository;
    private final StringRedisTemplate redisTemplate;
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

    //평단가 업데이트 (없다면 holding객체 생성)
    @Transactional
    public void updateHoldings(TradeMatchingEvent event) {
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
