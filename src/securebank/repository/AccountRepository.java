package securebank.repository;

import securebank.model.Account;

import java.util.List;
import java.util.Optional;

//Storage for accounts returned accounts are copies  changes only count after update()
public interface AccountRepository {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwner(String username);

    // Saves a new account
    void save(Account account);

    // Saves changes (such as a new balance) to an existing account
    void update(Account account);

    int count();
}
