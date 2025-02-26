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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;
    private final StringRedisTemplate redisTemplate;

    private static final long DEFAULT_BALANCE = 1000000; // 기본 예수금 (1,000,000)
    private static final long DEFAULT_STOCKS = 100L; // 기본 보유 주식 수량 (100주)

    @Transactional
    public UUID createOrder(Long userId, OrderCreateReqDto orderCreateReqDto) {
        OrderType orderType = OrderType.valueOf(orderCreateReqDto.getOfferType());

        if (orderType == OrderType.BUY) {
            validateBalance(orderCreateReqDto);
        } else if (orderType == OrderType.SELL) {
            validateStocks(orderCreateReqDto);
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

        System.out.println(order);

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
     * BUY 주문 시 예수금 검증
     */
    private void validateBalance(OrderCreateReqDto orderCreateReqDto) {
        Long buyerAvailableBalance = getCachedBalance(orderCreateReqDto.getUserId());

        if (buyerAvailableBalance < orderCreateReqDto.getOfferPrice()) {
            log.error("예수금 부족 - User ID: {}, 필요 금액: {}, 보유 금액: {}",
                    orderCreateReqDto.getUserId(), orderCreateReqDto.getOfferPrice(), buyerAvailableBalance);
            throw new InsufficientBalanceException("예수금이 부족합니다");
        }
    }

    /**
     * SELL 주문 시 보유 주식 검증
     */
    private void validateStocks(OrderCreateReqDto orderCreateReqDto) {
        Long sellerAvailableStocks = getCachedStocks(orderCreateReqDto.getUserId(), orderCreateReqDto.getStockTicker());

        if (sellerAvailableStocks < orderCreateReqDto.getOfferQuantity()) {
            log.error("보유 주식 부족 - User ID: {}, 필요 주식: {}, 보유 주식: {}",
                    orderCreateReqDto.getUserId(), orderCreateReqDto.getOfferQuantity(), sellerAvailableStocks);
            throw new InsufficientStockException("보유주식이 부족합니다");
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
}
