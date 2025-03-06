package finpago.dataservice.chart.controller;

import finpago.common.global.common.ApiResponse;
import finpago.dataservice.chart.dto.ChartResponseDto;
import finpago.dataservice.chart.service.ChartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/chart")
public class ChartController {

    private final ChartService chartService;
    //필수 정보: ticker, chartType
    //부가 정보: startDate, endDate

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChartResponseDto>>> getCharts(
            @RequestParam("ticker") String ticker,
            @RequestParam("chartType") String chartType,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<ChartResponseDto> charts = chartService.getCharts(ticker, chartType, startDate, endDate);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK,  "차트 조회 성공", charts));
    }
}
