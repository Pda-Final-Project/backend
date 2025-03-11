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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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
        System.out.println("체결 정보는용: "+event);
        TradeStatus status;

        if (event.getUnfilledQuantity().equals(event.getBuyerOrderQuantity())) {
            status = TradeStatus.UNFILLED;
        } else if (event.getUnfilledQuantity() > 0) {
            status = TradeStatus.PENDING;
        } else {
            status = TradeStatus.SUCCESS;
        }


        BuyTrade trade = BuyTrade.builder()
                .buyTradeNumber(event.getTradeId())
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

        if (status != TradeStatus.UNFILLED) {
            saveBuyTradeToRedis(event.getBuyerUserId(), trade);
            saveBuyTradeExecutionToRedis(trade);
            executionProducer.sendBuyTradeToSettlement(event);
        }
    }

    @Transactional
    public void processSellTrade(SellTradeMatchEvent event) {
        validateSellerStocks(event);

        TradeStatus status;
        if (event.getUnfilledQuantity().equals(event.getSellerOrderQuantity())) {
            status = TradeStatus.UNFILLED;
        } else if (event.getUnfilledQuantity() > 0) {
            status = TradeStatus.PENDING;
        } else {
            status = TradeStatus.SUCCESS;
        }

        SellTrade trade = SellTrade.builder()
                .sellTradeNumber(event.getTradeId())
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
        if (status != TradeStatus.UNFILLED) {
            saveSellTradeToRedis(event.getSellerUserId(), trade);
            saveSellTradeExecutionToRedis(trade);
            executionProducer.sendSellTradeToSettlement(event);
        }
    }

    public void saveBuyTradeExecutionToRedis(BuyTrade trade) {
        String redisKey = "stock:" + trade.getTradeTicker() + ":purchase";

        // Redis 저장 형식에 맞게 데이터 변환
        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("price", trade.getTradePrice()); // 체결가
        tradeData.put("volume", trade.getTradeQuantity()); // 체결량
        tradeData.put("trade_volume", 0); // 거래량 (0으로 채움)
        tradeData.put("time", trade.getTradeDate().format(DateTimeFormatter.ofPattern("HHmmss"))); // 체결 시간

        try {
            // JSON 직렬화 후 Redis 리스트에 추가
            String tradeJson = objectMapper.writeValueAsString(tradeData);
            redisTemplate.opsForList().leftPush(redisKey, tradeJson);

            // 체결 내역 업데이트 Pub/Sub 발행
            redisTemplate.convertAndSend("trade_updates", tradeJson);

        } catch (JsonProcessingException e) {
            System.err.println("Redis 저장 오류: " + e.getMessage());
        }
    }

    public void saveSellTradeExecutionToRedis(SellTrade trade) {
        String redisKey = "stock:" + trade.getTradeTicker() + ":purchase";

        // Redis 저장 형식에 맞게 데이터 변환
        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("price", trade.getTradePrice()); // 체결가
        tradeData.put("volume", trade.getTradeQuantity()); // 체결량
        tradeData.put("trade_volume", 0); // 거래량 (0으로 채움)
        tradeData.put("time", trade.getTradeDate().format(DateTimeFormatter.ofPattern("HHmmss"))); // 체결 시간

        try {
            // JSON 직렬화 후 Redis 리스트에 추가
            String tradeJson = objectMapper.writeValueAsString(tradeData);
            redisTemplate.opsForList().leftPush(redisKey, tradeJson);

        } catch (JsonProcessingException e) {
            System.err.println("Redis 저장 오류: " + e.getMessage());
        }
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
