package finpago.common.global.exception.error;

// 특정 종목의 공시 정보를 찾을 수 없는 경우
public class NotFoundFillingException extends RuntimeException {
    public NotFoundFillingException() {
        super();
    }

    public NotFoundFillingException(String message) {
        super(message);
    }

    public NotFoundFillingException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundFillingException(Throwable cause) {
        super(cause);
    }
}
