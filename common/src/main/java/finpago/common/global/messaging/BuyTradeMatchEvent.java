package finpago.common.global.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuyTradeMatchEvent {
    private Long tradeId;       // 체결 고유 번호
    private Long buyOfferNumber;    // 매수 주문 ID
    private Long buyerUserId;       // 매수자 ID
    private String stockTicker; // 주식 티커
    private Long tradeQuantity; // 체결 수량
    private Long unfilledQuantity; //미체결 수량
    private Long tradePrice;    // 체결 가격
    private LocalDateTime tradeTimestamp; // 체결 시각
    private Long buyerOrderQuantity; // 매수 주문 수량
    private Float exchangeRate; // 환율
    private Long buyerOfferPrice; // 매수 주문 가격
}