package securebank.test;

import securebank.model.AccountType;
import securebank.model.User;
import securebank.repository.FileAccountRepository;
import securebank.repository.FileTransactionRepository;
import securebank.repository.FileUserRepository;
import securebank.service.BankingService;
import securebank.util.AuditLogger;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;

import static securebank.test.Assert.*;
import static securebank.test.TestRunner.suite;
import static securebank.test.TestRunner.test;
import static securebank.test.TestSupport.*;

final class PersistenceTests {

    private PersistenceTests() {
    }

    private static BankingService open(Path dir) {
        return new BankingService(new FileAccountRepository(dir.resolve("accounts.txt")),
                new FileTransactionRepository(dir.resolve("transactions.txt")), AuditLogger.disabled(), Clock.systemUTC());
    }

    static void run() {
        suite("Data persistence (text files, loaded on start-up)");

        test("missing folder and files are created automatically and start empty", () -> {
            Path dir = tempDir();
            try {
                Path nested = dir.resolve("a").resolve("data");
                assertEquals(0, new FileUserRepository(nested.resolve("users.txt")).count(), "empty");
                assertTrue(Files.exists(nested.resolve("users.txt")), "file created");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("users, accounts and transactions all survive a restart", () -> {
            Path dir = tempDir();
            try {
                new FileUserRepository(dir.resolve("users.txt")).save(new User("alice", "HASH", LocalDateTime.now()));
                BankingService first = open(dir);
                String no = first.createAccount("alice", AccountType.CURRENT).getAccountNumber();
                first.deposit("alice", no, new BigDecimal("100.50"));
                first.withdraw("alice", no, new BigDecimal("20.25"));

                // "restart": build everything again from the files
                FileUserRepository users = new FileUserRepository(dir.resolve("users.txt"));
                BankingService second = open(dir);
                assertEquals(1, users.count(), "user reloaded");
                assertEquals("alice", users.findByUsername("ALICE").get().getUsername(), "found ignoring case");
                assertMoney("80.25", second.getOwnedAccount("alice", no).getBalance(), "balance reloaded");
                assertEquals(AccountType.CURRENT, second.getOwnedAccount("alice", no).getType(), "type reloaded");
                assertEquals(2, second.getTransactionHistory("alice", no).size(), "history reloaded");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("the transaction file is append-only: earlier lines are never changed", () -> {
            Path dir = tempDir();
            try {
                BankingService s = open(dir);
                String no = s.createAccount("alice", AccountType.SAVINGS).getAccountNumber();
                s.deposit("alice", no, new BigDecimal("10.00"));
                String firstLine = Files.readAllLines(dir.resolve("transactions.txt")).get(0);
                s.deposit("alice", no, new BigDecimal("5.00"));
                assertEquals(firstLine, Files.readAllLines(dir.resolve("transactions.txt")).get(0), "first line unchanged");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("record injection: '|' and line breaks in a value stay inside one field", () -> {
            Path dir = tempDir();
            try {
                Path file = dir.resolve("users.txt");
                String nasty = "evil|name\nfake|HASH|2026-01-01T00:00:00";
                new FileUserRepository(file).save(new User(nasty, "H", LocalDateTime.now()));
                assertEquals(1, Files.readAllLines(file).size(), "still one line in the file");
                FileUserRepository reloaded = new FileUserRepository(file);
                assertEquals(1, reloaded.count(), "still one user");
                assertEquals(nasty, reloaded.findByUsername(nasty).get().getUsername(), "stored exactly");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("damaged or duplicate lines are skipped; good ones still load", () -> {
            Path dir = tempDir();
            try {
                Path file = dir.resolve("accounts.txt");
                Files.write(file, ("1111111111|SAVINGS|alice|10.00|2026-01-01T10:00:00\n"
                        + "not a record\n"
                        + "2222222222|SAVINGS|alice|abc|2026-01-01T10:00:00\n"
                        + "3333333333|BOGUS|alice|1.00|2026-01-01T10:00:00\n"
                        + "4444444444|SAVINGS|alice|-5.00|2026-01-01T10:00:00\n"
                        + "short|SAVINGS|alice|1.00|2026-01-01T10:00:00\n"
                        + "1111111111|SAVINGS|mallory|99.00|2026-01-01T10:00:00\n"
                        + "6666666666|CURRENT|alice|-9.00|2026-01-01T10:00:00\n").getBytes(StandardCharsets.UTF_8));
                FileAccountRepository repo = new FileAccountRepository(file);
                assertEquals(2, repo.count(), "only the two good records load");
                assertEquals("alice", repo.findByAccountNumber("1111111111").get().getOwnerUsername(), "first duplicate wins");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("the repository hands out copies, so edits only count after update()", () -> {
            Path dir = tempDir();
            try {
                FileAccountRepository repo = new FileAccountRepository(dir.resolve("accounts.txt"));
                repo.save(AccountType.SAVINGS.newAccount("1111111111", "alice", BigDecimal.ZERO, LocalDateTime.now()));
                securebank.model.Account copy = repo.findByAccountNumber("1111111111").get();
                copy.credit(new BigDecimal("40.00"));
                assertMoney("0", repo.findByAccountNumber("1111111111").get().getBalance(), "not stored yet");
                repo.update(copy);
                assertMoney("40.00", new FileAccountRepository(dir.resolve("accounts.txt"))
                        .findByAccountNumber("1111111111").get().getBalance(), "stored after update");
            } finally {
                deleteRecursively(dir);
            }
        });
    }
}
