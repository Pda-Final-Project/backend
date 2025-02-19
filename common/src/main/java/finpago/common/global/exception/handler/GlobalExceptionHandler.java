package finpago.common.global.exception.handler;

/*사용법
if (userBalance.compareTo(orderAmount) < 0) {
            throw new InsufficientBalanceException("예수금이 부족합니다.");
        }*/

import finpago.common.global.exception.dto.CommonResponse;
import finpago.common.global.exception.dto.ErrorResponse;
import finpago.common.global.exception.error.ErrorCode;
import finpago.common.global.exception.error.InsufficientBalanceException;
import finpago.common.global.exception.error.NotFoundAccountException;
import finpago.common.global.exception.error.NotFoundOrderException;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 필수 입력값 누락 예외 처리 (로그인 & 회원가입)
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<CommonResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.error("필수 입력값이 누락되었습니다.");

        ErrorCode errorCode = ErrorCode.LOGIN_MISSING_FIELDS;

        ErrorResponse error = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(errorCode.getMessage())
                .code(errorCode.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // JWT 토큰 만료 예외 처리
    @ExceptionHandler(ExpiredJwtException.class)
    protected ResponseEntity<CommonResponse> handleExpiredJwtException(ExpiredJwtException exception) {
        log.error("JWT 토큰 만료");

        ErrorCode errorCode = ErrorCode.TOKEN_EXPIRED;

        ErrorResponse error = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(errorCode.getMessage())
                .code(errorCode.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 로그인 실패 예외 처리 (잘못된 아이디 또는 비밀번호)
    @ExceptionHandler(SecurityException.class)
    protected ResponseEntity<CommonResponse> handleSecurityException(SecurityException exception) {
        log.error("로그인 실패 (잘못된 아이디 또는 비밀번호)");

        ErrorCode errorCode = ErrorCode.LOGIN_INVALID_CREDENTIALS;

        ErrorResponse error = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(errorCode.getMessage())
                .code(errorCode.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 계좌를 찾을 수 없는 예외 처리
    @ExceptionHandler(NotFoundAccountException.class)
    protected ResponseEntity<CommonResponse> handleNotFoundAccountException(NotFoundAccountException exception) {
        log.error("계좌를 찾을 수 없습니다.");

        ErrorCode errorCode = ErrorCode.ACCOUNT_NOT_FOUND;

        ErrorResponse error = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(errorCode.getMessage())
                .code(errorCode.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 주문을 찾을 수 없는 예외 처리
    @ExceptionHandler(NotFoundOrderException.class)
    protected ResponseEntity<CommonResponse> handleNotFoundOrderException(NotFoundOrderException exception) {
        log.error("주문을 찾을 수 없습니다.");

        ErrorCode errorCode = ErrorCode.ORDER_NOT_FOUND;

        ErrorResponse error = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(errorCode.getMessage())
                .code(errorCode.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    //예수금 부족 예외 처리
    @ExceptionHandler(InsufficientBalanceException.class)
    protected ResponseEntity<CommonResponse> handleInsufficientBalanceException(InsufficientBalanceException exception) {
        log.error("예수금이 부족합니다.");

        ErrorCode errorCode = ErrorCode.INSUFFICIENT_FUNDS;

        ErrorResponse error = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(errorCode.getMessage())
                .code(errorCode.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 예상치 못한 예외 처리
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<CommonResponse> handleGeneralException(Exception exception) {
        log.error("[서버 오류] {}", exception.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(response);
    }
}