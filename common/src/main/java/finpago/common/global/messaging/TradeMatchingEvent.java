package finpago.common.global.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TradeMatchingEvent {
    private UUID tradeId;       // 체결 고유 번호
    private UUID buyOfferNumber;    // 매수 주문 ID
    private UUID sellOfferNumber;   // 매도 주문 ID
    private Long buyerUserId;       // 매수자 ID
    private Long sellerUserId;      // 매도자 ID
    private String stockTicker; // 주식 티커
    private Long tradeQuantity; // 체결 수량
    private Long tradePrice;    // 체결 가격
    private LocalDateTime tradeTimestamp; // 체결 시각
    private Long buyerOrderQuentity; //매도자가 주문넣엇던 수향
    private Long sellerOrderQuentity; //매수자가 주문넣엇던 수량
    private Float exchangeRate=0.0f;//환율
}

