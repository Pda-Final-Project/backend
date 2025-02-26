package finpago.fillingservice.dto;

import finpago.fillingservice.entity.Filling;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FillingResponseDto {
    private String fillingId;
    private String fillingTitle;
    private String fillingType;
    private String fillingTicker;
    private String fillingUrl;
    private String fillingFileType;
    private String fillingTranslatedContentUrl;
    private String filling10qJsonUrl;
    private String submitTimestamp;

    public FillingResponseDto(Filling filling) {
        this.fillingId = filling.getFillingId();
        this.fillingTitle = filling.getFillingTitle();
        this.fillingType = filling.getFillingType();
        this.fillingTicker = filling.getFillingTicker();
        this.fillingUrl = filling.getFillingUrl();
        this.fillingFileType = filling.getFillingFileType();
        this.fillingTranslatedContentUrl = filling.getFillingTranslatedContentUrl();
        this.filling10qJsonUrl = filling.getFilling10qJsonUrl();
        this.submitTimestamp = filling.getSubmitTimestamp();
    }
}