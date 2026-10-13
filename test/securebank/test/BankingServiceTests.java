package securebank.test;

import securebank.exception.AccountNotFoundException;
import securebank.exception.InsufficientFundsException;
import securebank.exception.PersistenceException;
import securebank.exception.ValidationException;
import securebank.model.Account;
import securebank.model.AccountType;
import securebank.model.Transaction;
import securebank.model.TransactionType;
import securebank.repository.FileAccountRepository;
import securebank.repository.FileTransactionRepository;
import securebank.repository.TransactionRepository;
import securebank.service.BankingService;
import securebank.util.AuditLogger;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static securebank.test.Assert.*;
import static securebank.test.TestRunner.suite;
import static securebank.test.TestRunner.test;
import static securebank.test.TestSupport.*;

final class BankingServiceTests {

    private BankingServiceTests() {
    }

    private static BankingService service(Path dir, TransactionRepository ledger) {
        return new BankingService(new FileAccountRepository(dir.resolve("accounts.txt")), ledger,
                AuditLogger.disabled(), new MutableClock());
    }

    private static BankingService service(Path dir) {
        return service(dir, new FileTransactionRepository(dir.resolve("transactions.txt")));
    }

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    // A ledger that always fails, to prove balances are rolled back
    private static final class BrokenLedger implements TransactionRepository {
        @Override public void save(Transaction t) { throw new PersistenceException("simulated disk failure"); }
        @Override public List<Transaction> findByAccountNumber(String n) { return java.util.Collections.emptyList(); }
        @Override public int count() { return 0; }
    }

    static void run() {
        suite("BankingService (accounts, deposits, withdrawals, ownership)");

        test("new account: zero balance, 10-digit number, correct owner", () -> {
            Path dir = tempDir();
            try {
                Account a = service(dir).createAccount("alice", AccountType.SAVINGS);
                assertMoney("0", a.getBalance(), "balance");
                assertTrue(a.getAccountNumber().matches("[1-9][0-9]{9}"), "number " + a.getAccountNumber());
                assertEquals("alice", a.getOwnerUsername(), "owner");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("a user can hold only 5 accounts", () -> {
            Path dir = tempDir();
            try {
                BankingService s = service(dir);
                for (int i = 0; i < BankingService.MAX_ACCOUNTS_PER_USER; i++) {
                    s.createAccount("alice", AccountType.CURRENT);
                }
                assertThrows(ValidationException.class, () -> s.createAccount("alice", AccountType.CURRENT), "limit");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("deposit and withdraw update the balance and record transactions", () -> {
            Path dir = tempDir();
            try {
                BankingService s = service(dir);
                String no = s.createAccount("alice", AccountType.SAVINGS).getAccountNumber();
                s.deposit("alice", no, d("100.00"));
                Transaction w = s.withdraw("alice", no, d("30.25"));
                assertMoney("69.75", w.getResultingBalance(), "balance after");
                List<Transaction> history = s.getTransactionHistory("alice", no);
                assertEquals(2, history.size(), "two transactions");
                assertEquals(TransactionType.WITHDRAWAL, history.get(0).getType(), "newest first");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("savings cannot overdraw; a refused withdrawal records nothing", () -> {
            Path dir = tempDir();
            try {
                BankingService s = service(dir);
                String no = s.createAccount("alice", AccountType.SAVINGS).getAccountNumber();
                s.deposit("alice", no, d("50.00"));
                assertThrows(InsufficientFundsException.class, () -> s.withdraw("alice", no, d("50.01")), "overdraw");
                assertMoney("50.00", s.getOwnedAccount("alice", no).getBalance(), "unchanged");
                assertEquals(1, s.getTransactionHistory("alice", no).size(), "only the deposit");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("current account may use its $500 overdraft", () -> {
            Path dir = tempDir();
            try {
                BankingService s = service(dir);
                String no = s.createAccount("alice", AccountType.CURRENT).getAccountNumber();
                s.withdraw("alice", no, d("500.00"));
                assertMoney("-500.00", s.getOwnedAccount("alice", no).getBalance(), "at the limit");
                assertThrows(InsufficientFundsException.class, () -> s.withdraw("alice", no, d("0.01")), "beyond");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("the service re-validates amounts even if the caller did not", () -> {
            Path dir = tempDir();
            try {
                BankingService s = service(dir);
                String no = s.createAccount("alice", AccountType.SAVINGS).getAccountNumber();
                for (BigDecimal bad : new BigDecimal[]{null, BigDecimal.ZERO, d("-5"), d("1.005"), d("2000000000")}) {
                    assertThrows(ValidationException.class, () -> s.deposit("alice", no, bad), "deposit " + bad);
                    assertThrows(ValidationException.class, () -> s.withdraw("alice", no, bad), "withdraw " + bad);
                }
            } finally {
                deleteRecursively(dir);
            }
        });

        test("another user's account is refused, with the same message as a missing one", () -> {
            Path dir = tempDir();
            try {
                BankingService s = service(dir);
                String bobs = s.createAccount("bob", AccountType.SAVINGS).getAccountNumber();
                s.deposit("bob", bobs, d("100.00"));
                String notYours = assertThrows(AccountNotFoundException.class, () -> s.getOwnedAccount("alice", bobs), "balance").getMessage();
                String missing = assertThrows(AccountNotFoundException.class, () -> s.getOwnedAccount("alice", "9999999999"), "missing").getMessage();
                assertEquals(missing, notYours, "same message, so account numbers cannot be probed");
                assertThrows(AccountNotFoundException.class, () -> s.deposit("alice", bobs, d("1.00")), "deposit");
                assertThrows(AccountNotFoundException.class, () -> s.withdraw("alice", bobs, d("1.00")), "withdraw");
                assertThrows(AccountNotFoundException.class, () -> s.getTransactionHistory("alice", bobs), "history");
                assertMoney("100.00", s.getOwnedAccount("bob", bobs).getBalance(), "bob's money untouched");
                assertEquals(0, s.getAccounts("alice").size(), "alice sees no accounts");
            } finally {
                deleteRecursively(dir);
            }
        });

        test("if the transaction cannot be saved, the balance is rolled back", () -> {
            Path dir = tempDir();
            try {
                BankingService s = service(dir, new BrokenLedger());
                String no = s.createAccount("alice", AccountType.SAVINGS).getAccountNumber();
                assertThrows(PersistenceException.class, () -> s.deposit("alice", no, d("50.00")), "deposit");
                assertMoney("0", s.getOwnedAccount("alice", no).getBalance(), "balance restored");
                assertMoney("0", new FileAccountRepository(dir.resolve("accounts.txt"))
                        .findByAccountNumber(no).get().getBalance(), "file restored too");
            } finally {
                deleteRecursively(dir);
            }
        });
    }
}
