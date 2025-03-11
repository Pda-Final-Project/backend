package finpago.common.global.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellTradeMatchEvent {
    private Long tradeId;       // 체결 고유 번호
    private Long sellOfferNumber;   // 매도 주문 ID
    private Long sellerUserId;      // 매도자 ID
    private String stockTicker; // 주식 티커
    private Long tradeQuantity; // 체결 수량
    private Long unfilledQuantity; //미체결 수량
    private Long tradePrice;    // 체결 가격
    private LocalDateTime tradeTimestamp; // 체결 시각
    private Long sellerOrderQuantity; // 매도 주문 수량
    private Float exchangeRate; // 환율
    private Long sellerOfferPrice; // 매도 주문 가격
}
