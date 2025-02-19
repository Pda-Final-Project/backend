package finpago.common.global.exception.error;

//주문을 찾을 수 없는 경우
public class NotFoundOrderException extends RuntimeException {
    public NotFoundOrderException() {
        super();
    }

    public NotFoundOrderException(String message) {
        super(message);
    }

    public NotFoundOrderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundOrderException(Throwable cause) {
        super(cause);
    }
}

