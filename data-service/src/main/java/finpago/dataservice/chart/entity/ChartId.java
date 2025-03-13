package finpago.dataservice.chart.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChartId implements Serializable {
    private LocalDateTime reportDate;
    private String stockTicker;
    private String chartType;
}
