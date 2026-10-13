package securebank.exception;

/**
 * Thrown when user-supplied data breaks a validation rule, such as,a weak password,
 * a malformed amount, or a username that is already taken*/
public class ValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
