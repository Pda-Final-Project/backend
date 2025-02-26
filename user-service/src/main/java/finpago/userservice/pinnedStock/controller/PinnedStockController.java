package finpago.userservice.pinnedStock.controller;

import finpago.userservice.common.global.response.ApiResponse;
import finpago.userservice.common.global.response.ResponseCode;
import finpago.userservice.pinnedStock.dto.PinnedStockReqDto;
import finpago.userservice.pinnedStock.service.PinnedStockService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/stocks")
@RequiredArgsConstructor
public class PinnedStockController {

    private final PinnedStockService pinnedStockService;

    @PostMapping("/like")
    public ResponseEntity<ApiResponse<String>> addPinnedStock(@RequestBody PinnedStockReqDto requestDTO, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.of(ResponseCode.INVALID_TOKEN));
        }
        token = token.substring(7);
        pinnedStockService.addPinnedStock(token, requestDTO.getStockTicker());
        return ResponseEntity.ok(ApiResponse.of(ResponseCode.PINNED_STOCK_ADDED));
    }
}
