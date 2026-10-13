package securebank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

// An immutable record of one deposit or withdrawal
public final class Transaction {

    private final String transactionId;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final BigDecimal resultingBalance;

    public Transaction(String transactionId, String accountNumber, TransactionType type,
                       BigDecimal amount, LocalDateTime timestamp, BigDecimal resultingBalance) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.type = Objects.requireNonNull(type);
        this.amount = Objects.requireNonNull(amount);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.resultingBalance = Objects.requireNonNull(resultingBalance);
    }

    public String getTransactionId() { return transactionId; }

    public String getAccountNumber() { return accountNumber; }

    public TransactionType getType() { return type; }

    public BigDecimal getAmount() { return amount; }

    public LocalDateTime getTimestamp() { return timestamp; }

    //Account balance right after transaction
    public BigDecimal getResultingBalance() { return resultingBalance; }
}
