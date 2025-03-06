package finpago.dataservice.chart.repository;

import finpago.dataservice.chart.entity.Chart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChartRepository extends JpaRepository<Chart, LocalDateTime> {
    // 종목 코드 별, 차트 타입 별 데이터 조회 (reportDate 내림차순 정렬)
    List<Chart> findByStockTickerAndChartTypeOrderByReportDateDesc(String stockTicker, String chartType);

    // 종목 코드 별, 차트 타입 별, reportDate가 startDate와 endDate 사이에 있는 데이터 조회 (reportDate 내림차순 정렬)
    List<Chart> findByStockTickerAndChartTypeAndReportDateBetweenOrderByReportDateDesc(String stockTicker,
                                                                                       String chartType,
                                                                                       LocalDateTime startDate,
                                                                                       LocalDateTime endDate);
}
