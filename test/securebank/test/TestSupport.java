package securebank.test;

import securebank.security.PasswordHasher;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

// Test helpers: a clock the tests can move, temp folders and a fast password hasher
final class TestSupport {

    private TestSupport() {
    }

    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }

        @Override public Clock withZone(ZoneId zone) { return this; }

        @Override public Instant instant() { return now; }
    }

    // Real PBKDF2 with few iterations, so tests run quickly
    static PasswordHasher fastHasher() {
        return new PasswordHasher(1_000);
    }

    static char[] pw(String text) {
        return text.toCharArray();
    }

    static Path tempDir() throws IOException {
        return Files.createTempDirectory("securebank-test");
    }

    static void deleteRecursively(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                    Files.deleteIfExists(f);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                    Files.deleteIfExists(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // temp files only
        }
    }
}
