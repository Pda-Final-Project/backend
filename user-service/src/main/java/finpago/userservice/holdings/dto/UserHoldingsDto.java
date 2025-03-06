package finpago.userservice.holdings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//보유종목 조회dto
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHoldingsDto {
    private String stockTicker; // 종목 코드
    private double buyAmount; // 매수 금액(KRW)
    private double buyAveragePrice; // 매수 평균가(KRW)
    private double currentPrice; // 현재가(KRW)
    private long holdingQuantity; // 보유 수량
    private double evaluationAmount; // 평가 금액(KRW) (보유 수량 * 현재가)
    private double profitChange; // 손익등락(KRW)
    private double returnRate; // 수익률(%)
}
