package finpago.fillingservice.service;

import finpago.common.global.exception.error.NotFoundFillingException;
import finpago.fillingservice.dto.FillingResponseDto;
import finpago.fillingservice.dto.FillingsResponseDto;
import finpago.fillingservice.entity.Filling;
import finpago.fillingservice.repository.FillingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FillingServiceImpl implements FillingService {

    private final FillingRepository fillingRepository;

    @Override
    public FillingsResponseDto getFillings(String ticker, String fillingType, String startDate, String endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Filling> fillingsPage;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<String> fillingTypes = convertFillingType(fillingType);

        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate, formatter) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate, formatter) : null;

            if (ticker == null && fillingTypes.isEmpty() && startDate == null && endDate == null) {
                fillingsPage = fillingRepository.findAllByOrderBySubmitTimestampDesc(pageable);
            } else if (ticker == null && fillingTypes.isEmpty()) {
                fillingsPage = fillingRepository.findBySubmitTimestampBetweenOrderBySubmitTimestampDesc(start.atStartOfDay(), end.atTime(23, 59, 59), pageable);
            } else if (ticker == null && startDate == null && endDate == null) {
                fillingsPage = fillingRepository.findByFillingTypeInOrderBySubmitTimestampDesc(fillingTypes, pageable);
            } else if (fillingTypes.isEmpty() && startDate == null && endDate == null) {
                fillingsPage = fillingRepository.findByFillingTickerOrderBySubmitTimestampDesc(ticker, pageable);
            } else if (ticker == null) {
                fillingsPage = fillingRepository.findByFillingTypeInAndSubmitTimestampBetweenOrderBySubmitTimestampDesc(
                        fillingTypes, start.atStartOfDay(), end.atTime(23, 59, 59), pageable);
            } else if (fillingTypes.isEmpty()) {
                fillingsPage = fillingRepository.findByFillingTickerAndSubmitTimestampBetweenOrderBySubmitTimestampDesc(
                        ticker, start.atStartOfDay(), end.atTime(23, 59, 59), pageable);
            } else if (startDate == null && endDate == null) {
                fillingsPage = fillingRepository.findByFillingTickerAndFillingTypeInOrderBySubmitTimestampDesc(
                        ticker, fillingTypes, pageable);
            } else {
                fillingsPage = fillingRepository.findByFillingTickerAndFillingTypeInAndSubmitTimestampBetweenOrderBySubmitTimestampDesc(
                        ticker, fillingTypes, start.atStartOfDay(), end.atTime(23, 59, 59), pageable);
            }

            if (fillingsPage.isEmpty()) {
                throw new NotFoundFillingException("해당 조건의 공시가 존재하지 않습니다.");
            }

            return FillingsResponseDto.builder()
                    .page(fillingsPage.getNumber())
                    .size(fillingsPage.getSize())
                    .totalElements(fillingsPage.getTotalElements())
                    .totalPages(fillingsPage.getTotalPages())
                    .content(fillingsPage.getContent().stream()
                            .map(FillingResponseDto::new)
                            .collect(Collectors.toList()))
                    .build();

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("날짜 형식이 잘못되었습니다. 'yyyy-MM-dd' 형식으로 입력해 주세요.");
        }


    }

    @Override
    public FillingResponseDto getFilling(String fillingId) {
        Filling filling = fillingRepository.findByFillingId(fillingId)
                .orElseThrow(() -> new NotFoundFillingException("해당 공시가 존재하지 않습니다."));
        return new FillingResponseDto(filling);
    }

    private List<String> convertFillingType(String fillingType) {
        if (fillingType == null) {
            return List.of();  // 빈 리스트 반환하여 NullPointerException 방지
        }

        Map<String, List<String>> fillingTypeMap = new HashMap<>();

        // ✅ 프론트에서 보내는 값과 DB 저장 값을 매핑 (복수 개 반환)
        fillingTypeMap.put("10-Q", List.of("10-Q"));
        fillingTypeMap.put("8-K", List.of("8-K", "8-K/A"));
        fillingTypeMap.put("S-1", List.of("S-1"));
        fillingTypeMap.put("4", List.of("4", "4/A"));
        fillingTypeMap.put("SC-13G", List.of("SC 13G", "SC 13D"));  // SC-13G → SC 13G, SC 13D

        return fillingTypeMap.getOrDefault(fillingType, List.of(fillingType)); // 기본적으로 동일한 값 반환
    }
}