package finpago.dataservice.earning.entity;

import lombok.*;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Earnings implements Serializable {
    @JsonProperty("date")
    private String date;            // 발표일
    @JsonProperty("eps")
    private String eps;             // 실제 EPS
    @JsonProperty("eps_estimated")
    private String epsEstimated;    // 예상 EPS
    @JsonProperty("revenue")
    private String revenue;         // 실제 매출
    @JsonProperty("revenue_estimated")
    private String revenueEstimated;// 예상 매출
    @JsonProperty("time")
    private String time;            // 발표 시간 (amc: 장 마감 후, bmo: 장 시작 전)
}