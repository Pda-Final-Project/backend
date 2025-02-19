package finpago.common.global.exception.error;

//체결 내역을 찾을 수 없는 경우
public class NotFoundTradeException extends RuntimeException {
    public NotFoundTradeException() {
        super();
    }

    public NotFoundTradeException(String message) {
        super(message);
    }

    public NotFoundTradeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundTradeException(Throwable cause) {
        super(cause);
    }
}

