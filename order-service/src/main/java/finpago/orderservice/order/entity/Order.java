package finpago.orderservice.order.entity;

import finpago.common.global.common.BaseEntity;
import finpago.orderservice.order.enums.OrderStatus;
import finpago.orderservice.order.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "offer_number", length = 64, nullable = false, updatable = false)
    private UUID offerNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_status", length = 10, nullable = false)
    private OrderStatus offerStatus; // 주문 상태 (PENDING, CANCELED, FINISHED)

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", length = 10, nullable = false)
    private OrderType offerType; // 주문 유형 (BUY, SELL)

    @Column(name = "offer_quantity", nullable = false)
    private Float offerQuantity;

    @Column(name = "offer_price", nullable = false)
    private Long offerPrice;

    @Column(name = "user_id", length = 64, nullable = false)
    private Long userId;

    @Column(name = "stock_ticker", length = 10, nullable = false)
    private String stockTicker; // 주식 티커 (AAPL, TSLA 등)
}
