package finpago.common.global.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoticeEvent {
    private Long userId;         // 알림을 받을 사용자 ID
    private String title;        // 알림 제목
    private String stockTicker;  // 주식 티커
    private Long orderQuantity;  // 원래 주문했던 수량
    private Long tradeQuantity;  // 실제 체결된 수량
    private Long tradePrice;     // 체결 가격
}
