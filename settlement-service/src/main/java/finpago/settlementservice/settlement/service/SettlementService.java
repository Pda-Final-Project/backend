package finpago.settlementservice.settlement.service;

import finpago.common.global.exception.error.InsufficientBalanceException;
import finpago.common.global.exception.error.InsufficientStockException;
import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
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

    private static final long DEFAULT_BALANCE = 10000000; // 기본 예수금
    private static final long DEFAULT_STOCKS = 100; // 기본 보유 주식 수량
    private static final float DEFAULT_EXCHANGE_RATE = 1.0f; // 기본 환율 (1.0)
    private static final long EXPIRATION_DAYS = 30; // Redis 데이터 보관 기간 (30일)


    @Transactional
    public void processBuySettlement(BuyTradeMatchEvent event) {
        validateBuyerBalance(event);

        Float exchangeRate = getExchangeRate(event.getStockTicker());
        event.setExchangeRate(exchangeRate);

        updateBatchBalance(event.getBuyerUserId(), -event.getTradePrice() * event.getTradeQuantity());
        updateBalance(event.getBuyerUserId(), -event.getTradePrice() * event.getTradeQuantity());
        updateHoldings(event.getBuyerUserId(), event.getStockTicker(), event.getTradeQuantity());
        updateStockForFxTracking(event.getBuyerUserId(), event.getStockTicker(), event.getTradeQuantity(), exchangeRate, event.getTradePrice());

        settlementProducer.sendBuySettlementSuccess(event);
        log.info("매수 정산 완료: {}", event);
    }


    @Transactional
    public void processSellSettlement(SellTradeMatchEvent event) {
        validateSellerStocks(event);

        Float exchangeRate = getExchangeRate(event.getStockTicker());
        event.setExchangeRate(exchangeRate);

        updateBatchBalance(event.getSellerUserId(), event.getTradePrice() * event.getTradeQuantity());
        updateHoldings(event.getSellerUserId(), event.getStockTicker(), -event.getTradeQuantity());
        updateStockForFxTracking(event.getSellerUserId(), event.getStockTicker(), -event.getTradeQuantity(), exchangeRate, event.getTradePrice());

        settlementProducer.sendSellSettlementSuccess(event);
        log.info("매도 정산 완료: {}", event);
    }

    /**
     * 매수자의 예수금 검증
     */
    private void validateBuyerBalance(BuyTradeMatchEvent event) {
        Long buyerAvailableBalance = getCachedAvailableBalance(event.getBuyerUserId());
        Long requiredAmount = event.getTradePrice() * event.getTradeQuantity();

        if (buyerAvailableBalance < requiredAmount) {
            log.error("예수금 부족 - 매수자 ID: {}, 필요 금액: {}, 보유 금액: {}",
                    event.getBuyerUserId(), requiredAmount, buyerAvailableBalance);
            settlementProducer.sendBuySettlementFailure(event);
            throw new InsufficientBalanceException("예수금 부족으로 정산 실패");
        }
    }

    /**
     * 매도자의 보유 주식 검증
     */
    private void validateSellerStocks(SellTradeMatchEvent event) {
        Long sellerAvailableStocks = getCachedAvailableStocks(event.getSellerUserId(), event.getStockTicker());

        if (sellerAvailableStocks < event.getTradeQuantity()) {
            log.error("보유 주식 부족 - 매도자 ID: {}, 필요 주식: {}, 보유 주식: {}",
                    event.getSellerUserId(), event.getTradeQuantity(), sellerAvailableStocks);
            settlementProducer.sendSellSettlementFailure(event);
            throw new InsufficientStockException("보유 주식 부족으로 정산 실패");
        }
    }

    /**
     * 사용 가능 예수금 조회
     */
    private Long getCachedAvailableBalance(Long userId) {
        String balanceKey = "user:" + userId + ":balance";
        String balanceStr = redisTemplate.opsForValue().get(balanceKey);
        return balanceStr != null ? Long.parseLong(balanceStr) : DEFAULT_BALANCE;
    }

    /**
     * 사용 가능 주식 조회
     */
    private Long getCachedAvailableStocks(Long userId, String stockTicker) {
        String stockKey = "user:" + userId + ":holdings:" + stockTicker;
        String stockStr = redisTemplate.opsForValue().get(stockKey);
        return stockStr != null ? Long.parseLong(stockStr) : DEFAULT_STOCKS;
    }

    private Float getExchangeRate(String stockTicker) {
        String exchangeRateKey = "stock:" + stockTicker + ":exchange_rate";
        String exchangeRateStr = redisTemplate.opsForValue().get(exchangeRateKey);
        return exchangeRateStr != null ? Float.parseFloat(exchangeRateStr) : DEFAULT_EXCHANGE_RATE;
    }


    /**
     * 사용 가능 예수금 업데이트
     */
    private void updateAvailableBalance(Long userId, Long amount) {
        String balanceKey = "user:" + userId + ":available_balance";
        Long currentBalance = getCachedAvailableBalance(userId);
        redisTemplate.opsForValue().set(balanceKey, String.valueOf(currentBalance + amount), EXPIRATION_DAYS, TimeUnit.DAYS);
    }

    /**
     * 사용 가능 주식 업데이트
     */
    private void updateAvailableStocks(Long userId, String stockTicker, Long quantity) {
        String stockKey = "user:" + userId + ":available_stocks:" + stockTicker;
        Long currentStocks = getCachedAvailableStocks(userId, stockTicker);
        redisTemplate.opsForValue().set(stockKey, String.valueOf(currentStocks + quantity), EXPIRATION_DAYS, TimeUnit.DAYS);
    }

    /**
     * 실제 예수금 업데이트
     */
    private void updateBalance(Long userId, Long amount) {
        String balanceKey = "user:" + userId + ":balance";
        Long currentBalance = getCachedAvailableBalance(userId);
        redisTemplate.opsForValue().set(balanceKey, String.valueOf(currentBalance + amount), EXPIRATION_DAYS, TimeUnit.DAYS);
    }

    /**
     * 배치 계산용 예수금 업데이트 (D+2일 반영용)
     * - 실제 balance 업데이트를 위한 중간 저장소 역할
     * - 사용자가 직접 조회하는 출금 가능 예수금과는 별도로 관리됨
     */
    private void updateBatchBalance(Long userId, Long amount) {
        String batchBalanceKey = "user:" + userId + ":batch_balance";
        Long currentBalance = getCachedAvailableBalance(userId);
        redisTemplate.opsForValue().set(batchBalanceKey, String.valueOf(currentBalance + amount), EXPIRATION_DAYS, TimeUnit.DAYS);
    }

    /**
     * 보유 주식 업데이트 (배치)
     */
    private void updateHoldings(Long userId, String stockTicker, Long quantity) {
        String stockKey = "user:" + userId + ":holdings:" + stockTicker;
        Long currentStocks = getCachedAvailableStocks(userId, stockTicker);
        redisTemplate.opsForValue().set(stockKey, String.valueOf(currentStocks + quantity), EXPIRATION_DAYS, TimeUnit.DAYS);
    }


    /**
     * 환차손익 계산을 위해 체결 당시 환율, 수량 및 거래 가격을 Redis에 저장
     */
    private void updateStockForFxTracking(Long userId, String stockTicker, Long quantity, Float exchangeRate, Long tradePrice) {
        String holdingsFxKey = "user:" + userId + ":holdings-fx:" + stockTicker;

        // 저장할 데이터 형식: "환율:거래수량:거래가격"
        String tradeInfo = exchangeRate + ":" + quantity + ":" + tradePrice;

        // Redis List에 저장 (FIFO 구조로 저장)
        redisTemplate.opsForList().rightPush(holdingsFxKey, tradeInfo);

        // 데이터 유효기간 설정 (30일 후 자동 삭제)
        redisTemplate.expire(holdingsFxKey, 30, TimeUnit.DAYS);

        log.info("[환차손익 데이터 저장] 사용자ID: {}, 주식: {}, 체결 환율: {}, 체결 수량: {}, 체결 가격: {}",
                userId, stockTicker, exchangeRate, quantity, tradePrice);
    }

}
