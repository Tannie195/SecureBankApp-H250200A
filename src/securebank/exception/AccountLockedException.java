package securebank.exception;

/**
 {@link AuthenticationException}, thrown when a username has
 * been temporarily locked after too many failed login attempts
 */
public class AccountLockedException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public AccountLockedException(String message) {
        super(message);
    }
}
