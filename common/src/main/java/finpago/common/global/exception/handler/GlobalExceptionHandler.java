package finpago.common.global.exception.handler;

/*사용법
if (userBalance.compareTo(orderAmount) < 0) {
            throw new InsufficientBalanceException("예수금이 부족합니다.");
        }*/

import finpago.common.global.exception.dto.CommonResponse;
import finpago.common.global.exception.dto.ErrorResponse;
import finpago.common.global.exception.error.*;
import lombok.extern.slf4j.Slf4j;
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
        log.error("JWT 토큰 만료: {}", exception.getMessage());

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

    // JWT 검증 실패 예외 처리
    @ExceptionHandler(JwtValidationException.class)
    protected ResponseEntity<CommonResponse> handleJwtValidationException(JwtValidationException exception) {
        log.error("JWT 검증 실패: {}", exception.getMessage());

        ErrorCode errorCode = ErrorCode.TOKEN_INVALID;

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

    // 중복된 전화번호 예외 처리
    @ExceptionHandler(DuplicateUserPhoneException.class)
    protected ResponseEntity<CommonResponse> handleDuplicateUserPhoneException(DuplicateUserPhoneException exception) {
        log.error("회원가입 실패 (중복된 전화번호)");

        ErrorCode errorCode = ErrorCode.SIGNUP_DUPLICATE_USER;

        ErrorResponse error = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(exception.getMessage())
                .code(errorCode.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 등록되지 않은 사용자 예외 처리
    @ExceptionHandler(UserNotFoundException.class)
    protected ResponseEntity<CommonResponse> handleUserNotFoundException(UserNotFoundException exception) {
        log.error("로그인 실패 (미등록 전화번호)");

        ErrorCode errorCode = ErrorCode.LOGIN_INVALID_CREDENTIALS;

        ErrorResponse error = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(exception.getMessage())
                .code(errorCode.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 잘못된 비밀번호 예외 처리
    @ExceptionHandler(InvalidCredentialsException.class)
    protected ResponseEntity<CommonResponse> handleInvalidCredentialsException(InvalidCredentialsException exception) {
        log.error("로그인 실패 (잘못된 비밀번호)");

        ErrorCode errorCode = ErrorCode.LOGIN_INVALID_CREDENTIALS;

        ErrorResponse error = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(exception.getMessage())
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
                .message(exception.getMessage())
                .code(errorCode.getCode())
                .build();

        CommonResponse response = CommonResponse.builder()
                .success(false)
                .error(error)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 존재하지 않는 티커로 공시 리스트 조회
    @ExceptionHandler(NotFoundFillingException.class)
    protected ResponseEntity<CommonResponse> handleNotFoundFillingsByTicker(NotFoundFillingException exception) {
        log.error("해당 종목의 보고서가 존재하지 않습니다.");

        ErrorCode errorCode = ErrorCode.FILLING_NOT_FOUND;

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