package finpago.executionservice.execution.entity;

import finpago.common.global.common.BaseEntity;
import finpago.common.global.enums.TradeStatus;
import finpago.executionservice.execution.config.UUIDToStringConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buy_trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyTrade extends BaseEntity {

    @Id
    @Column(name = "buy_trade_number", length = 64, nullable = false, updatable = false)
//    @Convert(converter = UUIDToStringConverter.class)
    private UUID buyTradeNumber; // 매수 체결 고유 번호

    @Column(name = "buy_offer_number", length = 64, nullable = false)
    private UUID buyOfferNumber; // 매수 주문 고유 번호

    @Column(name = "trade_ticker", length = 10, nullable = false)
    private String tradeTicker; // 주식 티커

    @Column(name = "buyer_user_id", length = 64, nullable = false)
    private Long buyerUserId; // 매수자 ID

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

    @Column(name = "buy_offer_price", nullable = false)
    private Long buyerOfferPrice; // 매수 주문 가격

    @Column(name = "buy_offer_quentity", nullable = false)
    private Long buyerOrderQuantity; // 매수 주문 수량



}