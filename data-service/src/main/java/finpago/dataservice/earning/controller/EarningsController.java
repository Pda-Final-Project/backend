package finpago.dataservice.earning.controller;

import finpago.common.global.common.ApiResponse;
import finpago.dataservice.earning.entity.Earnings;
import finpago.dataservice.earning.service.EarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/earnings")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EarningsController {

    private final EarningsService earningsService;

    /**
     * 특정 종목의 Earnings 데이터를 조회하는 API
     * @param ticker 조회할 종목 코드 (예: "TSLA", "NVDA")
     * @return 해당 종목의 실적 데이터 리스트
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<ApiResponse<List<Earnings>>> getEarnings(@PathVariable String ticker) {
        List<Earnings> earnings = earningsService.getEarningsByTicker(ticker);

        if (earnings.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body(ApiResponse.success(HttpStatus.NO_CONTENT, "해당 종목의 실적 데이터가 없습니다.", earnings));
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "종목 실적 데이터 조회 성공", earnings));
    }
}
