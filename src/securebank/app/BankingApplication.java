package securebank.app;

import securebank.exception.AccountNotFoundException;
import securebank.exception.AuthenticationException;
import securebank.exception.InputClosedException;
import securebank.exception.InsufficientFundsException;
import securebank.exception.PersistenceException;
import securebank.exception.ValidationException;
import securebank.model.Account;
import securebank.model.AccountType;
import securebank.model.Transaction;
import securebank.model.User;
import securebank.service.AuthenticationService;
import securebank.service.BankingService;
import securebank.util.AuditLogger;
import securebank.util.ConsoleUtil;
import securebank.util.InputValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 *  CONSOLE MENUS
 *  It Reads and validates input, then calls the service It holds no business rules
 * Safe error handling: expected problems show their own message; anything unexpected shows a generic
 * message and only the exception type goes to the audit log
 */
public class BankingApplication {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int HISTORY_LIMIT = 50;

    private final ConsoleUtil console;
    private final AuthenticationService auth;
    private final BankingService banking;
    private final AuditLogger audit;

    public BankingApplication(ConsoleUtil console, AuthenticationService auth, BankingService banking,
                              AuditLogger audit) {
        this.console = console;
        this.auth = auth;
        this.banking = banking;
        this.audit = audit;
    }
//Runs until the user exits or input ends

    public void run() {
        if (!console.isPasswordHidden()) {
            console.printLine("NOTE: no secure terminal detected, so typed passwords will be VISIBLE.");
        }
        audit.log("STARTUP", "system", "Application started");
        boolean running = true;
        while (running) {
            try {
                running = welcomeMenu();
            } catch (InputClosedException e) {
                running = false;
            } catch (RuntimeException e) {
                reportFailure(e);
            }
        }
        audit.log("SHUTDOWN", "system", "Application stopped");
        console.printLine("Goodbye.");
    }

    private boolean welcomeMenu() {
        console.printLine("");
        console.printLine("--- MAIN MENU ---");
        console.printLine("1. Register");
        console.printLine("2. Login");
        console.printLine("3. Exit");
        switch (InputValidator.parseMenuChoice(console.readLine("Choose an option: "))) {
            case 1: register(); return true;
            case 2: login(); return true;
            case 3: return false;
            default:
                console.printLine("Invalid option. Please choose 1, 2 or 3.");
                return true;
        }
    }

