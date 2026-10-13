package securebank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// account types, and matching Account subclass
public enum AccountType {
    SAVINGS("Savings"),
    CURRENT("Current");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Account newAccount(String accountNumber, String ownerUsername,
                              BigDecimal balance, LocalDateTime createdAt) {
        switch (this) {
            case SAVINGS:
                return new SavingsAccount(accountNumber, ownerUsername, balance, createdAt);
            case CURRENT:
                return new CurrentAccount(accountNumber, ownerUsername, balance, createdAt);
            default:
                throw new IllegalStateException("Unknown account type: " + this);
        }
    }
}
