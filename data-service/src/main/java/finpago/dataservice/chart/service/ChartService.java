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
        // 두 날짜가 모두 존재할 때만 변환하여 조회
        if (startDate != null && endDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            charts = chartRepository.findByStockTickerAndChartTypeAndReportDateBetweenOrderByReportDate(
                    stockTicker, chartType, startDateTime, endDateTime);
        } else {
            charts = chartRepository.findByStockTickerAndChartTypeOrderByReportDate(stockTicker, chartType);
        }
        return charts.stream()
                .map(ChartResponseDto::new)
                .collect(Collectors.toList());
    }

}
