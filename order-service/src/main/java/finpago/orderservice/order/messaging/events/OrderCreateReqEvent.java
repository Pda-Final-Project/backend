package finpago.orderservice.order.messaging.events;

import finpago.orderservice.order.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateReqEvent {
    private String userId;
    private OrderType offerType;
    private Float offerQuantity;
    private Long offerPrice;
    private String stockTicker;
}
