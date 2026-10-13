package securebank.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Password hashing with PBKDF2-HMAC-SHA256 and a random salt per password
 * Stored format: {@code PBKDF2WithHmacSHA256$iterations$salt$hash} (so strength can be raised later)
 */
public final class PasswordHasher {

    public static final int DEFAULT_ITERATIONS = 600_000; // OWASP recommendation
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int MAX_ITERATIONS = 5_000_000;   // refuses absurd values from a tampered file
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int iterations;

    public PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    // A lower count is only for fast unit tests
    public PasswordHasher(int iterations) {
        if (iterations < 1 || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException("Iteration count out of range.");
        }
        this.iterations = iterations;
    }

    public String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, iterations);
        return ALGORITHM + "$" + iterations + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    // True if the password matches the stored hash a malformed stored value gives false, never an error
    public boolean verify(char[] password, String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$", -1);
        if (parts.length != 4 || !ALGORITHM.equals(parts[0])) {
            return false;
        }
        try {
            int storedIterations = Integer.parseInt(parts[1]);
            if (storedIterations < 1 || storedIterations > MAX_ITERATIONS) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(derive(password, salt, storedIterations), expected); // constant time
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterationCount) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterationCount, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is unavailable.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
