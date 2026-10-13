package securebank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//current account: the balance may  go below zero, up to a fixed overdraft limit
public final class CurrentAccount extends Account {

    // How far below 0 a current account may go
    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("500.00");

    public CurrentAccount(String accountNumber, String ownerUsername,
                          BigDecimal balance, LocalDateTime createdAt) {
        super(accountNumber, ownerUsername, balance, createdAt, OVERDRAFT_LIMIT);
    }

    @Override
    public AccountType getType() {
        return AccountType.CURRENT;
    }
}
