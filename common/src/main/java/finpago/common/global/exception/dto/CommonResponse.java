package finpago.common.global.exception.dto;

import lombok.*;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class CommonResponse {
    @Builder.Default
    private String id = UUID.randomUUID().toString(); // 요청 트래킹 ID
    @Builder.Default
    private Date dateTime = new Date(); // 예외 발생 시간
    private boolean success; // API 성공 여부
    private Object response; // 성공 시 응답 데이터
    private Object error; // 실패 시 에러 정보
}
