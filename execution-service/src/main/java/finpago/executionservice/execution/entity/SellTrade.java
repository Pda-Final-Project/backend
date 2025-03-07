package finpago.executionservice.execution.entity;

import finpago.common.global.common.BaseEntity;
import finpago.common.global.enums.TradeStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sell_trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellTrade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sell_trade_number", length = 64, nullable = false, updatable = false)
    private UUID sellTradeNumber; // 매도 체결 고유 번호

    @Column(name = "sell_offer_number", length = 64, nullable = false)
    private UUID sellOfferNumber; // 매도 주문 고유 번호

    @Column(name = "trade_ticker", length = 10, nullable = false)
    private String tradeTicker; // 주식 티커

    @Column(name = "seller_user_id", length = 64, nullable = false)
    private Long sellerUserId; // 매도자 ID

    @Column(name = "trade_date", nullable = false)
    private LocalDateTime tradeDate; // 체결 일자

    @Column(name = "trade_quantity", nullable = false)
    private Long tradeQuantity; // 체결 수량

    @Column(name = "unfilled_quantity", nullable = false)
    private Long unfilledQuantity; // 미체결 수량

    @Column(name = "trade_price", nullable = false)
    private Long tradePrice; // 체결 가격

    @Column(name = "trade_exchange_rate")
    private Float tradeExchangeRate; // 환율

    @Column(name = "trade_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeStatus tradeStatus; // 체결 상태 (SUCCESS / FAILED)

    @Column(name = "sell_offer_price", nullable = false)
    private Long sellerOfferPrice; // 매도 주문 가격

    @Column(name = "sell_offer_quentity", nullable = false)
    private Long sellerOrderQuantity; // 매도 주문 수량
}