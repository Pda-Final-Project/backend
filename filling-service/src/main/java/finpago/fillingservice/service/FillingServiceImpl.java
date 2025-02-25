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

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FillingServiceImpl implements FillingService {

    private final FillingRepository fillingRepository;

    @Override
    public FillingsResponseDto getFillingsByTicker(String ticker, String fillingType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Filling> fillingsPage = (fillingType == null)
                ? fillingRepository.findByFillingTickerOrderBySubmitTimestampDesc(ticker, pageable)
                : fillingRepository.findByFillingTickerAndFillingTypeOrderBySubmitTimestampDesc(ticker, fillingType, pageable);

        if (fillingsPage.isEmpty()) {
            throw new NotFoundFillingException("해당 티커의 공시가 존재하지 않습니다.");
        }

        FillingsResponseDto responseDto = FillingsResponseDto.builder()
                .page(fillingsPage.getNumber())
                .size(fillingsPage.getSize())
                .totalElements(fillingsPage.getTotalElements())
                .totalPages(fillingsPage.getTotalPages())
                .content(fillingsPage.getContent().stream()
                        .map(FillingResponseDto::new)
                        .collect(Collectors.toList()))
                .build();
        return responseDto;
    }

    @Override
    public FillingResponseDto getFilling(String fillingId) {
        Filling filling = fillingRepository.findByFillingId(fillingId)
                .orElseThrow(() -> new NotFoundFillingException("해당 공시가 존재하지 않습니다."));
        return new FillingResponseDto(filling);
    }
}