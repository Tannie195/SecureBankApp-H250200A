package securebank.exception;

/**
 * Thrown when a login attempt fails ,message is always generic
 * ("Invalid username or password.")*/
public class AuthenticationException extends Exception {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
