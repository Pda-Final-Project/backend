package finpago.orderservice.order.service;

import finpago.orderservice.order.config.auth.JwtUtil;
import finpago.orderservice.order.dto.OrderCreateReqDto;
import finpago.orderservice.order.entity.Order;
import finpago.orderservice.order.enums.OrderStatus;
import finpago.orderservice.order.enums.OrderType;
import finpago.orderservice.order.messaging.events.OrderCreateReqEvent;
import finpago.orderservice.order.messaging.producer.OrderProducer;
import finpago.orderservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final JwtUtil jwtUtil;
    private final OrderProducer orderProducer;

    public UUID createOrder(Long userId, OrderCreateReqDto orderCreateReqDto) {
        Order order = Order.builder()
                .offerStatus(OrderStatus.SENT) // 주문 상태: 주문접수
                .offerType(OrderType.valueOf(orderCreateReqDto.getOfferType())) // 매수/매도
                .offerQuantity(orderCreateReqDto.getOfferQuantity())
                .offerPrice(orderCreateReqDto.getOfferPrice())
                .userId(userId)
                .stockTicker(orderCreateReqDto.getStockTicker())
                .build();

        orderRepository.save(order);

        OrderCreateReqEvent orderCreateReqEvent = new OrderCreateReqEvent(
                order.getOfferNumber(),
                userId,
                order.getOfferType(),
                order.getOfferQuantity(),
                order.getOfferPrice(),
                order.getStockTicker(),
                order.getOfferStatus()
        );

        orderProducer.sendOrder(orderCreateReqEvent);

        return order.getOfferNumber();
    }
}
