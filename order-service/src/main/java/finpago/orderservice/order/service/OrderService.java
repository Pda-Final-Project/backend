package finpago.orderservice.order.service;

import finpago.common.global.enums.OrderStatus;
import finpago.common.global.enums.OrderType;
import finpago.common.global.exception.error.InsufficientBalanceException;
import finpago.common.global.exception.error.InsufficientStockException;
import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.orderservice.order.dto.OrderCreateReqDto;
import finpago.orderservice.order.entity.Order;
import finpago.orderservice.order.messaging.producer.OrderProducer;
import finpago.orderservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;
    private final StringRedisTemplate redisTemplate;

    private static final long DEFAULT_BALANCE = 1_000_000L; // 기본 예수금
    private static final long DEFAULT_STOCKS = 100L; // 기본 보유 주식 수량

    @Transactional
    public UUID createOrder(Long userId, OrderCreateReqDto orderCreateReqDto) {
        OrderType orderType = OrderType.valueOf(orderCreateReqDto.getOfferType());

        if (orderType == OrderType.BUY) {
            validateAndDeductAvailableBalance(orderCreateReqDto);
        } else if (orderType == OrderType.SELL) {
            validateAndDeductAvailableStocks(orderCreateReqDto);
        }

        // 검증 통과 후 주문 저장 & Kafka 메시지 발행
        Order order = Order.builder()
                .offerStatus(OrderStatus.CREATED)
                .offerType(orderType)
                .offerQuantity(orderCreateReqDto.getOfferQuantity())
                .offerPrice(orderCreateReqDto.getOfferPrice())
                .userId(userId)
                .stockTicker(orderCreateReqDto.getStockTicker())
                .build();

        orderRepository.save(order);

        OrderCreateReqEvent event = new OrderCreateReqEvent(
                order.getOfferNumber(),
                userId,
                order.getOfferType(),
                order.getOfferQuantity(),
                order.getOfferPrice(),
                order.getStockTicker(),
                order.getOfferStatus(),
                order.getCreatedAt()
        );

        orderProducer.sendOrder(event);

        return order.getOfferNumber();
    }

    /**
     * 매수 주문 시 사용 가능 예수금 검증 후 차감
     */
    private void validateAndDeductAvailableBalance(OrderCreateReqDto orderCreateReqDto) {
        Long availableBalance = getCachedAvailableBalance(orderCreateReqDto.getUserId());
        Long requiredAmount = orderCreateReqDto.getOfferPrice() * orderCreateReqDto.getOfferQuantity();

        if (availableBalance < requiredAmount) {
            log.error("예수금 부족 - User ID: {}, 필요 금액: {}, 보유 금액: {}",
                    orderCreateReqDto.getUserId(), requiredAmount, availableBalance);
            throw new InsufficientBalanceException("예수금이 부족합니다");
        }

        // 사용 가능 예수금 차감
        updateAvailableBalance(orderCreateReqDto.getUserId(), -requiredAmount);
        log.info("매수 주문 - 사용 가능 예수금 차감: 사용자 {}, 차감 금액 {}", orderCreateReqDto.getUserId(), requiredAmount);
    }

    /**
     * 매도 주문 시 사용 가능 주식 검증 후 차감
     */
    private void validateAndDeductAvailableStocks(OrderCreateReqDto orderCreateReqDto) {
        Long availableStocks = getCachedAvailableStocks(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker());

        if (availableStocks < orderCreateReqDto.getOfferQuantity()) {
            log.error("보유 주식 부족 - User ID: {}, 필요 주식: {}, 보유 주식: {}",
                    orderCreateReqDto.getUserId(), orderCreateReqDto.getOfferQuantity(), availableStocks);
            throw new InsufficientStockException("보유 주식이 부족합니다");
        }

        // 사용 가능 주식 차감
        updateAvailableStocks(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker(), -orderCreateReqDto.getOfferQuantity());
        log.info("매도 주문 - 사용 가능 주식 차감: 사용자 {}, 종목 {}, 차감 수량 {}",
                orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker(), orderCreateReqDto.getOfferQuantity());
    }

    /**
     * 사용 가능 예수금 조회
     */
    private Long getCachedAvailableBalance(Long userId) {
        String balanceKey = "user:" + userId + ":available_balance";
        String balanceStr = redisTemplate.opsForValue().get(balanceKey);
        return balanceStr != null ? Long.parseLong(balanceStr) : DEFAULT_BALANCE;
    }

    /**
     * 사용 가능 주식 조회
     */
    private Long getCachedAvailableStocks(Long userId, String stockTicker) {
        String stockKey = "user:" + userId + ":available_stocks:" + stockTicker;
        String stockStr = redisTemplate.opsForValue().get(stockKey);
        return stockStr != null ? Long.parseLong(stockStr) : DEFAULT_STOCKS;
    }

    /**
     * 사용 가능 예수금 업데이트
     */
    private void updateAvailableBalance(Long userId, Long amount) {
        String balanceKey = "user:" + userId + ":available_balance";
        Long currentBalance = getCachedAvailableBalance(userId);
        redisTemplate.opsForValue().set(balanceKey, String.valueOf(currentBalance + amount), 5, TimeUnit.MINUTES);
    }

    /**
     * 사용 가능 주식 업데이트
     */
    private void updateAvailableStocks(Long userId, String stockTicker, Long quantity) {
        String stockKey = "user:" + userId + ":available_stocks:" + stockTicker;
        Long currentStocks = getCachedAvailableStocks(userId, stockTicker);
        redisTemplate.opsForValue().set(stockKey, String.valueOf(currentStocks + quantity), 5, TimeUnit.MINUTES);
    }
}
