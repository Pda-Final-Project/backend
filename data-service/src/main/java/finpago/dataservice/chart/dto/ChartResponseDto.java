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
    private LocalDateTime reportDate;
    private String stockTicker;
    private String chartType;
    private BigInteger chartOpen;
    private BigInteger chartHigh;
    private BigInteger chartLow;
    private BigInteger chartVolume;

    public ChartResponseDto(Chart chart) {
        this.chartVolume = chart.getChartVolume();
        this.chartLow = chart.getChartLow();
        this.chartHigh = chart.getChartHigh();
        this.chartOpen = chart.getChartOpen();
        this.chartType = chart.getChartType();
        this.stockTicker = chart.getStockTicker();
        this.reportDate = chart.getReportDate();
    }
}
