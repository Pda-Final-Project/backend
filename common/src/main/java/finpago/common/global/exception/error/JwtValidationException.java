package finpago.common.global.exception.error;

//JWT 검증 실패 예외
public class JwtValidationException extends RuntimeException {
    public JwtValidationException() {
        super();
    }

    public JwtValidationException(String message) {
        super(message);
    }

    public JwtValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JwtValidationException(Throwable cause) {
        super(cause);
    }
}
