package securebank.exception;

// Thrown when a withdrawal is larger than the funds available in the account
public class InsufficientFundsException extends Exception {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String message) {
        super(message);
    }
}
