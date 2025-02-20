package finpago.userservice.pinnedStock;

import finpago.common.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pinned_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//관심종목
public class PinnedStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pinned_stock_id", length = 64, nullable = false, updatable = false)
    private String pinnedStockId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "stock_ticker", length = 10, nullable = false)
    private String stockTicker;

}
