package finpago.userservice.pinnedStock.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockInfo {
    private String name;
    private int price;
    private double change;
}
