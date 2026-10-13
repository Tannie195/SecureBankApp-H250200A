package securebank.test;

import java.math.BigDecimal;
import java.util.Objects;

//Minimal assertion helpers (no external test library needed)
public final class Assert {

    private Assert() {
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " (expected <" + expected + "> but was <" + actual + ">)");
        }
    }

    //Compares money by value, so 50 and 50.00 count as equal
    public static void assertMoney(String expected, BigDecimal actual, String message) {
        if (actual == null || new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError(message + " (expected <" + expected + "> but was <" + actual + ">)");
        }
    }

    public static void assertContains(String text, String fragment, String message) {
        if (text == null || !text.contains(fragment)) {
            throw new AssertionError(message + " (expected to find <" + fragment + ">)");
        }
    }

    public static void assertNotContains(String text, String fragment, String message) {
        if (text != null && text.contains(fragment)) {
            throw new AssertionError(message + " (did not expect to find <" + fragment + ">)");
        }
    }

    //Passes only if {@code body} throws {@code expected} (or a subclass of it)
    public static <T extends Throwable> T assertThrows(Class<T> expected, TestRunner.Body body, String message) {
        try {
            body.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) {
                return expected.cast(thrown);
            }
            throw new AssertionError(message + " (expected " + expected.getSimpleName()
                    + " but got " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage() + ")");
        }
        throw new AssertionError(message + " (expected " + expected.getSimpleName() + " but nothing was thrown)");
    }
}
