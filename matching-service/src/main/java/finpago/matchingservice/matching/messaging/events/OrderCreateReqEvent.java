package finpago.matchingservice.matching.messaging.events;

import finpago.matchingservice.order.OrderStatus;
import finpago.matchingservice.order.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateReqEvent {
    private UUID offerNumber;
    private Long userId;
    private OrderType offerType;
    private Long offerQuantity;
    private Long offerPrice;
    private String stockTicker;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;
}
