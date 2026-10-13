package securebank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// A savings account: the balance can never go below zero
public final class SavingsAccount extends Account {

    public SavingsAccount(String accountNumber, String ownerUsername,
                          BigDecimal balance, LocalDateTime createdAt) {
        super(accountNumber, ownerUsername, balance, createdAt, BigDecimal.ZERO);
    }

    @Override
    public AccountType getType() {
        return AccountType.SAVINGS;
    }
}
