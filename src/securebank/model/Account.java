package securebank.model;

import securebank.exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base class for bank accounts
 * The balance is private (encapsulation) and changes only through credit() and debit() enforcing the money rules
 * Subclasses set the overdraft limit.
 */
public abstract class Account {

    private final String accountNumber;
    private final String ownerUsername;
    private final LocalDateTime createdAt;
    private final BigDecimal overdraftLimit;
    private BigDecimal balance;

    protected Account(String accountNumber, String ownerUsername, BigDecimal balance,
                      LocalDateTime createdAt, BigDecimal overdraftLimit) {
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.ownerUsername = Objects.requireNonNull(ownerUsername);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.overdraftLimit = Objects.requireNonNull(overdraftLimit);
        if (overdraftLimit.signum() < 0) {
            throw new IllegalArgumentException("Overdraft limit must not be negative.");
        }
        if (balance == null || balance.compareTo(overdraftLimit.negate()) < 0) {
            throw new IllegalArgumentException("Balance is below the minimum allowed for this account.");
        }
        this.balance = balance;
    }

    public abstract AccountType getType();

    //Independent copy thus changing it never alters stored data until update() is called /
    public final Account copy() {
        return getType().newAccount(accountNumber, ownerUsername, balance, createdAt);
    }

    public String getAccountNumber() { return accountNumber; }

    public String getOwnerUsername() { return ownerUsername; }

    public BigDecimal getBalance() { return balance; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public BigDecimal getOverdraftLimit() { return overdraftLimit; }

    // Balance plus any unused overdraft. /
    public BigDecimal getAvailableFunds() {
        return balance.add(overdraftLimit);
    }

    //Adds money. @throws IllegalArgumentException if the amount is null, zero or negative /
    public void credit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    // Removes money @throws InsufficientFundsException if it exceeds the available funds /
    public void debit(BigDecimal amount) throws InsufficientFundsException {
        requirePositive(amount);
        if (amount.compareTo(getAvailableFunds()) > 0) {
            throw new InsufficientFundsException("Insufficient funds: you can withdraw at most $"
                    + getAvailableFunds().setScale(2, RoundingMode.HALF_UP).toPlainString() + ".");
        }
        balance = balance.subtract(amount);
    }

    public boolean isOwnedBy(String username) {
        return ownerUsername.equalsIgnoreCase(username);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }
}
