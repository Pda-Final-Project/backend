package finpago.common.global.messaging;

import finpago.common.global.enums.OrderStatus;
import finpago.common.global.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateReqEvent {
    private Long offerNumber;
    private Long userId;
    private OrderType offerType;
    private Long offerQuantity;
    private Long offerPrice;
    private String stockTicker;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;
}
