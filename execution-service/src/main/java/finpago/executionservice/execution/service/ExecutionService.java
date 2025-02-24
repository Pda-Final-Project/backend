package finpago.executionservice.execution.service;

import finpago.common.global.exception.error.InsufficientBalanceException;
import finpago.common.global.exception.error.InsufficientStockException;
import finpago.executionservice.OrderStatus;
import finpago.executionservice.OrderType;
import finpago.executionservice.execution.TradeStatus;
import finpago.executionservice.execution.entity.Trade;
import finpago.executionservice.execution.messaging.events.OrderCreateReqEvent;
import finpago.executionservice.execution.messaging.events.TradeMatchingEvent;
import finpago.executionservice.execution.messaging.producer.ExecutionProducer;
import finpago.executionservice.execution.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionService {

    private final TradeRepository tradeRepository;
    private final ExecutionProducer executionProducer;
    private final StringRedisTemplate redisTemplate;

    private static final long DEFAULT_BALANCE = 1_000_000L; // 기본 예수금 (1,000,000)
    private static final long DEFAULT_STOCKS = 100L; // 기본 보유 주식 수량 (100주)
    private static final float DEFAULT_EXCHANGE_RATE = 1.0f; // 기본 환율 (1.0)

    @Transactional
    public void processTrade(TradeMatchingEvent event) {
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
            throw new InsufficientBalanceException("예수금 부족");
        }

        if (!isStocksAvailable) {
            log.error("보유 주식 부족 - User ID: {}, 필요 주식: {}, 보유 주식: {}",
                    event.getSellerUserId(), event.getTradeQuantity(), sellerAvailableStocks);
            throw new InsufficientStockException("보유 주식 부족");
        }

        // 검증 통과 후 체결 저장
        Trade trade = Trade.builder()
                .tradeNumber(UUID.randomUUID())
                .buyOfferNumber(event.getBuyOfferNumber())
                .sellOfferNumber(event.getSellOfferNumber())
                .tradeTicker(event.getStockTicker())
                .buyerUserId(event.getBuyerUserId())
                .sellerUserId(event.getSellerUserId())
                .tradeDate(LocalDateTime.now())
                .tradeQuantity(event.getTradeQuantity())
                .tradePrice(event.getTradePrice())
                .tradeExchangeRate(exchangeRate) // Redis에서 가져온 환율 적용
                .tradeStatus(TradeStatus.SUCCESS)
                .build();

        tradeRepository.save(trade);

        // 체결 성공 시 Settlement 모듈로 Kafka 메시지 전송
        executionProducer.sendTradeToSettlement(event);
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
    public void handleSettlementFailure(TradeMatchingEvent event) {
        log.warn("체결 실패 처리 시작: {}", event);

        // 체결 테이블에서 해당 거래 조회
        Optional<Trade> tradeOptional = tradeRepository.findById(event.getTradeId());

        if (tradeOptional.isPresent()) {
            Trade trade = tradeOptional.get();

            // 체결 상태를 FAILED로 업데이트
            trade.setTradeStatus(TradeStatus.FAILED);
            tradeRepository.save(trade);

            log.warn("체결 상태 FAILED로 업데이트 완료: {}", trade);

            // 체결 실패 시 주문을 다시 매칭 모듈로 전송
            OrderCreateReqEvent buyOrder = new OrderCreateReqEvent(
                    event.getBuyOfferNumber(),
                    event.getBuyerUserId(),
                    OrderType.BUY,
                    event.getTradeQuantity(),
                    event.getTradePrice(),
                    event.getStockTicker(),
                    OrderStatus.CREATED,
                    event.getTradeTimestamp()
            );

            OrderCreateReqEvent sellOrder = new OrderCreateReqEvent(
                    event.getSellOfferNumber(),
                    event.getSellerUserId(),
                    OrderType.SELL,
                    event.getTradeQuantity(),
                    event.getTradePrice(),
                    event.getStockTicker(),
                    OrderStatus.CREATED,
                    event.getTradeTimestamp()
            );

            // Matching 모듈로 전송
            executionProducer.sendFailedTradeToMatching(buyOrder);
            executionProducer.sendFailedTradeToMatching(sellOrder);

            log.warn("체결 실패 주문 재매칭 요청 완료: 매수={}, 매도={}", buyOrder, sellOrder);
        } else {
            log.error("정산 실패 처리 중 해당 체결을 찾을 수 없음: {}", event.getTradeId());
        }
    }


    @Transactional(readOnly = true)
    public List<Trade> getUserTrades(Long userId) {
        return tradeRepository.findByBuyerUserIdOrSellerUserId(userId, userId);
    }
}
