package finpago.orderservice.order.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderCreateReqDto {
//    private Long orderId;
    private Long offerQuantity;
    private Long offerPrice;
    private String stockTicker;
    private String offerType; // "BUY" or "SELL"
}
