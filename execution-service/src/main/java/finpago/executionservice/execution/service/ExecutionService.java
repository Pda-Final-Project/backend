package finpago.executionservice.execution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.enums.OrderStatus;
import finpago.common.global.enums.OrderType;
import finpago.common.global.enums.TradeStatus;
import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.executionservice.execution.entity.BuyTrade;
import finpago.executionservice.execution.entity.SellTrade;
import finpago.executionservice.execution.messaging.producer.ExecutionProducer;
import finpago.executionservice.execution.repository.BuyTradeRepository;
import finpago.executionservice.execution.repository.SellTradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

//체결 비즈니스 로직
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionService {

    private final BuyTradeRepository buyTradeRepository;
    private final SellTradeRepository sellTradeRepository;
    private final ExecutionProducer executionProducer;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final long DEFAULT_BALANCE = 1_000_000L; // 기본 예수금 (1,000,000)
    private static final long DEFAULT_STOCKS = 100L; // 기본 보유 주식 수량 (100주)
    private static final float DEFAULT_EXCHANGE_RATE = 1.0f; // 기본 환율 (1.0)


    @Transactional
    public void processBuyTrade(BuyTradeMatchEvent event) {
        validateBuyerBalance(event);

        TradeStatus status = (event.getUnfilledQuantity() > 0) ? TradeStatus.PENDING : TradeStatus.SUCCESS;

        BuyTrade trade = BuyTrade.builder()
                .buyOfferNumber(event.getBuyOfferNumber())
                .tradeTicker(event.getStockTicker())
                .buyerUserId(event.getBuyerUserId())
                .tradeDate(LocalDateTime.now())
                .tradeQuantity(event.getTradeQuantity())
                .unfilledQuantity(event.getUnfilledQuantity())
                .tradePrice(event.getTradePrice())
                .tradeExchangeRate(event.getExchangeRate())
                .tradeStatus(status)
                .buyerOfferPrice(event.getBuyerOfferPrice())
                .buyerOrderQuantity(event.getBuyerOrderQuantity())
                .build();

        buyTradeRepository.save(trade);
        saveBuyTradeToRedis(event.getBuyerUserId(), trade);
        executionProducer.sendBuyTradeToSettlement(event);
    }

    @Transactional
    public void processSellTrade(SellTradeMatchEvent event) {
        validateSellerStocks(event);

        TradeStatus status = (event.getUnfilledQuantity() > 0) ? TradeStatus.PENDING : TradeStatus.SUCCESS;

        SellTrade trade = SellTrade.builder()
                .sellOfferNumber(event.getSellOfferNumber())
                .tradeTicker(event.getStockTicker())
                .sellerUserId(event.getSellerUserId())
                .tradeDate(LocalDateTime.now())
                .tradeQuantity(event.getTradeQuantity())
                .unfilledQuantity(event.getUnfilledQuantity())
                .tradePrice(event.getTradePrice())
                .tradeExchangeRate(event.getExchangeRate())
                .tradeStatus(status)
                .sellerOfferPrice(event.getSellerOfferPrice())
                .sellerOrderQuantity(event.getSellerOrderQuantity())
                .build();

        sellTradeRepository.save(trade);
        saveSellTradeToRedis(event.getSellerUserId(), trade);
        executionProducer.sendSellTradeToSettlement(event);
    }

    private void saveBuyTradeToRedis(Long userId, BuyTrade trade) {
        String key = "user:" + userId + ":buy-trades";
        try {
            String tradeJson = objectMapper.writeValueAsString(trade);
            redisTemplate.opsForList().rightPush(key, tradeJson);
        } catch (JsonProcessingException e) {
            log.error("Redis 저장 중 오류 발생: ", e);
        }
    }

    private void saveSellTradeToRedis(Long userId, SellTrade trade) {
        String key = "user:" + userId + ":sell-trades";
        try {
            String tradeJson = objectMapper.writeValueAsString(trade);
            redisTemplate.opsForList().rightPush(key, tradeJson);
        } catch (JsonProcessingException e) {
            log.error("Redis 저장 중 오류 발생: ", e);
        }
    }
  /**
    * 매수자의 예수금 검증
     */
    private void validateBuyerBalance(BuyTradeMatchEvent event) {
        Long buyerAvailableBalance = getCachedBalance(event.getBuyerUserId());
        System.out.println(buyerAvailableBalance);
        Long requiredAmount = event.getTradePrice() * event.getTradeQuantity();
        System.out.println(requiredAmount);
        if (buyerAvailableBalance < requiredAmount) {
            log.error("예수금 부족 - 매수자 ID: {}, 필요 금액: {}, 보유 금액: {}",
                    event.getBuyerUserId(), requiredAmount, buyerAvailableBalance);
            sendFailedBuyTradeToMatching(event);
            throw new IllegalStateException("예수금 부족으로 거래 실패");
        }
    }

    /**
     * 매도자의 보유 주식 검증
     */
    private void validateSellerStocks(SellTradeMatchEvent event) {
        Long sellerAvailableStocks = getCachedStocks(event.getSellerUserId(), event.getStockTicker());

        if (sellerAvailableStocks < event.getTradeQuantity()) {
            log.error("보유 주식 부족 - 매도자 ID: {}, 필요 주식: {}, 보유 주식: {}",
                    event.getSellerUserId(), event.getTradeQuantity(), sellerAvailableStocks);
            sendFailedSellTradeToMatching(event);
            throw new IllegalStateException("보유 주식 부족으로 거래 실패");
        }
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

    @Transactional
    public void handleBuySettlementFailure(BuyTradeMatchEvent event) {
        log.warn("매수 체결 실패 처리 시작: {}", event);
        Optional<BuyTrade> tradeOptional = buyTradeRepository.findById(event.getTradeId());

        if (tradeOptional.isPresent()) {
            BuyTrade trade = tradeOptional.get();
            trade.setTradeStatus(TradeStatus.FAILED);
            buyTradeRepository.save(trade);
            log.warn("매수 체결 상태 FAILED로 업데이트 완료: {}", trade);
            sendFailedBuyTradeToMatching(event);
        } else {
            log.error("매수 정산 실패 처리 중 해당 체결을 찾을 수 없음: {}", event.getTradeId());
        }
    }

    @Transactional
    public void handleSellSettlementFailure(SellTradeMatchEvent event) {
        log.warn("매도 체결 실패 처리 시작: {}", event);
        Optional<SellTrade> tradeOptional = sellTradeRepository.findById(event.getSellOfferNumber());

        if (tradeOptional.isPresent()) {
            SellTrade trade = tradeOptional.get();
            trade.setTradeStatus(TradeStatus.FAILED);
            sellTradeRepository.save(trade);
            log.warn("매도 체결 상태 FAILED로 업데이트 완료: {}", trade);
            sendFailedSellTradeToMatching(event);
        } else {
            log.error("매도 정산 실패 처리 중 해당 체결을 찾을 수 없음: {}", event.getSellOfferNumber());
        }
    }

    /**
     * 체결 실패 주문을 `Matching` 모듈로 다시 전송
     */
    @Transactional
    public void sendFailedBuyTradeToMatching(BuyTradeMatchEvent event) {
        log.warn("매수 체결 실패 - Matching 모듈로 재전송: {}", event);
        OrderCreateReqEvent orderEvent = new OrderCreateReqEvent(
                event.getBuyOfferNumber(),
                event.getBuyerUserId(),
                OrderType.BUY,
                event.getTradeQuantity(),
                event.getTradePrice(),
                event.getStockTicker(),
                OrderStatus.CREATED,
                LocalDateTime.now()
        );
        executionProducer.sendFailedTradeToMatching(orderEvent);
    }

    @Transactional
    public void sendFailedSellTradeToMatching(SellTradeMatchEvent event) {
        log.warn("매도 체결 실패 - Matching 모듈로 재전송: {}", event);
        OrderCreateReqEvent orderEvent = new OrderCreateReqEvent(
                event.getSellOfferNumber(),
                event.getSellerUserId(),
                OrderType.SELL,
                event.getTradeQuantity(),
                event.getTradePrice(),
                event.getStockTicker(),
                OrderStatus.CREATED,
                LocalDateTime.now()
        );
        executionProducer.sendFailedTradeToMatching(orderEvent);
    }

}
