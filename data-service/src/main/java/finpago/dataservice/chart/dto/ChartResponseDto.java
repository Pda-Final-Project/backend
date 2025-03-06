package finpago.dataservice.chart.dto;

import finpago.dataservice.chart.entity.Chart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartResponseDto {
    private LocalDateTime date;
    private BigInteger open;
    private BigInteger high;
    private BigInteger low;
    private BigInteger close;
    private BigInteger volume;

    public ChartResponseDto(Chart chart) {
        this.volume = chart.getChartVolume();
        this.low = chart.getChartLow();
        this.high = chart.getChartHigh();
        this.open = chart.getChartOpen();
        this.close = chart.getChartClose();
        this.date = chart.getReportDate();
    }
}
