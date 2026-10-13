package securebank.util;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Security log: who did what, and when. Never records passwords, hashes or full account numbers
 * Text is limited to safe characters so users cannot forge log lines. A logging failure never stops the app
 * Line format: {@code timestamp|EVENT|username|detail}
 */
public final class AuditLogger {

    private final Path file; // null = logging off (used in tests)

    private AuditLogger(Path file) {
        this.file = file;
    }

    public static AuditLogger toFile(Path file) {
        return new AuditLogger(file);
    }

    public static AuditLogger disabled() {
        return new AuditLogger(null);
    }

    public synchronized void log(String event, String username, String detail) {
        if (file == null) {
            return;
        }
        String line = Instant.now().truncatedTo(ChronoUnit.SECONDS) + "|" + clean(event, 20)
                + "|" + clean(username, 30) + "|" + clean(detail, 100);
        try {
            FileStorageUtil.appendLine(file, line);
        } catch (RuntimeException e) {
            System.err.println("[WARN] Could not write to the audit log.");
        }
    }

    //Keeps letters, digits and a few harmless symbols everything else (including | and line breaks) becomes '?'
    static String clean(String text, int maxLength) {
        if (text == null) {
            return "-";
        }
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < text.length() && i < maxLength; i++) {
            char c = text.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || " _.,:$*@()-".indexOf(c) >= 0;
            safe.append(ok ? c : '?');
        }
        return safe.toString();
    }

    //{@code 1234567890} becomes {@code ******7890}
    public static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }
}
