package finpago.userservice.holdings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//손익내역 sum
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeProfitSumDto {
    private double realizedProfit; // 실현 손익(KRW)
    private double sellbuyProfit; //매매손익(KRW) : 매도금액-매수금액
    private double sellAmount; // 매도 금액(KRW)
    private double buyAmount; // 매수 금액(KRW)
    private double fxProfit; // 환차손익(KRW)
}
