package securebank.test;

import securebank.security.PasswordHasher;

import static securebank.test.Assert.*;
import static securebank.test.TestRunner.suite;
import static securebank.test.TestRunner.test;
import static securebank.test.TestSupport.fastHasher;
import static securebank.test.TestSupport.pw;

final class PasswordHasherTests {

    private PasswordHasherTests() {
    }

    static void run() {
        suite("PasswordHasher (PBKDF2 + salt)");

        test("correct password verifies, wrong one does not", () -> {
            PasswordHasher h = fastHasher();
            String stored = h.hash(pw("Tr1cky-Horse!"));
            assertTrue(h.verify(pw("Tr1cky-Horse!"), stored), "correct");
            assertFalse(h.verify(pw("Tr1cky-Horse?"), stored), "wrong");
        });

        test("same password gives different hashes (random salt)", () ->
                assertFalse(fastHasher().hash(pw("Same-Pass-1!")).equals(fastHasher().hash(pw("Same-Pass-1!"))), "salted"));

        test("stored value has the expected format and never contains the password", () -> {
            String stored = fastHasher().hash(pw("Secret-Value-9!"));
            assertTrue(stored.startsWith("PBKDF2WithHmacSHA256$1000$"), "format");
            assertNotContains(stored, "Secret-Value-9!", "no plaintext");
        });

        test("production strength is 600,000 iterations", () ->
                assertEquals(600_000, PasswordHasher.DEFAULT_ITERATIONS, "iterations"));

        test("malformed stored values give false and never throw", () -> {
            for (String bad : new String[]{null, "", "garbage", "a$b$c$d", "PBKDF2WithHmacSHA256$99999999$AAAA$AAAA",
                    "PBKDF2WithHmacSHA256$1000$!!!$!!!"}) {
                assertFalse(fastHasher().verify(pw("x"), bad), "<" + bad + ">");
            }
        });
    }
}
