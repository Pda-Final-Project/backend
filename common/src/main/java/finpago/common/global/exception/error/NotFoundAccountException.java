package finpago.common.global.exception.error;

//계좌를 찾을 수 없는 경우
public class NotFoundAccountException extends RuntimeException {
    public NotFoundAccountException() {
        super();
    }

    public NotFoundAccountException(String message) {
        super(message);
    }

    public NotFoundAccountException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundAccountException(Throwable cause) {
        super(cause);
    }
}

