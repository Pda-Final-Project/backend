package finpago.dataservice.earning.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Earnings implements Serializable {

    private String date;            // 발표일
    private String eps;             // 실제 EPS
    private String epsEstimated;    // 예상 EPS
    private String revenue;         // 실제 매출
    private String revenueEstimated;// 예상 매출
    private String time;            // 발표 시간 (amc: 장 마감 후, bmo: 장 시작 전)
}
