# Secure Banking Application

## Description
A console-based banking application in Java where users register, log in, open savings or current accounts and deposit or withdraw money.
It uses a layered object-oriented design and applies secure coding practices like salted PBKDF2 password hashing, strict input validation, output encoding, ownership checks, safe error handling and audit logging.
All data is saved to text files in the `data/` folder and loaded again when the application starts.

## Student Details
- **Name:** Tanatswa Princess Muzani
- **Registration Number:** H250200A

## Features
- **User authentication:** register and log in; passwords are stored only as salted PBKDF2-HMAC-SHA256 hashes and must meet a strong password policy.
- **Brute-force protection:** 3 wrong passwords lock a username for 2 minutes.
- **Account management:** create Savings or Current accounts (up to 5 per user) with a random 10-digit account number; view all your accounts and the balance of any account you own.
- **Transactions:** deposit and withdraw. Invalid amounts are rejected. Savings accounts cannot be overdrawn; Current accounts can go down to -$500.00.
- **Transaction history:** every deposit and withdrawal per account, newest first.
- **Data persistence:** users, accounts and transactions are saved to `users.txt`, `accounts.txt` and `transactions.txt`, and loaded on start-up.
- **Audit log:** logins, failures, lockouts, transactions and denied access are recorded in `data/audit.log` (no passwords, masked account numbers).
- **Automated tests:** 55 tests covering the model, validation, hashing, login, banking rules, persistence and logging.

---

## Sample Data (evidence of testing)
The `data/` folder already contains files produced by running the application: one test user, two accounts and five transactions.

| Test user | Password |
|---|---|
| `tanatswa_test` | `Test@Bank2026!` |
| `Monet` | `M4RRYM3M4RRY3#` |

These are throwaway password used only for testing. You can log in as this user, or choose **Register** to create your own user and accounts; new records are added to the same files and reloaded on the next start.

## How to Run using Intellij IDEA

**IntelliJ IDEA**
1. **File > Open** and select the `SecureBankApp-H250200A` folder. If asked, choose a JDK 11 or newer.
2. Select the **Run Banking App** configuration and press Run.
3. Select **All Tests** and press Run to run the tests.

**Terminal** (from the project folder; JDK 11+). A real terminal hides typed passwords; the IntelliJ Run window cannot, so it shows a notice.
```
mkdir out
javac -d out $(find src test -name "*.java")      # Windows PowerShell: see note below
java -cp out securebank.Main
java -cp out securebank.test.TestRunner
```
Windows PowerShell: `javac -d out (Get-ChildItem -Recurse -Filter *.java src,test).FullName`

The start-up banner prints the full path of the data folder in use. By default it is `data/` in the folder you run from (IntelliJ is configured to use the project folder). To use another folder: `java -Dsecurebank.data.dir=C:\bank-data -cp out securebank.Main`.

---

## Where Each Requirement Is Implemented

| Requirement | Implemented in |
|---|---|
| **1. User authentication** | `AuthenticationService` (`register`, `login`), `PasswordHasher`, `LoginAttemptTracker`, menus in `BankingApplication` |
| **2. Account management** (create account, view balance) | `BankingService.createAccount / getAccounts / getOwnedAccount`; models `Account`, `SavingsAccount`, `CurrentAccount` |
| **3. Transactions** (deposit, withdraw) | `BankingService.deposit / withdraw / getTransactionHistory`; rules in `Account.credit / debit`; record in `Transaction` |
| **4. Data persistence** (text files, loaded on start) | `FileUserRepository`, `FileAccountRepository`, `FileTransactionRepository` load their file in the constructor; `Main` creates them at start-up; `FileStorageUtil` writes safely |

## Secure Coding Practices

