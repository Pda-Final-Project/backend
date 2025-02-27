package finpago.userservice.pinnedStock.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PinnedStockResDto {
    private String ticker;
    private String name;
    private int price;
    private double change;
}

