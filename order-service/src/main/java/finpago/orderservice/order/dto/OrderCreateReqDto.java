package finpago.orderservice.order.dto;

import lombok.Getter;

@Getter
public class OrderCreateReqDto {
    private Long orderId;
    private Long userId;
    private Long offerQuantity;
    private Long offerPrice;
    private String stockTicker;
    private String offerType; // "BUY" or "SELL"
}
