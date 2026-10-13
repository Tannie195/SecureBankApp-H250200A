package securebank.test;

import securebank.exception.AccountLockedException;
import securebank.exception.AuthenticationException;
import securebank.exception.ValidationException;
import securebank.model.User;
import securebank.repository.FileUserRepository;
import securebank.repository.UserRepository;
import securebank.security.LoginAttemptTracker;
import securebank.service.AuthenticationService;
import securebank.util.AuditLogger;

import java.nio.file.Path;

import static securebank.test.Assert.*;
import static securebank.test.TestRunner.suite;
import static securebank.test.TestRunner.test;
import static securebank.test.TestSupport.*;

final class AuthenticationServiceTests {

    private static final String GOOD = "Tr1cky-Horse-Battery!";

    private AuthenticationServiceTests() {
    }

    private static AuthenticationService service(Path dir, MutableClock clock) {
        UserRepository users = new FileUserRepository(dir.resolve("users.txt"));
        return new AuthenticationService(users, fastHasher(), new LoginAttemptTracker(clock),
                AuditLogger.disabled(), clock);
    }

    private static void fail(AuthenticationService s, String user) {
        try {
            s.login(user, pw("Wrong-Pass-1!"));
        } catch (AuthenticationException expected) {
            // expected
        }
    }

    static void run() {
        suite("AuthenticationService (login system)");

        test("register then login works; the name keeps its registered spelling", () -> {
            Path dir = tempDir();
            try {
                AuthenticationService s = service(dir, new MutableClock());
                s.register("Alice", pw(GOOD));
                assertEquals("Alice", s.login("alice", pw(GOOD)).getUsername(), "logged in");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("only a hash is stored, never the password", () -> {
            Path dir = tempDir();
            try {
                UserRepository users = new FileUserRepository(dir.resolve("users.txt"));
                new AuthenticationService(users, fastHasher(), new LoginAttemptTracker(new MutableClock()),
                        AuditLogger.disabled(), new MutableClock()).register("alice", pw(GOOD));
                User u = users.findByUsername("alice").get();
                assertNotContains(u.getPasswordHash(), GOOD, "no plaintext");
                assertTrue(u.getPasswordHash().startsWith("PBKDF2WithHmacSHA256$"), "PBKDF2");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("invalid usernames, weak passwords and duplicates are refused", () -> {
            Path dir = tempDir();
            try {
                AuthenticationService s = service(dir, new MutableClock());
                assertThrows(ValidationException.class, () -> s.register("a|b", pw(GOOD)), "bad username");
                assertThrows(ValidationException.class, () -> s.register("alice", pw("short1!")), "weak password");
                s.register("alice", pw(GOOD));
                assertThrows(ValidationException.class, () -> s.register("ALICE", pw(GOOD)), "duplicate");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("wrong password and unknown user give the same generic message", () -> {
            Path dir = tempDir();
            try {
                AuthenticationService s = service(dir, new MutableClock());
                s.register("alice", pw(GOOD));
                String wrong = assertThrows(AuthenticationException.class, () -> s.login("alice", pw("Wrong-Pass-1!")), "wrong").getMessage();
                String unknown = assertThrows(AuthenticationException.class, () -> s.login("nobody", pw("Wrong-Pass-1!")), "unknown").getMessage();
                assertEquals(wrong, unknown, "identical messages");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("a fresh system has no built-in or demo users", () -> {
            Path dir = tempDir();
            try {
                AuthenticationService s = service(dir, new MutableClock());
                assertThrows(AuthenticationException.class, () -> s.login("demo_user", pw("Demo1234Pass")), "demo");
                assertThrows(AuthenticationException.class, () -> s.login("admin", pw("Admin1234!")), "admin");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("three wrong passwords lock the username, even for the right password", () -> {
            Path dir = tempDir();
            try {
                AuthenticationService s = service(dir, new MutableClock());
                s.register("alice", pw(GOOD));
                fail(s, "alice");
                fail(s, "alice");
                assertThrows(AccountLockedException.class, () -> s.login("alice", pw("Wrong-Pass-1!")), "third failure");
                assertThrows(AccountLockedException.class, () -> s.login("alice", pw(GOOD)), "locked");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("the lock ends after 2 minutes", () -> {
            Path dir = tempDir();
            try {
                MutableClock clock = new MutableClock();
                AuthenticationService s = service(dir, clock);
                s.register("alice", pw(GOOD));
                fail(s, "alice");
                fail(s, "alice");
                fail(s, "alice");
                clock.advanceSeconds(121);
                assertEquals("alice", s.login("alice", pw(GOOD)).getUsername(), "can log in again");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("unknown usernames get locked too (nothing is revealed)", () -> {
            Path dir = tempDir();
            try {
                AuthenticationService s = service(dir, new MutableClock());
                fail(s, "ghost");
                fail(s, "ghost");
                assertThrows(AccountLockedException.class, () -> s.login("ghost", pw("Wrong-Pass-1!")), "locked");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("password arrays are wiped after use", () -> {
            Path dir = tempDir();
            try {
                AuthenticationService s = service(dir, new MutableClock());
                char[] reg = pw(GOOD);
                s.register("alice", reg);
                assertEquals(0, new String(reg).replace("\0", "").length(), "register wipes");
                char[] attempt = pw(GOOD);
                s.login("alice", attempt);
                assertEquals(0, new String(attempt).replace("\0", "").length(), "login wipes");
            } finally {
                deleteRecursively(dir);
            }
        });
    }
}
