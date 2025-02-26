package finpago.userservice.pinnedStock.controller;

import finpago.common.global.exception.error.DuplicatePinnedStockException;
import finpago.common.global.common.ApiResponse;
import finpago.userservice.pinnedStock.dto.PinnedStockReqDto;
import finpago.userservice.pinnedStock.dto.PinnedStockResDto;
import finpago.userservice.pinnedStock.service.PinnedStockService;
import finpago.userservice.user.config.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/stocks")
@RequiredArgsConstructor
public class PinnedStockController {

    private final PinnedStockService pinnedStockService;

    /**
     * 관심 종목 추가
     */
    @PostMapping("/like")
    public ResponseEntity<ApiResponse<String>> addPinnedStock(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PinnedStockReqDto requestDTO
    ) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰"));
        }

        Long userId = userDetails.getUser().getUserId();
        try {
            pinnedStockService.addPinnedStock(userId, requestDTO.getStockTicker());
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(HttpStatus.CREATED, "관심 종목 추가 완료", ""));
        } catch (DuplicatePinnedStockException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(HttpStatus.CONFLICT, "이미 등록된 종목입니다."));
        }
    }

    /**
     * 관심 종목 조회
     */
    @GetMapping("/like")
    public ResponseEntity<ApiResponse<List<PinnedStockResDto>>> getPinnedStocks(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰"));
        }

        Long userId = userDetails.getUser().getUserId();
        ApiResponse<List<PinnedStockResDto>> pinnedStocksResponse = pinnedStockService.getPinnedStocks(userId);
        return ResponseEntity.ok(pinnedStocksResponse);
    }
}
