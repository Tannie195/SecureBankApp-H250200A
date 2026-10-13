package securebank.util;

import securebank.exception.PersistenceException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

//Safe text-file reading and writing failures become a PersistenceException.
public final class FileStorageUtil {

    private FileStorageUtil() {
    }

    //Creates the folder and an empty file if they do not exist yet
    public static void ensureFileExists(Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            throw new PersistenceException("Unable to create data file: " + file.getFileName(), e);
        }
    }

    // All non-blank lines of the file
    public static List<String> readAllLines(Path file) {
        ensureFileExists(file);
        try {
            List<String> lines = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException e) {
            throw new PersistenceException("Unable to read data file: " + file.getFileName(), e);
        }
    }

    //Replaces the file in one step (temp file, then rename), so a crash cannot leave it half-written
    public static void writeAllLinesAtomically(Path file, List<String> lines) {
        ensureFileExists(file);
        Path temp = null;
        try {
            temp = Files.createTempFile(file.toAbsolutePath().getParent(), file.getFileName() + ".", ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicNotSupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenceException("Unable to write data file: " + file.getFileName(), e);
        } finally {
            try {
                if (temp != null) {
                    Files.deleteIfExists(temp);
                }
            } catch (IOException ignored) {
                // Nothing more can be done.
            }
        }
    }

    //Adds one line to the end of the file
    public static void appendLine(Path file, String line) {
        ensureFileExists(file);
        try {
            Files.write(file, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new PersistenceException("Unable to append to data file: " + file.getFileName(), e);
        }
    }
}
