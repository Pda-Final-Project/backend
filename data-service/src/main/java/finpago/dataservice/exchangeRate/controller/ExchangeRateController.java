package finpago.dataservice.exchangeRate.controller;

import finpago.common.global.common.ApiResponse;
import finpago.dataservice.exchangeRate.ExchangeRateService;
import finpago.dataservice.exchangeRate.dto.ExchangeRateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/api/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 특정 종목의 환율 조회 API
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<ApiResponse<ExchangeRateDto>> getExchangeRateByTicker(@PathVariable String ticker) {
        String redisKey = "stock:" + ticker + ":exchange_rate";
        String exchangeRate = redisTemplate.opsForValue().get(redisKey);

        if (exchangeRate != null) {
            ExchangeRateDto exchangeRateDto = new ExchangeRateDto(ticker, Double.parseDouble(exchangeRate));
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "환율 조회 성공", exchangeRateDto));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail(HttpStatus.NOT_FOUND, "해당 종목의 환율 데이터가 없습니다."));
        }
    }
}