package finpago.fillingservice.service;

import finpago.fillingservice.dto.FillingResponseDto;
import finpago.fillingservice.dto.FillingsResponseDto;

public interface FillingService {
    FillingsResponseDto getFillings(String ticker, String fillingType, String startDate, String endDate, int page, int size);

    FillingResponseDto getFilling(String fillingId);
}
