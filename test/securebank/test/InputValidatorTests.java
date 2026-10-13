package securebank.test;

import securebank.exception.ValidationException;
import securebank.util.InputValidator;

import static securebank.test.Assert.*;
import static securebank.test.TestRunner.suite;
import static securebank.test.TestRunner.test;
import static securebank.test.TestSupport.pw;

final class InputValidatorTests {

    private InputValidatorTests() {
    }

    static void run() {
        suite("InputValidator (allow-list validation)");

        test("valid username is accepted and trimmed", () ->
                assertEquals("alice_01", InputValidator.validateUsername("  alice_01 "), "username"));

        test("bad usernames are rejected", () -> {
            for (String bad : new String[]{null, "", "ab", "a_very_long_username_over_20", "has space",
                    "pipe|name", "new\nline", "semi;colon", "caf\u00e9"}) {
                assertThrows(ValidationException.class, () -> InputValidator.validateUsername(bad), "<" + bad + ">");
            }
        });

        test("valid amounts become 2-decimal numbers", () -> {
            assertEquals("100.00", InputValidator.validateAmount("100").toPlainString(), "whole");
            assertEquals("100.50", InputValidator.validateAmount("100.5").toPlainString(), "one decimal");
        });

        test("bad amounts are rejected", () -> {
            for (String bad : new String[]{null, "", "0", "-5", "+5", "1e5", "1.234", "1,000", "abc",
                    "99999999999", "1000000000.01"}) {
                assertThrows(ValidationException.class, () -> InputValidator.validateAmount(bad), "<" + bad + ">");
            }
        });

        test("account numbers must be exactly 10 digits", () -> {
            assertEquals("1234567890", InputValidator.validateAccountNumber(" 1234567890 "), "valid");
            for (String bad : new String[]{null, "", "123456789", "12345abcde", "1234567890; DROP TABLE x", "../../etc"}) {
                assertThrows(ValidationException.class, () -> InputValidator.validateAccountNumber(bad), "<" + bad + ">");
            }
        });

        test("menu choice: numbers parse, anything else is -1", () -> {
            assertEquals(3, InputValidator.parseMenuChoice(" 3 "), "valid");
            assertEquals(-1, InputValidator.parseMenuChoice("abc"), "text");
            assertEquals(-1, InputValidator.parseMenuChoice("99999999999"), "too long");
        });

        test("a strong password is accepted", () ->
                InputValidator.validatePassword(pw("Tr1cky-Horse-Battery!"), "alice"));

        test("weak passwords are rejected", () -> {
            for (String bad : new String[]{"Ab1!xyz", "lowercase-only-1!", "UPPERCASE-ONLY-1!",
                    "No-Digits-Here-Abc!", "NoSymbolsHere123", "My-Alice-Pass-1!"}) {
                assertThrows(ValidationException.class,
                        () -> InputValidator.validatePassword(pw(bad), "alice"), "<" + bad + ">");
            }
        });
    }
}
