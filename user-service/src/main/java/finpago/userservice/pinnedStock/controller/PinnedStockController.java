package finpago.userservice.pinnedStock.controller;

import finpago.userservice.pinnedStock.dto.PinnedStockReqDto;
import finpago.userservice.pinnedStock.entity.PinnedStock;
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
    public ResponseEntity<PinnedStock> addPinnedStock(@RequestBody PinnedStockReqDto requestDTO, HttpServletRequest request) {
        PinnedStock pinnedStock = pinnedStockService.addPinnedStock(request, requestDTO);
        return ResponseEntity.ok(pinnedStock);
    }

}
