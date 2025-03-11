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

import java.util.Optional;
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

    private static final long DEFAULT_BALANCE = 10000000; // 기본 예수금
    private static final long DEFAULT_STOCKS = 100; // 기본 보유 주식 수량
    private static final long EXPIRATION_DAYS = 30; // Redis 데이터 보관 기간 (30일)

    @Transactional
    public Long createOrder(Long userId, OrderCreateReqDto orderCreateReqDto) {
        OrderType orderType = OrderType.valueOf(orderCreateReqDto.getOfferType());

        if (orderType == OrderType.BUY) {
            validateAndDeductAvailableBalance(userId,orderCreateReqDto);
        } else if (orderType == OrderType.SELL) {
            validateAndDeductAvailableStocks(userId,orderCreateReqDto);
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
    private void validateAndDeductAvailableBalance(Long userId,OrderCreateReqDto orderCreateReqDto) {
        Long availableBalance = getCachedAvailableBalance(userId);
        Long requiredAmount = orderCreateReqDto.getOfferPrice() * orderCreateReqDto.getOfferQuantity();

        System.out.println("캐싱된사용가능예수금:"+availableBalance);
        System.out.println("필요한 돈이용:"+requiredAmount);

        if (availableBalance < requiredAmount) {
            throw new InsufficientBalanceException("예수금이 부족합니다");
        }

        updateAvailableBalance(userId, -requiredAmount);
//        updateBalance(orderCreateReqDto.getUserId(), -requiredAmount);
        updateAvailableStocks(userId, orderCreateReqDto.getStockTicker(), orderCreateReqDto.getOfferQuantity());
//        updateHoldings(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker(), orderCreateReqDto.getOfferQuantity());
    }

    /**
     * 매도 주문 시 사용 가능 주식 검증 후 차감
     */
    private void validateAndDeductAvailableStocks(Long userId, OrderCreateReqDto orderCreateReqDto) {
        Long availableStocks = getCachedAvailableStocks(userId, orderCreateReqDto.getStockTicker());

        System.out.println("캐싱된사용가능 주식수:"+availableStocks);

        if (availableStocks < orderCreateReqDto.getOfferQuantity()) {
            throw new InsufficientStockException("보유 주식이 부족합니다");
        }

        updateAvailableStocks(userId, orderCreateReqDto.getStockTicker(), -orderCreateReqDto.getOfferQuantity());
//        updateHoldings(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker(), -orderCreateReqDto.getOfferQuantity());
        updateAvailableBalance(userId, orderCreateReqDto.getOfferPrice() * orderCreateReqDto.getOfferQuantity());
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
        System.out.println("유저아이딩: " + userId);
        System.out.println("돈은용: " + amount);

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
        System.out.println("유저아이딩: " + userId);
        System.out.println("주식종목은용: " + stockTicker);
        System.out.println("수량은용: " + quantity);
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

    @Transactional
    public void retryUnmatchedOrder(OrderCreateReqEvent event) {
        Long orderId = event.getOfferNumber();
        Optional<Order> existingOrder = orderRepository.findById(orderId);

        Order order;
        if (existingOrder.isPresent()) {
            // 기존 주문이 있으면 상태만 FAILED으로 변경
            order = existingOrder.get();
            order.setOfferStatus(OrderStatus.FAILED);
            log.info("기존 주문 PENDING 상태로 업데이트: {}", order);
        } else {
            // 주문이 존재하지 않으면 새로 저장

            order = Order.builder()
                    .userId(event.getUserId())
                    .offerType(event.getOfferType())
                    .offerQuantity(event.getOfferQuantity())
                    .offerPrice(event.getOfferPrice())
                    .stockTicker(event.getStockTicker())
                    .offerStatus(OrderStatus.FAILED) // PENDING 상태로 설정
                    .build();
            log.info("새로운 미체결 주문 저장: {}", order);
        }

        orderRepository.save(order);

        // 다시 매칭 모듈로 전송
        orderProducer.sendOrder(event);
        log.info("미체결 주문을 매칭 모듈로 재전송 완료: {}", event);
    }
}
