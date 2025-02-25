package finpago.fillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FillingsResponseDto {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<FillingResponseDto> content;
}