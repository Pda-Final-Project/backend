package finpago.executionservice.execution.entity;

import finpago.common.global.common.BaseEntity;
import finpago.executionservice.execution.TradeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trade_number", length = 64, nullable = false, updatable = false)
    private UUID tradeNumber; // 체결 고유 번호

    @Column(name = "buyer_offer_number", length = 64, nullable = false)
    private UUID buyOfferNumber; // 매수 주문 고유 번호

    @Column(name = "seller_offer_number", length = 64, nullable = false)
    private UUID sellOfferNumber; // 매도 주문 고유 번호

    @Column(name = "trade_ticker", length = 10, nullable = false)
    private String tradeTicker; // 주식 티커

    @Column(name = "buyer_user_id", length = 64, nullable = false)
    private Long buyerUserId; // 매수자 ID

    @Column(name = "seller_user_id", length = 64, nullable = false)
    private Long sellerUserId; // 매도자 ID

    @Column(name = "trade_date", nullable = false)
    private LocalDateTime tradeDate; // 체결 일자

    @Column(name = "trade_quantity", nullable = false)
    private Long tradeQuantity; // 체결 수량

    @Column(name = "trade_price", nullable = false)
    private Long tradePrice; // 체결 가격

    @Column(name = "trade_exchange_rate")
    private Float tradeExchangeRate; // 환율

    @Column(name = "trade_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeStatus tradeStatus; // 체결 상태 (SUCCESS / FAILED)

}
