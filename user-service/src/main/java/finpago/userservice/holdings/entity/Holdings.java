package finpago.userservice.holdings.entity;

import finpago.common.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "holdings")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holdings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holding_id", nullable = false, updatable = false)
    private Long holdingId;

    @Column(name = "stock_ticker", length = 10, nullable = false)
    private String stockTicker;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    //보유 종목 수량
    @Column(name = "holding_quantity")
    private Long holdingQuantity;

    //평단가(개인 투자자가 주식을 매수한 평균 가격)
    @Column(name = "holding_price")
    private Long holdingPrice;

    //보유종목 총 가격(이 종목의 평단가 * 수량)
    @Column(name = "holding_total_price")
    private Long holdingTotalPrice;

    //매수 평균환율
    @Column(name = "exchange_rate")
    private Float exchangeRate;

    public void updateHoldings(Long additionalQuantity, Long tradePrice, Float tradeExchangeRate) {
        long newTotalQuantity = this.holdingQuantity + additionalQuantity;

        if (newTotalQuantity < 0) {
            throw new IllegalArgumentException("보유 수량이 음수가 될 수 없습니다.");
        }

        if (newTotalQuantity == 0) {
            this.holdingQuantity = 0L;
            this.holdingPrice = 0L;
            this.holdingTotalPrice = 0L;
            this.exchangeRate = 0.0f;
            return;
        }

        long newTotalCost = (this.holdingTotalPrice) + (tradePrice * additionalQuantity);
        long newAveragePrice = newTotalCost / newTotalQuantity;

        float newTotalExchangeRate = (this.exchangeRate * this.holdingQuantity) + (tradeExchangeRate * additionalQuantity);
        float newAverageExchangeRate = newTotalExchangeRate / newTotalQuantity;

        this.holdingQuantity = newTotalQuantity;
        this.holdingPrice = newAveragePrice;
        this.holdingTotalPrice = newTotalCost;
        this.exchangeRate = newAverageExchangeRate;
    }
    /**
     * 매도 체결 시 보유 주식 차감
     */
    public void updateHoldingsForSell(Long sellQuantity, Long tradePrice, Float tradeExchangeRate) {
        if (this.holdingQuantity < sellQuantity) {
            throw new IllegalArgumentException("매도할 주식이 부족합니다.");
        }

        long newTotalQuantity = this.holdingQuantity - sellQuantity;

        // 보유 수량이 0이 되면 초기화
        if (newTotalQuantity == 0) {
            this.holdingQuantity = 0L;
            this.holdingPrice = 0L;
            this.holdingTotalPrice = 0L;
            this.exchangeRate = 0.0f;
            return;
        }

        // 총 보유 금액 = 기존 총 금액 - (매도한 수량 * 기존 평단가)
        long newTotalCost = this.holdingTotalPrice - (tradePrice * sellQuantity);

        // 새로운 평균 매수 가격 (매도 이후에도 남아있는 주식 기준)
        long newAveragePrice = (newTotalQuantity > 0) ? newTotalCost / newTotalQuantity : 0L;

        // 새로운 평균 환율 (매도 체결가 기준으로 조정)
        float newTotalExchangeRate = (this.exchangeRate * this.holdingQuantity) - (tradeExchangeRate * sellQuantity);
        float newAverageExchangeRate = (newTotalQuantity > 0) ? newTotalExchangeRate / newTotalQuantity : 0.0f;

        this.holdingQuantity = newTotalQuantity;
        this.holdingPrice = newAveragePrice;
        this.holdingTotalPrice = newTotalCost;
        this.exchangeRate = newAverageExchangeRate;
    }

}
