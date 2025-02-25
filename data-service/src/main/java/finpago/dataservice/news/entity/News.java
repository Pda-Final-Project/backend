package finpago.dataservice.news.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class News implements Serializable {

    @JsonProperty("news_title")
    private String newsTitle;

    @JsonProperty("news_company")
    private String newsCompany;

    @JsonProperty("news_img")
    private String newsImg;

    @JsonProperty("news_url")
    private String newsUrl;

    @JsonProperty("news_date")
    private String newsDate;
}