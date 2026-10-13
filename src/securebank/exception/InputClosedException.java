package securebank.exception;

/**
 * Thrown when the input stream ends (ie Ctrl+D / Ctrl+Z), so the
 * program can exit cleanly instead of looping forever on empty input
 */
public class InputClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InputClosedException() {
        super("Input stream closed.");
    }
}
