package finpago.common.global.exception.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 로그인 & 인증 관련 예외
    LOGIN_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E001", "잘못된 전화번호 또는 비밀번호입니다."),
    LOGIN_MISSING_FIELDS(HttpStatus.BAD_REQUEST, "E002", "필수 입력값이 누락되었습니다."),
    TOKEN_EXPIRED(HttpStatus.FORBIDDEN, "E003", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.FORBIDDEN, "E004", "토큰 검증이 실패되었습니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "E019", "계정이 정지되었습니다."),

    // 회원가입 관련
    SIGNUP_DUPLICATE_USER(HttpStatus.CONFLICT, "E004", "이미 존재하는 전화번호입니다."),
    SIGNUP_MISSING_FIELDS(HttpStatus.BAD_REQUEST, "E020", "필수 입력값이 누락되었습니다."),
    SIGNUP_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "E021", "비밀번호는 최소 8자 이상, 특수문자를 포함해야 합니다."),

    // 주문 관련
    INSUFFICIENT_FUNDS(HttpStatus.BAD_REQUEST, "E002", "예수금이 부족합니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "E005", "주문을 찾을 수 없습니다."),

    // 공시 관련
    FILLING_NOT_FOUND(HttpStatus.NOT_FOUND, "E012", "해당 공시를 찾을 수 없습니다."),

    // 알림 관련
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "E014", "알림이 존재하지 않습니다."),

    // 계좌 관련
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "E016", "계좌를 찾을 수 없습니다."),

    // 공통 서버
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}