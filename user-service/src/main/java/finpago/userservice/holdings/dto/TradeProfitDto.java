package finpago.userservice.holdings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeProfitDto {
    private String stockTicker; // 종목 코드
    private double realizedProfit; // 실현 손익(KRW)
    private double returnRate; // 손익률(%)
    private double sellAveragePrice; // 매도 평균가(KRW)
    private double buyAveragePrice; // 매수 평균가(KRW)
    private double sellAmount; // 매도 금액(USD)
    private double buyAmount; // 매수 금액(USD)
    private double sellQuantity; // 매도 수량
    private double buyQuantity; // 매수 수량
    private double sellExchangeRate; // 매도 환율(KRW/USD)
    private double buyExchangeRate; // 매수 환율(KRW/USD)
}
