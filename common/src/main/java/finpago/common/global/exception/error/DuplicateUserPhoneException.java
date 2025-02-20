package finpago.common.global.exception.error;

public class DuplicateUserPhoneException extends RuntimeException {
    public DuplicateUserPhoneException() {
        super();
    }

    public DuplicateUserPhoneException(String message) {
        super(message);
    }

    public DuplicateUserPhoneException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateUserPhoneException(Throwable cause) {
        super(cause);
    }
}