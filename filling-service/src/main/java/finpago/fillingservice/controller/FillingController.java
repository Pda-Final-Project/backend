package finpago.fillingservice.controller;

import finpago.common.global.common.ApiResponse;
import finpago.fillingservice.dto.FillingResponseDto;
import finpago.fillingservice.dto.FillingsResponseDto;
import finpago.fillingservice.service.FillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/fillings")
public class FillingController {

    private final FillingService fillingService;

    @GetMapping("/ticker/{ticker}")
    @Operation(summary = "특정 티커의 공시 리스트 조회", description = "특정 티커의 공시 리스트를 조회합니다.")
    public ResponseEntity<ApiResponse<FillingsResponseDto>> getFillingsByTicker(
            @Parameter(description = "티커") @PathVariable(name = "ticker") String ticker,
            @Parameter(description = "공시 타입") @RequestParam(name = "fillingType", required = false) String fillingType,
            @Parameter(description = "페이지 번호", schema = @Schema(defaultValue = "0")) @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @Parameter(description = "페이지 크기", schema = @Schema(defaultValue = "10")) @RequestParam(name = "size", defaultValue = "10", required = false) int size) {

        FillingsResponseDto fillingsResponseDto = fillingService.getFillingsByTicker(ticker, fillingType, page, size);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .body(ApiResponse.success(HttpStatus.FOUND, "공시 리스트 조회 성공", fillingsResponseDto));
    }

    @GetMapping("/{filling_id}")
    @Operation(summary = "특정 공시 조회", description = "특정 공시를 조회합니다.")
    public ResponseEntity<ApiResponse<FillingResponseDto>> getFilling(
            @Parameter(description = "공시 일련번호") @PathVariable(name = "filling_id") String fillingId) {

        FillingResponseDto responseDto = fillingService.getFilling(fillingId);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .body(ApiResponse.success(HttpStatus.FOUND, "공시 조회 성공", responseDto));
    }
}