package finpago.common.global.exception.error;

//JWT 만료 예외
public class ExpiredJwtException extends RuntimeException {
    public ExpiredJwtException() {
        super();
    }

    public ExpiredJwtException(String message) {
        super(message);
    }

    public ExpiredJwtException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExpiredJwtException(Throwable cause) {
        super(cause);
    }
}
