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
    private static final long EXPIRATION_DAYS = 30; // Redis 데이터 보관 기간 (30일)

    @Transactional
    public UUID createOrder(Long userId, OrderCreateReqDto orderCreateReqDto) {
        OrderType orderType = OrderType.valueOf(orderCreateReqDto.getOfferType());

        if (orderType == OrderType.BUY) {
            validateAndDeductAvailableBalance(orderCreateReqDto);
        } else if (orderType == OrderType.SELL) {
            validateAndDeductAvailableStocks(orderCreateReqDto);
        }

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
            throw new InsufficientBalanceException("예수금이 부족합니다");
        }

        updateAvailableBalance(orderCreateReqDto.getUserId(), -requiredAmount);
        updateBalance(orderCreateReqDto.getUserId(), -requiredAmount);
        updateAvailableStocks(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker(), orderCreateReqDto.getOfferQuantity());
        updateHoldings(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker(), orderCreateReqDto.getOfferQuantity());
    }

    /**
     * 매도 주문 시 사용 가능 주식 검증 후 차감
     */
    private void validateAndDeductAvailableStocks(OrderCreateReqDto orderCreateReqDto) {
        Long availableStocks = getCachedAvailableStocks(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker());

        if (availableStocks < orderCreateReqDto.getOfferQuantity()) {
            throw new InsufficientStockException("보유 주식이 부족합니다");
        }

        updateAvailableStocks(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker(), -orderCreateReqDto.getOfferQuantity());
        updateHoldings(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker(), -orderCreateReqDto.getOfferQuantity());
        updateAvailableBalance(orderCreateReqDto.getUserId(), orderCreateReqDto.getOfferPrice() * orderCreateReqDto.getOfferQuantity());
    }

    private Long getCachedAvailableBalance(Long userId) {
        String key = "user:" + userId + ":available_balance";
        return redisTemplate.opsForValue().get(key) != null ? Long.parseLong(redisTemplate.opsForValue().get(key)) : DEFAULT_BALANCE;
    }

    private Long getCachedAvailableStocks(Long userId, String stockTicker) {
        String key = "user:" + userId + ":available_stocks:" + stockTicker;
        return redisTemplate.opsForValue().get(key) != null ? Long.parseLong(redisTemplate.opsForValue().get(key)) : DEFAULT_STOCKS;
    }

    /**
     * 사용 가능 예수금 업데이트 (30일 보관)
     */
    private void updateAvailableBalance(Long userId, Long amount) {
        String key = "user:" + userId + ":available_balance";
        Long current = getCachedAvailableBalance(userId);
        redisTemplate.opsForValue().set(key, String.valueOf(current + amount), EXPIRATION_DAYS, TimeUnit.DAYS);
    }

    /**
     * 실제 예수금 업데이트 (30일 보관)
     */
    private void updateBalance(Long userId, Long amount) {
        String key = "user:" + userId + ":balance";
        Long current = getCachedAvailableBalance(userId);
        redisTemplate.opsForValue().set(key, String.valueOf(current + amount), EXPIRATION_DAYS, TimeUnit.DAYS);
    }

    /**
     * 사용 가능 주식 업데이트 (30일 보관)
     */
    private void updateAvailableStocks(Long userId, String stockTicker, Long quantity) {
        String key = "user:" + userId + ":available_stocks:" + stockTicker;
        Long current = getCachedAvailableStocks(userId, stockTicker);
        redisTemplate.opsForValue().set(key, String.valueOf(current + quantity), EXPIRATION_DAYS, TimeUnit.DAYS);
    }

    /**
     * 보유 주식 업데이트 (30일 보관)
     */
    private void updateHoldings(Long userId, String stockTicker, Long quantity) {
        String key = "user:" + userId + ":holdings:" + stockTicker;
        Long current = getCachedAvailableStocks(userId, stockTicker);
        redisTemplate.opsForValue().set(key, String.valueOf(current + quantity), EXPIRATION_DAYS, TimeUnit.DAYS);
    }
}
