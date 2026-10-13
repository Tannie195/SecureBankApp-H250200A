package securebank.util;

import securebank.exception.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * All input validation uses allow-lists: input is accepted only if it matches
 * exactly what is expected otherwise it is rejected
 */
public final class InputValidator {

    public static final int PASSWORD_MIN = 10;
    public static final int PASSWORD_MAX = 128;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern AMOUNT = Pattern.compile("^[0-9]{1,10}(\\.[0-9]{1,2})?$");
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("^[0-9]{10}$");

    private InputValidator() {
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME.matcher(username).matches();
    }

    public static boolean isValidAccountNumber(String accountNumber) {
        return accountNumber != null && ACCOUNT_NUMBER.matcher(accountNumber).matches();
    }

    // Returns the trimmed username: 3-20 letters, digits or underscores
    public static String validateUsername(String raw) throws ValidationException {
        String username = raw == null ? "" : raw.trim();
        if (!isValidUsername(username)) {
            throw new ValidationException("Username must be 3-20 characters: letters, digits or underscores only.");
        }
        return username;
    }

    // Password rules: 10-128 characters with upper, lower, digit and symbol; must not contain the username
    public static void validatePassword(char[] password, String username) throws ValidationException {
        if (password == null || password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
            throw new ValidationException("Password must be " + PASSWORD_MIN + "-" + PASSWORD_MAX + " characters long.");
        }
        boolean upper = false, lower = false, digit = false, symbol = false;
        for (char c : password) {
            if (Character.isISOControl(c)) {
                throw new ValidationException("Password must not contain control characters.");
            }
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else if (!Character.isLetter(c)) symbol = true;
        }
        if (!(upper && lower && digit && symbol)) {
            throw new ValidationException("Password needs an uppercase letter, a lowercase letter, a digit and a symbol.");
        }
        if (username != null && username.length() >= 3
                && new String(password).toLowerCase().contains(username.toLowerCase())) {
            throw new ValidationException("Password must not contain your username.");
        }
    }

    // Parses an amount: plain number, at most 2 decimals, above zero. Result always has 2 decimals
    public static BigDecimal validateAmount(String raw) throws ValidationException {
        String text = raw == null ? "" : raw.trim();
        if (!AMOUNT.matcher(text).matches()) {
            throw new ValidationException("Amount must be a plain number with at most 2 decimals (e.g. 100.00).");
        }
        BigDecimal amount = new BigDecimal(text).setScale(2);
        if (amount.signum() <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException("Amount is too large.");
        }
        return amount;
    }

    // Returns the trimmed account number: exactly 10 digits.
    public static String validateAccountNumber(String raw) throws ValidationException {
        String number = raw == null ? "" : raw.trim();
        if (!isValidAccountNumber(number)) {
            throw new ValidationException("Account number must be exactly 10 digits.");
        }
        return number;
    }

    // Menu input as a number, or -1 if it is not a short whole number
    public static int parseMenuChoice(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty() || text.length() > 3) {
            return -1;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
