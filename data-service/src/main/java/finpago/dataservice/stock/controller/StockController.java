package finpago.dataservice.stock.controller;

import finpago.common.global.common.ApiResponse;
import finpago.dataservice.stock.service.StockService;
import finpago.dataservice.stock.service.StockSseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("v1/api/stocks")
public class StockController {
    private final StockService stockService;
    private final StockSseService stockSseService;

    public StockController(StockService stockService, StockSseService stockSseService){
        this.stockService = stockService;
        this.stockSseService = stockSseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getStocks(
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String searchParam) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "종목 조회 성공", stockService.getStocks(sortBy, searchParam)));
    }
}
