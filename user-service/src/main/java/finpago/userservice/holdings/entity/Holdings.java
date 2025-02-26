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

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    //보유 종목 수량
    @Column(name = "holding_quantity")
    private Long holdingQuantity;

    //현재 시세 기준 보유종목 총 가격(이 종목의 현재시세 * 수량)
    @Column(name = "holding_total_price")
    private Long holdingTotalPrice;

}
