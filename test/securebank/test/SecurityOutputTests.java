package securebank.test;

import securebank.util.AuditLogger;
import securebank.util.ConsoleUtil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static securebank.test.Assert.*;
import static securebank.test.TestRunner.suite;
import static securebank.test.TestRunner.test;
import static securebank.test.TestSupport.deleteRecursively;
import static securebank.test.TestSupport.tempDir;

final class SecurityOutputTests {

    private SecurityOutputTests() {
    }

    private static String printed(String text) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ConsoleUtil(new Scanner(""), new PrintStream(bytes, true, "UTF-8"), null).printLine(text);
        return bytes.toString("UTF-8").trim();
    }

    static void run() {
        suite("Output encoding and audit log");

        test("normal text is printed unchanged", () ->
                assertEquals("Balance: $1,000.00 caf\u00e9", printed("Balance: $1,000.00 caf\u00e9"), "plain text"));

        test("terminal escape codes and direction overrides are neutralised", () -> {
            String out = printed("\u001b[2J\u001b[31mred\u202Eevil");
            assertFalse(out.indexOf('\u001b') >= 0, "no ESC character");
            assertFalse(out.indexOf('\u202E') >= 0, "no bidi override");
        });

        test("over-long input lines are cut off", () -> {
            ConsoleUtil c = new ConsoleUtil(new Scanner(new String(new char[5000]).replace('\0', 'a') + "\n"),
                    new PrintStream(new ByteArrayOutputStream()), null);
            assertEquals(ConsoleUtil.MAX_INPUT_LENGTH, c.readLine("> ").length(), "capped");
        });

        test("audit entry has timestamp, event, user and detail", () -> {
            Path dir = tempDir();
            try {
                Path file = dir.resolve("audit.log");
                AuditLogger.toFile(file).log("LOGIN_OK", "alice", "Login successful");
                String[] parts = Files.readAllLines(file).get(0).split("\\|");
                assertEquals(4, parts.length, "four fields");
                assertEquals("LOGIN_OK", parts[1], "event");
                assertEquals("alice", parts[2], "user");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("log forging: typed line breaks and pipes cannot create fake entries", () -> {
            Path dir = tempDir();
            try {
                Path file = dir.resolve("audit.log");
                String attack = "mallory\n2026-01-01T00:00:00Z|LOGIN_OK|admin|Login successful";
                AuditLogger.toFile(file).log("LOGIN_FAILED", attack, attack);
                List<String> lines = Files.readAllLines(file);
                assertEquals(1, lines.size(), "one line");
                assertEquals(4, lines.get(0).split("\\|", -1).length, "four fields");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("account numbers are masked in logs", () -> {
            assertEquals("******7890", AuditLogger.mask("1234567890"), "masked");
            assertEquals("****", AuditLogger.mask(null), "null");
        });

        test("a logging failure never stops the caller", () -> {
            Path dir = tempDir();
            try {
                AuditLogger.toFile(dir).log("X", "alice", "y"); // a folder cannot be appended to
            } finally {
                deleteRecursively(dir);
            }
        });
    }
}