    private void register() {
        console.printLine("");
        console.printLine("Username: 3-20 letters, digits or underscores.");
        console.printLine("Password: 10+ characters with upper, lower, digit and symbol.");
        String username = console.readLine("Choose a username: ");
        char[] password = console.readPassword("Choose a password: ");
        char[] confirm = console.readPassword("Confirm password: ");
        try {
            if (!Arrays.equals(password, confirm)) {
                console.printLine("Passwords do not match.");
                return;
            }
            auth.register(username, password);
            console.printLine("Registration successful. You can now log in.");
        } catch (ValidationException e) {
            console.printLine("Registration failed: " + e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    private void login() {
        console.printLine("");
        String username = console.readLine("Username: ");
        char[] password = console.readPassword("Password: ");
        try {
            User user = auth.login(username, password);
            console.printLine("Login successful. Welcome, " + user.getUsername() + "!");
            bankingMenu(user.getUsername());
        } catch (AuthenticationException e) {
            console.printLine(e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void bankingMenu(String user) {
        boolean loggedIn = true;
        while (loggedIn) {
            console.printLine("");
            console.printLine("--- BANKING MENU ---");
            console.printLine("1. Create Bank Account");
            console.printLine("2. View My Accounts");
            console.printLine("3. View Account Balance");
            console.printLine("4. Deposit");
            console.printLine("5. Withdraw");
            console.printLine("6. Transaction History");
            console.printLine("7. Logout");
            int choice = InputValidator.parseMenuChoice(console.readLine("Choose an option: "));
            try {
                switch (choice) {
                    case 1: createAccount(user); break;
                    case 2: viewAccounts(user); break;
                    case 3: viewBalance(user); break;
                    case 4: deposit(user); break;
                    case 5: withdraw(user); break;
                    case 6: viewHistory(user); break;
                    case 7:
                        audit.log("LOGOUT", user, "User logged out");
                        console.printLine("Logged out. Goodbye, " + user + ".");
                        loggedIn = false;
                        break;
                    default:
                        console.printLine("Invalid option. Please choose 1-7.");
                }
            } catch (InputClosedException e) {
                throw e;
            } catch (ValidationException | AccountNotFoundException | InsufficientFundsException e) {
                console.printLine(e.getMessage());
            } catch (RuntimeException e) {
                reportFailure(e);
            }
        }
    }

    private void createAccount(String user) throws ValidationException {
        AccountType[] types = AccountType.values();
        for (int i = 0; i < types.length; i++) {
            console.printLine((i + 1) + ". " + types[i].getDisplayName());
        }
        int choice = InputValidator.parseMenuChoice(console.readLine("Account type: "));
        if (choice < 1 || choice > types.length) {
            console.printLine("Invalid account type. No account was created.");
            return;
        }
        Account account = banking.createAccount(user, types[choice - 1]);
        console.printLine("Account created. Number: " + account.getAccountNumber()
                + " (" + account.getType().getDisplayName() + "), balance " + money(account.getBalance()));
    }

    private void viewAccounts(String user) {
        List<Account> accounts = banking.getAccounts(user);
        if (accounts.isEmpty()) {
            console.printLine("You have no accounts yet. Choose option 1 to create one.");
            return;
        }
        for (Account a : accounts) {
            console.printLine(a.getAccountNumber() + "  " + a.getType().getDisplayName()
                    + "  balance " + money(a.getBalance()));
        }
    }

    private void viewBalance(String user) throws ValidationException, AccountNotFoundException {
        String number = InputValidator.validateAccountNumber(console.readLine("Account number: "));
        Account account = banking.getOwnedAccount(user, number);
        console.printLine("Balance of " + number + ": " + money(account.getBalance()));
    }

    private void deposit(String user) throws ValidationException, AccountNotFoundException {
        String number = InputValidator.validateAccountNumber(console.readLine("Account number: "));
        BigDecimal amount = InputValidator.validateAmount(console.readLine("Deposit amount: "));
        Transaction t = banking.deposit(user, number, amount);
        console.printLine("Deposit successful. New balance: " + money(t.getResultingBalance()));
    }

    private void withdraw(String user)
            throws ValidationException, AccountNotFoundException, InsufficientFundsException {
        String number = InputValidator.validateAccountNumber(console.readLine("Account number: "));
        BigDecimal amount = InputValidator.validateAmount(console.readLine("Withdrawal amount: "));
        Transaction t = banking.withdraw(user, number, amount);
        console.printLine("Withdrawal successful. New balance: " + money(t.getResultingBalance()));
    }

    private void viewHistory(String user) throws ValidationException, AccountNotFoundException {
        String number = InputValidator.validateAccountNumber(console.readLine("Account number: "));
        List<Transaction> history = banking.getTransactionHistory(user, number);
        if (history.isEmpty()) {
            console.printLine("No transactions yet.");
            return;
        }
        int shown = Math.min(history.size(), HISTORY_LIMIT);
        console.printLine("Newest first (" + shown + " of " + history.size() + "):");
        for (Transaction t : history.subList(0, shown)) {
            console.printLine(t.getTimestamp().format(TIME) + "  " + t.getType() + "  "
                    + money(t.getAmount()) + "  balance after " + money(t.getResultingBalance()));
        }
    }
//
//    Generic message for the user only the exception type is logged

    private void reportFailure(RuntimeException problem) {
        audit.log("ERROR", "system", problem.getClass().getSimpleName());
        if (problem instanceof PersistenceException) {
            console.printLine("A data storage problem occurred. Your last action may not have been saved.");
        } else {
            console.printLine("An unexpected error occurred. Please try again.");
        }
    }

    private static String money(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        return (rounded.signum() < 0 ? "-$" : "$") + rounded.abs().toPlainString();
    }
}
