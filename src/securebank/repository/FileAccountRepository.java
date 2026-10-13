package securebank.repository;

import securebank.model.Account;
import securebank.model.AccountType;
import securebank.util.FileStorageUtil;
import securebank.util.InputValidator;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Accounts in {@code accounts.txt}: {@code accountNumber|type|owner|balance|createdAt}
 * Loaded once at start-up every change is written to the file first, then to memory.
 */
public final class FileAccountRepository implements AccountRepository {

    private final Path file;
    private Map<String, Account> accounts = new LinkedHashMap<>(); // key: account number

    public FileAccountRepository(Path file) {
        this.file = file;
        int skipped = 0;
        for (String line : FileStorageUtil.readAllLines(file)) {
            try {
                List<String> f = RecordCodec.decode(line);
                if (f.size() != 5 || !InputValidator.isValidAccountNumber(f.get(0))
                        || !InputValidator.isValidUsername(f.get(2)) || accounts.containsKey(f.get(0))) {
                    throw new IllegalArgumentException();
                }
                accounts.put(f.get(0), AccountType.valueOf(f.get(1)).newAccount(f.get(0), f.get(2),
                        new BigDecimal(f.get(3)), LocalDateTime.parse(f.get(4))));
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        if (skipped > 0) {
            System.err.println("[WARN] accounts.txt: " + skipped + " unreadable record(s) skipped.");
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        Account account = accounts.get(accountNumber);
        return account == null ? Optional.empty() : Optional.of(account.copy());
    }

    @Override
    public List<Account> findByOwner(String username) {
        List<Account> owned = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a.isOwnedBy(username)) {
                owned.add(a.copy());
            }
        }
        return owned;
    }

    @Override
    public void save(Account account) {
        if (accounts.containsKey(account.getAccountNumber())) {
            throw new IllegalStateException("Account number already exists.");
        }
        store(account);
    }

    @Override
    public void update(Account account) {
        if (!accounts.containsKey(account.getAccountNumber())) {
            throw new IllegalStateException("Account does not exist.");
        }
        store(account);
    }

    @Override
    public int count() {
        return accounts.size();
    }

    private void store(Account account) {
        Map<String, Account> next = new LinkedHashMap<>(accounts);
        next.put(account.getAccountNumber(), account.copy());
        List<String> lines = new ArrayList<>();
        for (Account a : next.values()) {
            lines.add(RecordCodec.encode(Arrays.asList(a.getAccountNumber(), a.getType().name(),
                    a.getOwnerUsername(), a.getBalance().toPlainString(), a.getCreatedAt().toString())));
        }
        FileStorageUtil.writeAllLinesAtomically(file, lines); // write file first ...
        accounts = next;                                      // ... then memory
    }
}
