package finpago.userservice.holdings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//해외주식 잔고
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeSummaryDto {
    private double evaluationAmount; // 평가금액(KRW)
    private double profitChange; // 손익등락(KRW)
    private double returnRate; // 수익률(%)
    private double buyAmount; // 매수금액(KRW)
    private double tradeProfit; // 매매손익(KRW)
    private double fxProfit; // 환차손익(KRW)
}
