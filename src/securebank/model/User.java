package securebank.model;

import java.time.LocalDateTime;
import java.util.Objects;

// A registered user only the password hash is kept, never the password itself.
public final class User {

    private final String username;
    private final String passwordHash;
    private final LocalDateTime createdAt;

    public User(String username, String passwordHash, LocalDateTime createdAt) {
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getUsername() { return username; }

    public String getPasswordHash() { return passwordHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }

// Leaves out the hash so logging a User cannot leak it.
    @Override
    public String toString() {
        return "User{username='" + username + "'}";
    }
}
