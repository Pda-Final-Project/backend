package finpago.dataservice.exchangeRate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeRateDto {
    private String ticker;
    private Double exchangeRate;
}