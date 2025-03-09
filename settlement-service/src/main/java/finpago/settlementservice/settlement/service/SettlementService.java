package finpago.settlementservice.settlement.service;

import finpago.common.global.exception.error.InsufficientBalanceException;
import finpago.common.global.exception.error.InsufficientStockException;
import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.settlementservice.settlement.messaging.producer.SettlementProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
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
    private final RedissonClient redissonClient; // Redisson 클라이언트 주입

    private static final long DEFAULT_BALANCE = 10000000; // 기본 예수금
    private static final long DEFAULT_STOCKS = 100; // 기본 보유 주식 수량
    private static final float DEFAULT_EXCHANGE_RATE = 1.0f; // 기본 환율 (1.0)
    private static final long EXPIRATION_DAYS = 30; // Redis 데이터 보관 기간 (30일)


    @Transactional
    public void processBuySettlement(BuyTradeMatchEvent event) {
        validateBuyerBalance(event);
        System.out.println("검증완료");
        Float exchangeRate = getExchangeRate(event.getStockTicker());
        event.setExchangeRate(exchangeRate);

        System.out.println("exchangeRate: " + exchangeRate);
        updateBatchBalance(event.getBuyerUserId(), -event.getTradePrice() * event.getTradeQuantity());
        updateBalance(event.getBuyerUserId(), -event.getTradePrice() * event.getTradeQuantity());
        updateHoldings(event.getBuyerUserId(), event.getStockTicker(), event.getTradeQuantity());
//        updateStockForFxTracking(event.getBuyerUserId(), event.getStockTicker(), event.getTradeQuantity(), exchangeRate, event.getTradePrice());

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
//        updateStockForFxTracking(event.getSellerUserId(), event.getStockTicker(), -event.getTradeQuantity(), exchangeRate, event.getTradePrice());

        settlementProducer.sendSellSettlementSuccess(event);
        log.info("매도 정산 완료: {}", event);
    }

    /**
     * 매수자의 예수금 검증
     */
    private void validateBuyerBalance(BuyTradeMatchEvent event) {
        System.out.println("들어와요");
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
     * 예수금 업데이트 (Redisson 낙관적 락 적용)
     */
    private void updateBalance(Long userId, Long amount) {
        String balanceKey = "user:" + userId + ":balance";
        RBucket<String> balanceBucket = redissonClient.getBucket(balanceKey);

        boolean updated = false;
        while (!updated) {
            String balanceStr = balanceBucket.get();
            Long currentBalance = (balanceStr != null) ? Long.parseLong(balanceStr) : DEFAULT_BALANCE;
            Long newBalance = currentBalance + amount;

            updated = balanceBucket.trySet(String.valueOf(newBalance), EXPIRATION_DAYS, TimeUnit.DAYS);

            if (!updated) {
                log.warn("예수금 업데이트 충돌 발생 - 재시도 중... (User ID: {}, 현재 예수금: {})", userId, currentBalance);
            }
        }
        log.info("예수금 업데이트 완료 - User ID: {}, 변경 후 예수금: {}", userId, balanceBucket.get());
    }


    /**
     * 배치 계산용 예수금 업데이트 (D+2일 반영용)
     * - 실제 balance 업데이트를 위한 중간 저장소 역할
     * - 사용자가 직접 조회하는 출금 가능 예수금과는 별도로 관리됨
     */
    private void updateBatchBalance(Long userId, Long amount) {
        String batchBalanceKey = "user:" + userId + ":batch_balance";
        RBucket<String> batchBalanceBucket = redissonClient.getBucket(batchBalanceKey);

        boolean exists = batchBalanceBucket.isExists();
        log.info("batchBalanceKey 존재 여부: {}", exists);

        boolean updated = false;
        while (!updated) {
            String balanceStr = batchBalanceBucket.get();
            Long currentBalance = (balanceStr != null) ? Long.parseLong(balanceStr) : DEFAULT_BALANCE;

            Long newBalance = currentBalance + amount;
            log.info("User ID: {}, 기존 배치 예수금: {}, 추가 금액: {}, 변경 후 배치 예수금: {}",
                    userId, currentBalance, amount, newBalance);

            // 기존 값과 동일한 경우에만 업데이트 (낙관적 락 적용)
            updated = batchBalanceBucket.trySet(String.valueOf(newBalance), EXPIRATION_DAYS, TimeUnit.DAYS);

            if (!updated) {
                log.warn("배치 예수금 업데이트 충돌 발생 - 재시도 중... (User ID: {}, 현재 배치 예수금: {})", userId, currentBalance);
            }
        }
        log.info("배치 예수금 업데이트 완료 - User ID: {}, 변경 후 예수금: {}", userId, batchBalanceBucket.get());
    }

    /**
     * 보유 주식 업데이트 (Redisson 낙관적 락 적용)
     */
    private void updateHoldings(Long userId, String stockTicker, Long quantity) {
        String stockKey = "user:" + userId + ":holdings:" + stockTicker;
        RBucket<String> stockBucket = redissonClient.getBucket(stockKey);

        boolean updated = false;
        while (!updated) {
            String stockStr = stockBucket.get();
            Long currentStocks = (stockStr != null) ? Long.parseLong(stockStr) : DEFAULT_STOCKS;
            Long newStocks = currentStocks + quantity;

            updated = stockBucket.trySet(String.valueOf(newStocks), EXPIRATION_DAYS, TimeUnit.DAYS);

            if (!updated) {
                log.warn("보유 주식 업데이트 충돌 발생 - 재시도 중... (User ID: {}, 주식: {}, 현재 보유량: {})",
                        userId, stockTicker, currentStocks);
            }
        }
        log.info("보유 주식 업데이트 완료 - User ID: {}, 주식: {}, 변경 후 수량: {}", userId, stockTicker, stockBucket.get());
    }
}


//
//    /**
//     * 환차손익 계산을 위해 체결 당시 환율, 수량 및 거래 가격을 Redis에 저장
//     */
//    private void updateStockForFxTracking(Long userId, String stockTicker, Long quantity, Float exchangeRate, Long tradePrice) {
//        String holdingsFxKey = "user:" + userId + ":holdings-fx:" + stockTicker;
//
//        // 저장할 데이터 형식: "환율:거래수량:거래가격"
//        String tradeInfo = exchangeRate + ":" + quantity + ":" + tradePrice;
//
//        // Redis List에 저장 (FIFO 구조로 저장)
//        redisTemplate.opsForList().rightPush(holdingsFxKey, tradeInfo);
//
//        // 데이터 유효기간 설정 (30일 후 자동 삭제)
//        redisTemplate.expire(holdingsFxKey, 30, TimeUnit.DAYS);
//
//        log.info("[환차손익 데이터 저장] 사용자ID: {}, 주식: {}, 체결 환율: {}, 체결 수량: {}, 체결 가격: {}",
//                userId, stockTicker, exchangeRate, quantity, tradePrice);
//    }