| Practice | How it is done | Where |
|---|---|---|
| **Input validation** | Allow-lists with anchored regular expressions: username `[A-Za-z0-9_]{3,20}`, amounts as plain decimals (max 2 places), account numbers exactly 10 digits. Input lines are capped at 256 characters. The service validates amounts again. | `InputValidator`, `ConsoleUtil`, `BankingService` |
| **Output encoding** | Everything printed is encoded: control characters (such as terminal escape codes) and hidden direction-override characters become `?`. Log text is limited to safe characters. | `ConsoleUtil.encode`, `AuditLogger.clean` |
| **Injection defence** | There is no SQL, so there is no `PreparedStatement` to use. The equivalent risk in a text file is a `\|` or line break inside a value creating fake fields or records. `RecordCodec` plays the role of a parameterised query: values are escaped, never concatenated into a line. File names never come from user input. | `RecordCodec`, `File*Repository` |
| **Strong authentication and hashing** | PBKDF2-HMAC-SHA256, 600,000 iterations, a random 16-byte salt per password, constant-time comparison. Password policy: 10-128 characters with upper, lower, digit and symbol, no username inside. One generic login error, equal hashing work for unknown users, lockout after 3 failures. Passwords are `char[]` and wiped after use. | `PasswordHasher`, `InputValidator`, `LoginAttemptTracker`, `AuthenticationService` |
| **Least privilege** | Every banking method works only on the logged-in user's accounts. Another user's account gives the same "not found" answer as a missing one, and the attempt is logged. Repositories return copies, and the transaction ledger is append-only. | `BankingService`, `AccountRepository`, `TransactionRepository` |
| **Safe error handling** | Expected problems show a clear message; anything unexpected shows a generic message and only the exception type is logged (no stack traces on screen). If a transaction cannot be saved, the balance is rolled back. Damaged data lines are skipped without printing them. | `BankingApplication.reportFailure`, `BankingService.commit`, `File*Repository` |
| **Defensive logging** | Timestamped log of security events; never contains passwords, hashes or full account numbers (`******5643`); log lines cannot be forged; a logging failure never stops the app. | `AuditLogger` |
| **Safe file writing** | Files are replaced atomically (temp file, then rename); changes are written to disk before memory is updated. | `FileStorageUtil`, `File*Repository` |

## OOP Principles

| Principle | Where |
|---|---|
| **Inheritance** | `SavingsAccount` and `CurrentAccount` extend the abstract `Account` |
| **Polymorphism** | Code uses `Account` and calls `getType()` / `getOverdraftLimit()` without knowing the subclass |
| **Abstraction** | `Account` is abstract; `UserRepository`, `AccountRepository`, `TransactionRepository` hide how data is stored |
| **Encapsulation** | `Account` keeps its balance private and changes it only through `credit()` / `debit()`; `User` never exposes a password |
| **Immutability** | `Transaction` and `User` fields are final |
| **Factory** | `AccountType.newAccount(...)` builds the right account subclass |
| **Dependency injection** | `Main` creates the objects and passes them through constructors (including a `Clock`), so tests can swap them |
| **Single responsibility** | One job per package: `model`, `repository`, `service`, `security`, `util`, `app` |

## Project Structure
```
SecureBankApp-H250200A/
├── src/securebank/
│   ├── Main.java                 # start-up: load data files, connect classes, run
│   ├── app/BankingApplication    # console menus
│   ├── model/                    # Account (abstract), SavingsAccount, CurrentAccount, AccountType,
│   │                             # User, Transaction, TransactionType
│   ├── service/                  # AuthenticationService, BankingService
│   ├── security/                 # PasswordHasher, LoginAttemptTracker
│   ├── repository/               # 3 interfaces, 3 File* classes, RecordCodec
│   ├── util/                     # InputValidator, ConsoleUtil, FileStorageUtil, AuditLogger
│   └── exception/                # custom exceptions
├── test/securebank/test/         # TestRunner + 8 test suites (no external libraries)
├── data/                         # users.txt, accounts.txt, transactions.txt, audit.log
├── README.md
└── .gitignore
```

## Data File Formats
One record per line, fields separated by `|` (escaped by `RecordCodec`).

| File | Format |
|---|---|
| `users.txt` | `username\|passwordHash\|createdAt` (hash = `PBKDF2WithHmacSHA256$iterations$salt$hash`) |
| `accounts.txt` | `accountNumber\|type\|owner\|balance\|createdAt` |
| `transactions.txt` | `id\|accountNumber\|type\|amount\|timestamp\|balanceAfter` |
| `audit.log` | `timestamp\|EVENT\|username\|detail` (UTC) |

## Testing
Run **All Tests** or `java -cp out securebank.test.TestRunner`. All **55 tests pass**. They check, among other things: money rules and overdraft limits, rejected inputs (`-5`, `1e5`, `1.234`, bad account numbers, weak passwords), hashing and salts, escaping of `|` and line breaks, generic login errors, lockout and unlock, other users' accounts being refused, rollback when a write fails, reload after a restart, damaged data lines, output encoding and log forging.

## LIMITATIONS
- Single-user console program; do not run two copies on the same `data/` folder at once.
- Lockout state is kept in memory, so restarting clears it.
- Data files are not encrypted; the committed test data contains only a throwaway user.
- Educational project: not for real money or real credentials.
