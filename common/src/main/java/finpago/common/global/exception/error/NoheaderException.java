package finpago.common.global.exception.error;

//header존재하지않음 에러
public class NoheaderException extends RuntimeException {
    public NoheaderException() {
        super();
    }

    public NoheaderException(String message) {
        super(message);
    }

    public NoheaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoheaderException(Throwable cause) {
        super(cause);
    }
}
