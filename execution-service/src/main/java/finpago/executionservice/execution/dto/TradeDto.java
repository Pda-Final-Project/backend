package finpago.executionservice.execution.dto;

import finpago.common.global.enums.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TradeDto {
    private String stockTicker;
    private String executionType; //매매구분(현금매도,현금매수)
    private Long offerPrice;
    private Long orderQuantity;
    private Long tradePrice;
    private Long tradeQuantity;
    private Long unfilledQuantity;
    private TradeStatus tradeStatus;
    private Long tradeNumber;
    private LocalDateTime tradeDate;
}