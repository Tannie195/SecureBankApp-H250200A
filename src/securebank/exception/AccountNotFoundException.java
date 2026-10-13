package securebank.exception;

/**
 * Thrown when an account number does not exist, or exists but does not belong
 * to the logged-in user,thus  noone can examine other customers' account numbers
 */
public class AccountNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public AccountNotFoundException(String message) {
        super(message);
    }
}
