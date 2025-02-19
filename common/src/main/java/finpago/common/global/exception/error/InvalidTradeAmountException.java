package finpago.common.global.exception.error;

//잘못된 거래수량
public class InvalidTradeAmountException extends RuntimeException {
    public InvalidTradeAmountException() {
        super();
    }

    public InvalidTradeAmountException(String message) {
        super(message);
    }

    public InvalidTradeAmountException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTradeAmountException(Throwable cause) {
        super(cause);
    }
}

