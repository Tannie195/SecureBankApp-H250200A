package securebank;

import securebank.app.BankingApplication;
import securebank.exception.PersistenceException;
import securebank.repository.AccountRepository;
import securebank.repository.FileAccountRepository;
import securebank.repository.FileTransactionRepository;
import securebank.repository.FileUserRepository;
import securebank.repository.TransactionRepository;
import securebank.repository.UserRepository;
import securebank.security.LoginAttemptTracker;
import securebank.security.PasswordHasher;
import securebank.service.AuthenticationService;
import securebank.service.BankingService;
import securebank.util.AuditLogger;
import securebank.util.ConsoleUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;

/**
 * Entry point. Loads the data files from the data folder, connects the classes together
 * (constructor injection) and starts the application.
 */
public final class Main {

    /** Optional override: -Dsecurebank.data.dir=C:\some\folder. Default: "data" in the working directory. */
    private static final String DATA_DIR_PROPERTY = "securebank.data.dir";

    private Main() {
    }

    public static void main(String[] args) {
        try {
            Path dataDir = Paths.get(System.getProperty(DATA_DIR_PROPERTY, "data")).toAbsolutePath().normalize();
            Clock clock = Clock.systemDefaultZone();
            AuditLogger audit = AuditLogger.toFile(dataDir.resolve("audit.log"));

            UserRepository users = new FileUserRepository(dataDir.resolve("users.txt"));
            AccountRepository accounts = new FileAccountRepository(dataDir.resolve("accounts.txt"));
            TransactionRepository transactions = new FileTransactionRepository(dataDir.resolve("transactions.txt"));

            ConsoleUtil console = ConsoleUtil.forSystem();
            console.printLine("====== SECURE BANKING APPLICATION ======");
            console.printLine("Data folder: " + dataDir);
            console.printLine("Loaded " + users.count() + " user(s), " + accounts.count() + " account(s), "
                    + transactions.count() + " transaction(s).");

            AuthenticationService auth = new AuthenticationService(users, new PasswordHasher(),
                    new LoginAttemptTracker(clock), audit, clock);
            BankingService banking = new BankingService(accounts, transactions, audit, clock);
            new BankingApplication(console, auth, banking, audit).run();
        } catch (PersistenceException e) {
            System.err.println("The application could not start: " + e.getMessage());
            System.exit(1);
        }
    }
}
