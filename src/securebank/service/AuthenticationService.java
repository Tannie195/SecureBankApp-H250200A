package securebank.service;

import securebank.exception.AccountLockedException;
import securebank.exception.AuthenticationException;
import securebank.exception.ValidationException;
import securebank.model.User;
import securebank.repository.UserRepository;
import securebank.security.LoginAttemptTracker;
import securebank.security.PasswordHasher;
import securebank.util.AuditLogger;
import securebank.util.InputValidator;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

/**
 * Registration and login
 *  Thus login gives one generic error for wrong password and unknown user, does the same hashing work
 * in both cases and locks a username after repeated failures
 */
public class AuthenticationService {

    private static final String GENERIC_FAILURE = "Invalid username or password.";

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final LoginAttemptTracker attempts;
    private final AuditLogger audit;
    private final Clock clock;
    private final String dummyHash; // checked when the username does not exist, to keep timing equal

    public AuthenticationService(UserRepository users, PasswordHasher hasher, LoginAttemptTracker attempts,
                                 AuditLogger audit, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.attempts = attempts;
        this.audit = audit;
        this.clock = clock;
        this.dummyHash = hasher.hash("not-a-real-password".toCharArray());
    }

    // @throws ValidationException if the username or password breaks the rules, or the username is taken
    public void register(String rawUsername, char[] password) throws ValidationException {
        try {
            String username = InputValidator.validateUsername(rawUsername);
            InputValidator.validatePassword(password, username);
            if (users.findByUsername(username).isPresent()) {
                throw new ValidationException("That username is already taken.");
            }
            users.save(new User(username, hasher.hash(password), LocalDateTime.now(clock)));
            audit.log("REGISTER", username, "New user registered");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * @return the logged-in user
     * @throws AccountLockedException  if the username is locked
     * @throws AuthenticationException for any other failure (generic message)
     */
    public User login(String rawUsername, char[] password) throws AuthenticationException {
        String username = rawUsername == null ? "" : rawUsername.trim();
        try {
            long locked = attempts.secondsLocked(username);
            if (locked > 0) {
                audit.log("LOGIN_BLOCKED", username, "Attempt while locked");
                throw new AccountLockedException("Login is locked for " + locked + " more second(s).");
            }
            Optional<User> user = users.findByUsername(username);
            boolean tooLong = password.length > InputValidator.PASSWORD_MAX;
            boolean passwordOk = !tooLong
                    && hasher.verify(password, user.map(User::getPasswordHash).orElse(dummyHash));
            if (!user.isPresent() || !passwordOk) {
                boolean nowLocked = attempts.recordFailure(username);
                audit.log("LOGIN_FAILED", username, "Invalid credentials");
                if (nowLocked) {
                    audit.log("ACCOUNT_LOCKED", username, "Too many failed attempts");
                    throw new AccountLockedException("Too many failed attempts. Login is locked for "
                            + LoginAttemptTracker.LOCKOUT.getSeconds() + " seconds.");
                }
                throw new AuthenticationException(GENERIC_FAILURE);
            }
            attempts.reset(username);
            audit.log("LOGIN_OK", user.get().getUsername(), "Login successful");
            return user.get();
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
