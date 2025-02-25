package finpago.settlementservice.settlement.service;

import finpago.settlementservice.settlement.messaging.events.TradeMatchingEvent;
import finpago.settlementservice.settlement.messaging.producer.SettlementProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final StringRedisTemplate redisTemplate;
    private final SettlementProducer settlementProducer;

    private static final long DEFAULT_BALANCE = 1_000_000L; // 기본 예수금 (1,000,000)
    private static final long DEFAULT_STOCKS = 100L; // 기본 보유 주식 수량 (100주)
    private static final float DEFAULT_EXCHANGE_RATE = 1.0f; // 기본 환율 (1.0)


    @Transactional
    public void processSettlement(TradeMatchingEvent event) {
        // Redis에서 예수금 및 보유 주식 조회 (없을 경우 기본값 적용)
        Long buyerAvailableBalance = getCachedBalance(event.getBuyerUserId());
        Long sellerAvailableStocks = getCachedStocks(event.getSellerUserId(), event.getStockTicker());

        // Redis에서 환율 조회 (없을 경우 기본값 적용)
        Float exchangeRate = getExchangeRate(event.getStockTicker());

        // 예수금 & 보유 주식 검증
        boolean isFundsAvailable = buyerAvailableBalance >= event.getTradePrice();
        boolean isStocksAvailable = sellerAvailableStocks >= event.getTradeQuantity();

        if (!isFundsAvailable) {
            log.error("예수금 부족 - User ID: {}, 필요 금액: {}, 보유 금액: {}",
                    event.getBuyerUserId(), event.getTradePrice(), buyerAvailableBalance);

            //정산 실패 메시지 발행 (Execution 모듈에서 롤백 수행)
            settlementProducer.sendSettlementFailure(event);
            return; // 트랜잭션 종료
        }

        if (!isStocksAvailable) {
            log.error("보유 주식 부족 - User ID: {}, 필요 주식: {}, 보유 주식: {}",
                    event.getSellerUserId(), event.getTradeQuantity(), sellerAvailableStocks);

            // 정산 실패 메시지 발행 (Execution 모듈에서 롤백 수행)
            settlementProducer.sendSettlementFailure(event);
            return;
        }

        //정산 수행
        updateBalance(event.getBuyerUserId(), -event.getTradePrice());
        updateStocks(event.getBuyerUserId(), event.getStockTicker(), event.getTradeQuantity());
        updateStockForFxTracking(event.getBuyerUserId(), event.getStockTicker(), event.getTradeQuantity(), exchangeRate); //환율 이력 저장

        updateBalance(event.getSellerUserId(), event.getTradePrice());
        updateStocks(event.getSellerUserId(), event.getStockTicker(), -event.getTradeQuantity());
        updateStockForFxTracking(event.getSellerUserId(), event.getStockTicker(), -event.getTradeQuantity(), exchangeRate); //환율 이력 저장

        // 정산 완료 후 메시지 발행
        settlementProducer.sendSettlementSuccess(event);
        log.info("정산 완료: {}", event);
    }

    private Long getCachedBalance(Long userId) {
        String balanceKey = "user:" + userId + ":balance";
        String balanceStr = redisTemplate.opsForValue().get(balanceKey);
        return balanceStr != null ? Long.parseLong(balanceStr) : DEFAULT_BALANCE;
    }

    private Long getCachedStocks(Long userId, String stockTicker) {
        String stockKey = "user:" + userId + ":stocks:" + stockTicker;
        String stockStr = redisTemplate.opsForValue().get(stockKey);
        return stockStr != null ? Long.parseLong(stockStr) : DEFAULT_STOCKS;
    }

    private Float getExchangeRate(String stockTicker) {
        String exchangeRateKey = "stock:" + stockTicker + ":exchange_rate";
        String exchangeRateStr = redisTemplate.opsForValue().get(exchangeRateKey);
        return exchangeRateStr != null ? Float.parseFloat(exchangeRateStr) : DEFAULT_EXCHANGE_RATE;
    }

    private void updateBalance(Long userId, Long amount) {
        String balanceKey = "user:" + userId + ":balance";
        Long currentBalance = getCachedBalance(userId);
        redisTemplate.opsForValue().set(balanceKey, String.valueOf(currentBalance + amount), 5, TimeUnit.MINUTES);
    }

    private void updateStocks(Long userId, String stockTicker, Long quantity) {
        String stockKey = "user:" + userId + ":stocks:" + stockTicker;
        Long currentStocks = getCachedStocks(userId, stockTicker);
        redisTemplate.opsForValue().set(stockKey, String.valueOf(currentStocks + quantity), 5, TimeUnit.MINUTES);
    }

    /**
     * 환차손익 계산을 위해 체결 당시 환율 및 수량을 Redis에 저장
     *
     * @param userId 사용자 ID
     * @param stockTicker 주식 종목 (티커)
     * @param quantity 체결된 주식 수량
     * @param exchangeRate 체결 당시 환율
     */
    private void updateStockForFxTracking(Long userId, String stockTicker, Long quantity, Float exchangeRate) {
        String holdingsFxKey = "user:" + userId + ":holdings-fx:" + stockTicker;

        // 저장할 데이터 형식: "환율:거래수량"
        String tradeInfo = exchangeRate + ":" + quantity;

        // Redis List에 저장 (FIFO 구조로 저장)
        redisTemplate.opsForList().rightPush(holdingsFxKey, tradeInfo);

        // 데이터 유효기간 설정 (30일 후 자동 삭제)
        redisTemplate.expire(holdingsFxKey, 30, TimeUnit.DAYS);

        log.info("[환차손익계산용 데이터 저장] 사용자ID: {}, 주식: {}, 체결 환율: {}, 체결 수량: {}",
                userId, stockTicker, exchangeRate, quantity);
    }

}
