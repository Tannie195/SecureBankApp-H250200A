package securebank.repository;

import securebank.model.Transaction;

import java.util.List;

// Storage for transactions ,Append-only: there is no update or delete
public interface TransactionRepository {

    void save(Transaction transaction);

    // All transactions of the account, oldest first
    List<Transaction> findByAccountNumber(String accountNumber);

    int count();
}
