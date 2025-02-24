package finpago.orderservice.order.service;

import finpago.orderservice.order.dto.OrderCreateReqDto;
import finpago.orderservice.order.entity.Order;
import finpago.orderservice.order.enums.OrderStatus;
import finpago.orderservice.order.enums.OrderType;
import finpago.orderservice.order.messaging.events.OrderCreateReqEvent;
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

    @Transactional
    public UUID createOrder(Long userId, OrderCreateReqDto orderCreateReqDto) {
        String balanceKey = "user:" + userId + ":balance";
        String stockKey = "user:" + userId + ":stocks:" + orderCreateReqDto.getStockTicker();

        Long availableBalance = getCachedBalance(userId, balanceKey);
        Long availableStocks = getCachedStocks(userId, stockKey, orderCreateReqDto.getStockTicker());

        if (orderCreateReqDto.getOfferType().equals("BUY") && (availableBalance == null || availableBalance < orderCreateReqDto.getOfferPrice())) {
            throw new IllegalStateException("사용 가능 예수금 부족");
        }

        if (orderCreateReqDto.getOfferType().equals("SELL") && (availableStocks == null || availableStocks < orderCreateReqDto.getOfferQuantity())) {
            throw new IllegalStateException("보유 주식 부족");
        }

        // 검증 통과 후 주문 저장 & Kafka 메시지 발행
        Order order = Order.builder()
                .offerStatus(OrderStatus.CREATED)
                .offerType(OrderType.valueOf(orderCreateReqDto.getOfferType()))
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
     * Redis에서 사용 가능 예수금을 조회하는 메서드
     */
    private Long getCachedBalance(Long userId, String balanceKey) {
        String balanceStr = redisTemplate.opsForValue().get(balanceKey);
        if (balanceStr != null) {
            return Long.parseLong(balanceStr);
        }
        log.warn("Redis에 예수금 데이터 없음, 정산 모듈에서 조회 필요!");
        return null;
    }

    /**
     * Redis에서 보유 주식 정보를 조회하는 메서드
     */
    private Long getCachedStocks(Long userId, String stockKey, String stockTicker) {
        String stockStr = redisTemplate.opsForValue().get(stockKey);
        if (stockStr != null) {
            return Long.parseLong(stockStr);
        }
        log.warn("Redis에 보유 주식 데이터 없음, 체결 모듈에서 조회 필요!");
        return null;
    }
}
