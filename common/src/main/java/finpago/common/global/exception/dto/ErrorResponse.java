package finpago.common.global.exception.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ErrorResponse {
    private String code;    // 예외 코드
    private String message; // 예외 메시지
    private int status;     // HTTP 상태 코드
}