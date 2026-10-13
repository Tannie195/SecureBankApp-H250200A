package securebank.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Brute-force protection: after 3 wrong passwords a username is locked for 2 minutes
 * Unknown usernames are tracked the same way, so the lock reveals nothing about who is registered
 */
public final class LoginAttemptTracker {

    public static final int MAX_ATTEMPTS = 3;
    public static final Duration LOCKOUT = Duration.ofMinutes(2);

    private final Clock clock;
    private final Map<String, Integer> failures = new HashMap<>();
    private final Map<String, Instant> lockedUntil = new HashMap<>();

    public LoginAttemptTracker(Clock clock) {
        this.clock = clock;
    }

    //Seconds left on the lock, or 0 if the username is not locked
    public synchronized long secondsLocked(String username) {
        Instant until = lockedUntil.get(key(username));
        if (until == null) {
            return 0;
        }
        long seconds = (long) Math.ceil(Duration.between(clock.instant(), until).toMillis() / 1000.0);
        if (seconds <= 0) {
            lockedUntil.remove(key(username));
            return 0;
        }
        return seconds;
    }

    //Counts a failed attempt & Returns true if this one caused a lock
    public synchronized boolean recordFailure(String username) {
        int count = failures.merge(key(username), 1, Integer::sum);
        if (count < MAX_ATTEMPTS) {
            return false;
        }
        failures.remove(key(username));
        lockedUntil.put(key(username), clock.instant().plus(LOCKOUT));
        return true;
    }

    public synchronized void reset(String username) {
        failures.remove(key(username));
        lockedUntil.remove(key(username));
    }

    private static String key(String username) {
        String name = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return name.length() > 64 ? name.substring(0, 64) : name;
    }
}
