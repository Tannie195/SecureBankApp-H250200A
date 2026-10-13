package securebank.repository;

import securebank.model.User;
import securebank.util.FileStorageUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Users in {@code users.txt}: {@code username|passwordHash|createdAt}
 * Loaded once at start-up; every change is written to the file first, then to memory
 */
public final class FileUserRepository implements UserRepository {

    private final Path file;
    private Map<String, User> users = new LinkedHashMap<>(); // key: lower-case username

    public FileUserRepository(Path file) {
        this.file = file;
        int skipped = 0;
        for (String line : FileStorageUtil.readAllLines(file)) {
            try {
                List<String> f = RecordCodec.decode(line);
                if (f.size() != 3 || users.containsKey(key(f.get(0)))) {
                    throw new IllegalArgumentException();
                }
                users.put(key(f.get(0)), new User(f.get(0), f.get(1), LocalDateTime.parse(f.get(2))));
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        if (skipped > 0) {
            System.err.println("[WARN] users.txt: " + skipped + " unreadable record(s) skipped.");
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(key(username)));
    }

    @Override
    public void save(User user) {
        if (users.containsKey(key(user.getUsername()))) {
            throw new IllegalStateException("Username already exists.");
        }
        Map<String, User> next = new LinkedHashMap<>(users);
        next.put(key(user.getUsername()), user);
        List<String> lines = new ArrayList<>();
        for (User u : next.values()) {
            lines.add(RecordCodec.encode(Arrays.asList(u.getUsername(), u.getPasswordHash(), u.getCreatedAt().toString())));
        }
        FileStorageUtil.writeAllLinesAtomically(file, lines); // write file first ...
        users = next;                                         // ... then memory
    }

    @Override
    public int count() {
        return users.size();
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
