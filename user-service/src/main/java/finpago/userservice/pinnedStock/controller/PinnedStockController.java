package finpago.userservice.pinnedStock.controller;

import finpago.userservice.common.global.exception.error.DuplicatePinnedStockException;
import finpago.userservice.common.global.response.ApiResponse;
import finpago.userservice.common.global.response.ResponseCode;
import finpago.userservice.pinnedStock.dto.PinnedStockReqDto;
import finpago.userservice.pinnedStock.service.PinnedStockService;
import finpago.userservice.user.config.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/stocks")
@RequiredArgsConstructor
public class PinnedStockController {

    private final PinnedStockService pinnedStockService;

    @PostMapping("/like")
    public ResponseEntity<ApiResponse<String>> addPinnedStock(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PinnedStockReqDto requestDTO
    ) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().body(ApiResponse.of(ResponseCode.INVALID_TOKEN));
        }

        Long userId = userDetails.getUser().getUserId();
        try {
            pinnedStockService.addPinnedStock(userId, requestDTO.getStockTicker());
            return ResponseEntity.ok(ApiResponse.of(ResponseCode.PINNED_STOCK_ADDED));
        } catch (DuplicatePinnedStockException e) {
            return ResponseEntity.status(409).body(ApiResponse.of(ResponseCode.PINNED_STOCK_ALREADY_EXISTS));
        }
    }
}
