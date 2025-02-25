package finpago.fillingservice.service;

import finpago.fillingservice.dto.FillingResponseDto;
import finpago.fillingservice.dto.FillingsResponseDto;

public interface FillingService {
    FillingsResponseDto getFillingsByTicker(String ticker, String fillingType, int page, int size);

    FillingResponseDto getFilling(String fillingId);
}
