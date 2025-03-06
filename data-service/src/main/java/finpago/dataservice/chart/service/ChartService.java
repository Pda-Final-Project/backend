package finpago.dataservice.chart.service;

import finpago.dataservice.chart.dto.ChartResponseDto;
import finpago.dataservice.chart.entity.Chart;
import finpago.dataservice.chart.repository.ChartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartService {

    private final ChartRepository chartRepository;

    public List<ChartResponseDto> getCharts(String stockTicker, String chartType, LocalDate startDate, LocalDate endDate) {
        List<Chart> charts;
        if (startDate == null || endDate == null) {
            charts = chartRepository.findByStockTickerAndChartTypeOrderByReportDateDesc(stockTicker, chartType);
        } else {
            charts = chartRepository.findByStockTickerAndChartTypeAndReportDateBetweenOrderByReportDateDesc(
                    stockTicker, chartType, startDate, endDate);
        }
        return charts.stream()
                .map(ChartResponseDto::new)
                .collect(Collectors.toList());
    }

}
