package securebank.service;

import securebank.exception.AccountNotFoundException;
import securebank.exception.InsufficientFundsException;
import securebank.exception.ValidationException;
import securebank.model.Account;
import securebank.model.AccountType;
import securebank.model.Transaction;
import securebank.model.TransactionType;
import securebank.repository.AccountRepository;
import securebank.repository.TransactionRepository;
import securebank.util.AuditLogger;
import securebank.util.InputValidator;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Banking rules: open accounts, deposit, withdraw, history
 *
 *Least privilege: every method takes the logged-in {@code username} and only touches that user's
 * accounts. Someone else's account gives the same "not found" answer as a missing one (so account
 * numbers cannot be probed), and the attempt is logged. Amounts are also re-validated
 */
public class BankingService {

    public static final int MAX_ACCOUNTS_PER_USER = 5;
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final AuditLogger audit;
    private final Clock clock;

    public BankingService(AccountRepository accounts, TransactionRepository transactions,
                          AuditLogger audit, Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.audit = audit;
        this.clock = clock;
    }

    public synchronized Account createAccount(String username, AccountType type) throws ValidationException {
        if (accounts.findByOwner(username).size() >= MAX_ACCOUNTS_PER_USER) {
            throw new ValidationException("You can have at most " + MAX_ACCOUNTS_PER_USER + " accounts.");
        }
        Account account = type.newAccount(newAccountNumber(), username, ZERO, LocalDateTime.now(clock));
        accounts.save(account);
        audit.log("ACCOUNT_CREATED", username, type + " " + AuditLogger.mask(account.getAccountNumber()));
        return account;
    }

    public synchronized List<Account> getAccounts(String username) {
        return accounts.findByOwner(username);
    }

    /** @throws AccountNotFoundException if the account does not exist or is not owned by {@code username} */
    public synchronized Account getOwnedAccount(String username, String accountNumber)
            throws AccountNotFoundException {
        Optional<Account> account = accounts.findByAccountNumber(accountNumber);
        if (!account.isPresent() || !account.get().isOwnedBy(username)) {
            if (account.isPresent()) {
                audit.log("ACCESS_DENIED", username, "Tried account " + AuditLogger.mask(accountNumber));
            }
            throw new AccountNotFoundException("No account with that number was found on your profile.");
        }
        return account.get();
    }

    public synchronized Transaction deposit(String username, String accountNumber, BigDecimal amount)
            throws AccountNotFoundException, ValidationException {
        requireValidAmount(amount);
        Account account = getOwnedAccount(username, accountNumber);
        Account before = account.copy();
        account.credit(amount);
        return commit(before, account, TransactionType.DEPOSIT, amount, username);
    }

    public synchronized Transaction withdraw(String username, String accountNumber, BigDecimal amount)
            throws AccountNotFoundException, ValidationException, InsufficientFundsException {
        requireValidAmount(amount);
        Account account = getOwnedAccount(username, accountNumber);
        Account before = account.copy();
        try {
            account.debit(amount);
        } catch (InsufficientFundsException e) {
            audit.log("WITHDRAWAL_DENIED", username, "Insufficient funds " + AuditLogger.mask(accountNumber));
            throw e;
        }
        return commit(before, account, TransactionType.WITHDRAWAL, amount, username);
    }

    //Newest  account history first
    public synchronized List<Transaction> getTransactionHistory(String username, String accountNumber)
            throws AccountNotFoundException {
        getOwnedAccount(username, accountNumber);
        List<Transaction> history = new ArrayList<>(transactions.findByAccountNumber(accountNumber));
        Collections.reverse(history);
        return history;
    }

    private static void requireValidAmount(BigDecimal amount) throws ValidationException {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.compareTo(InputValidator.MAX_AMOUNT) > 0) {
            throw new ValidationException("Amount must be above zero, with at most 2 decimals.");
        }
    }

    //Saves the new balance then the transaction. If the second step fails the balance is restored
    private Transaction commit(Account before, Account after, TransactionType type, BigDecimal amount,
                               String username) {
        accounts.update(after);
        try {
            Transaction t = new Transaction(UUID.randomUUID().toString(), after.getAccountNumber(), type,
                    amount, LocalDateTime.now(clock), after.getBalance());
            transactions.save(t);
            audit.log(type.name(), username, amount.toPlainString() + " on " + AuditLogger.mask(after.getAccountNumber()));
            return t;
        } catch (RuntimeException failure) {
            try {
                accounts.update(before);
            } catch (RuntimeException rollbackFailure) {
                audit.log("ROLLBACK_FAILED", username, "Check " + AuditLogger.mask(after.getAccountNumber()));
            }
            throw failure;
        }
    }

    private String newAccountNumber() {
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder().append(1 + RANDOM.nextInt(9)); // never starts with 0
            for (int d = 1; d < 10; d++) {
                sb.append(RANDOM.nextInt(10));
            }
            if (!accounts.findByAccountNumber(sb.toString()).isPresent()) {
                return sb.toString();
            }
        }
        throw new IllegalStateException("Could not generate a unique account number.");
    }
}
