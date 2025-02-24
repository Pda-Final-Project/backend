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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;
    private final StringRedisTemplate redisTemplate;

    public UUID createOrder(Long userId, OrderCreateReqDto orderCreateReqDto) {
        String balanceKey = "user:" + userId + ":balance";
        String stockKey = "user:" + userId + ":stocks:" + orderCreateReqDto.getStockTicker();

        // Redis에서 사용 가능 예수금 조회
        String balanceStr = redisTemplate.opsForValue().get(balanceKey);
        Long availableBalance = balanceStr != null ? Long.parseLong(balanceStr) : null;

//        // Redis에 없으면 Feign Client로 정산 모듈에서 예수금 조회
//        if (availableBalance == null) {
//            try {
//                availableBalance = settlementFeignClient.getUserBalance(userId);
//                if (availableBalance != null) {
//                    redisTemplate.opsForValue().set(balanceKey, availableBalance.toString(), 5, TimeUnit.MINUTES); // ✅ Redis에 캐싱
//                }
//            } catch (Exception e) {
//                log.error("정산 모듈에서 예수금 조회 실패: {}", e.getMessage());
//                throw new IllegalStateException("예수금 조회 실패");
//            }
//        }

        // Redis에서 보유 주식 조회
        String stockStr = redisTemplate.opsForValue().get(stockKey);
        Long availableStocks = stockStr != null ? Long.parseLong(stockStr) : null;

//        // Redis에 없으면 Feign Client로 체결 모듈에서 보유 주식 조회
//        if (availableStocks == null) {
//            try {
//                availableStocks = executionFeignClient.getUserStocks(userId, orderCreateReqDto.getStockTicker());
//                if (availableStocks != null) {
//                    redisTemplate.opsForValue().set(stockKey, availableStocks.toString(), 5, TimeUnit.MINUTES); // ✅ Redis에 캐싱
//                }
//            } catch (Exception e) {
//                log.error("체결 모듈에서 보유 주식 조회 실패: {}", e.getMessage());
//                throw new IllegalStateException("보유 주식 조회 실패");
//            }
//        }

        // 사용 가능 예수금 검증
        if (orderCreateReqDto.getOfferType().equals("BUY") && (availableBalance == null || availableBalance < orderCreateReqDto.getOfferPrice())) {
            throw new IllegalStateException("사용 가능 예수금 부족");
        }

        // 보유 주식 검증
        if (orderCreateReqDto.getOfferType().equals("SELL") && (availableStocks == null || availableStocks < orderCreateReqDto.getOfferQuantity())) {
            throw new IllegalStateException("보유 주식 부족");
        }

        // 검증 통과 후 주문 저장 및 Kafka 발행
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
}
