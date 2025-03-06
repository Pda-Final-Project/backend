package finpago.executionservice.execution.dto;

import finpago.common.global.enums.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TradeDto {
    private String stockTicker;
    private Long offerPrice;
    private Long orderQuantity;
    private Long tradePrice;
    private Long tradeQuantity;
    private Long unfilledQuantity;
    private TradeStatus tradeStatus;
    private UUID tradeNumber;
    private LocalDateTime tradeDate;
}