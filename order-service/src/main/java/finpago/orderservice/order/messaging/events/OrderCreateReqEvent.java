package finpago.orderservice.order.messaging.events;

import finpago.orderservice.order.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateReqEvent {
    private UUID offerNumber;
    private Long userId;
    private OrderType offerType;
    private Float offerQuantity;
    private Long offerPrice;
    private String stockTicker;
}
