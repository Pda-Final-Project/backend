package finpago.userservice.account.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BalanceDto {
    private final Long availableBalance; // 주문 가능 금액
    private final Long d1Balance; // D+1일 예수금
    private final Long batchBalance; // 배치 예수금 (실제 예수금 업데이트 예정) D+2
    private final Long balance;
}
