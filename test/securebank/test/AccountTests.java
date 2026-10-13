package securebank.test;

import securebank.exception.InsufficientFundsException;
import securebank.model.Account;
import securebank.model.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static securebank.test.Assert.*;
import static securebank.test.TestRunner.suite;
import static securebank.test.TestRunner.test;

final class AccountTests {

    private AccountTests() {
    }

    private static Account make(AccountType type, String balance) {
        return type.newAccount("1234567890", "alice", new BigDecimal(balance), LocalDateTime.now());
    }

    static void run() {
        suite("Account classes (OOP: inheritance, polymorphism, encapsulation)");

        test("credit and debit change the balance", () -> {
            Account a = make(AccountType.SAVINGS, "10.00");
            a.credit(new BigDecimal("5.50"));
            a.debit(new BigDecimal("0.50"));
            assertMoney("15.00", a.getBalance(), "balance");
        });

        test("zero, negative and null amounts are rejected", () -> {
            Account a = make(AccountType.SAVINGS, "10.00");
            assertThrows(IllegalArgumentException.class, () -> a.credit(BigDecimal.ZERO), "zero");
            assertThrows(IllegalArgumentException.class, () -> a.credit(new BigDecimal("-1")), "negative");
            assertThrows(IllegalArgumentException.class, () -> a.debit(null), "null");
        });

        test("savings can never go below zero", () -> {
            Account a = make(AccountType.SAVINGS, "10.00");
            assertThrows(InsufficientFundsException.class, () -> a.debit(new BigDecimal("10.01")), "overdraw");
            a.debit(new BigDecimal("10.00"));
            assertMoney("0", a.getBalance(), "exactly zero is fine");
        });

        test("current account may overdraw up to $500, not beyond", () -> {
            Account a = make(AccountType.CURRENT, "0.00");
            a.debit(new BigDecimal("500.00"));
            assertMoney("-500.00", a.getBalance(), "at the limit");
            assertThrows(InsufficientFundsException.class, () -> a.debit(new BigDecimal("0.01")), "past the limit");
        });

        test("each subclass reports its own type (polymorphism)", () -> {
            assertEquals(AccountType.SAVINGS, make(AccountType.SAVINGS, "0").getType(), "savings");
            assertEquals(AccountType.CURRENT, make(AccountType.CURRENT, "0").getType(), "current");
        });

        test("a copy is independent of the original", () -> {
            Account original = make(AccountType.SAVINGS, "10.00");
            Account copy = original.copy();
            copy.credit(new BigDecimal("5.00"));
            assertMoney("10.00", original.getBalance(), "original unchanged");
        });

        test("ownership check ignores case", () -> {
            assertTrue(make(AccountType.SAVINGS, "0").isOwnedBy("ALICE"), "owner");
            assertFalse(make(AccountType.SAVINGS, "0").isOwnedBy("bob"), "other user");
        });
    }
}
