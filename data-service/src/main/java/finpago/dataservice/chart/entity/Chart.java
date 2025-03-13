package finpago.dataservice.chart.entity;

import finpago.common.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "charts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_chart",
                        columnNames = {"report_date", "stock_ticker", "chart_type"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chart extends BaseEntity {

    @Id
    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "stock_ticker", length = 10, nullable = false)
    private String stockTicker;

    @Column(name = "chart_type", length = 10)
    private String chartType;

    @Column(name = "chart_open")
    private BigInteger chartOpen;

    @Column(name = "chart_high")
    private BigInteger chartHigh;

    @Column(name = "chart_low")
    private BigInteger chartLow;

    @Column(name = "chart_close")
    private BigInteger chartClose;

    @Column(name = "chart_volume")
    private BigInteger chartVolume;
}
