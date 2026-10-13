

# Secure Banking Application

A Java console application for registering users, opening bank accounts and making deposits and withdrawals. This project demonstrates object-oriented programming and secure coding in a small banking system. It stores users, accounts and transaction history in text files and loads them when the application starts.

## Student details

- **Name:** Tanatswa Princess Muzani
- **Registration number:** H250200A

## What the application does

After registering and logging in, a user can:

- Open a savings or current account, with a maximum of five accounts per user.
- View their accounts and balances.
- Deposit or withdraw money.
- View an account's transaction history, starting with the most recent transaction.

Each account gets a random 10-digit account number. Savings accounts cannot go below a zero balance. Current accounts have a $500 overdraft limit, so their balance can go down to -$500.00.

Users can only access their own accounts. The application also records security events, such as failed logins and attempts to access another user's account.

## Running the project

You need **JDK 11 or newer**.

### IntelliJ IDEA

1. Open the `SecureBankApp-H250200A` folder using **File > Open**.
2. Select JDK 11 or newer if prompted.
3. Select the **Run Banking App** configuration and click Run.
4. To run the tests, select **All Tests** and click Run.

### Terminal

Run these commands from the project folder.

**Linux or macOS:**

```bash
mkdir -p out
javac -d out $(find src test -name '*.java')
java -cp out securebank.Main
```

**Windows PowerShell:**

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem -Path src,test -Recurse -Filter *.java).FullName
java -cp out securebank.Main
```

To run the tests after compiling:

```text
java -cp out securebank.test.TestRunner
```

Passwords are hidden when the application runs in a terminal that supports password input. The IntelliJ Run window cannot hide them, so the application displays a notice.

By default, files are saved in `data/` under the current working directory. The application prints the full data-folder path at startup. The IntelliJ run configuration uses the project folder.

To choose another folder, pass the `securebank.data.dir` property:

```text
java -Dsecurebank.data.dir=C:\bank-data -cp out securebank.Main
```

## Test accounts

The sample data includes the following users:

| Username | Test password |
| --- | --- |
| `tanatswa_test` | `Test@Bank2026!` |
| `Monet` | `M4RRYM3M4RRYM3#` |

These credentials are for demonstration only. You can also select **Register** to create a new user. New users, accounts and transactions are saved in the same data folder and loaded on the next run.

To start with an empty system, close the application and clear or delete `users.txt`, `accounts.txt`, `transactions.txt` and `audit.log`. This removes the saved users, accounts, transaction history and logs.

## Project structure

```text
SecureBankApp-H250200A/
├── src/securebank/
│   ├── Main.java
│   ├── app/
│   ├── model/
│   ├── service/
│   ├── security/
│   ├── repository/
│   ├── util/
│   └── exception/
├── test/securebank/test/
├── data/
├── README.md
└── .gitignore
```

`Main` loads the stored data, creates the application objects and starts the console menu. `BankingApplication` handles user interaction. `AuthenticationService` handles registration and login, while `BankingService` handles account operations and transactions.

The model classes represent users, accounts and transactions. Repository interfaces separate the banking logic from file storage, and their file-based implementations read and write the text files. Validation, console output, file-writing helpers and audit logging are kept in the utility package.

## Design decisions

Text files meet the assignment's storage requirement. Keeping file operations in `FileUserRepository`, `FileAccountRepository` and `FileTransactionRepository` means the services can work with users and accounts without handling file paths or record parsing themselves.

Savings and current accounts share their basic behaviour through `Account`. Their overdraft rules differ: a savings account with $100 should reject a $120 withdrawal, while a current account with $100 can allow it and finish with a balance of -$20, within its $500 overdraft limit.

Validation happens at both the console and service levels. The console can give early feedback about an invalid amount, while the service still checks it when called directly by a test or another part of the application.

Passing a `Clock` into the login-related code lets the tests check the two-minute lockout without waiting two real minutes.

## Object-oriented design

The account model is where the main OOP principles are demonstrated:

| Principle | Example in the project |
| --- | --- |
| Encapsulation | `Account` keeps its balance private. Changes go through `credit()` and `debit()` so account rules can be checked. |
| Inheritance | `SavingsAccount` and `CurrentAccount` extend the abstract `Account` class. |
| Polymorphism | The service works with `Account` objects and calls methods such as `getOverdraftLimit()` without needing to know the specific account class. |
| Abstraction | The abstract `Account` class defines shared account behaviour. Repository interfaces hide the details of file storage. |

`User` and `Transaction` use final fields. `AccountType.newAccount(...)` creates the appropriate account subclass. Dependencies are passed through constructors, including a `Clock`, so tests can use controlled time and substitute storage implementations.

## Security choices

### Passwords and login

Passwords are stored as salted **PBKDF2-HMAC-SHA256 hashes**, using 600,000 iterations and a random 16-byte salt for each password. Hash comparison uses a constant-time comparison. Passwords are handled as character arrays and cleared after use.

A password must be 10–128 characters long and include an uppercase letter, a lowercase letter, a digit and a symbol. It must not contain the username.

Login failures use a generic message so the response does not reveal whether a username exists. Unknown usernames still trigger password-hashing work. Three failed attempts lock the username for two minutes.

### Input and output

Usernames accept only letters, digits and underscores, with a length of 3–20 characters. Account numbers must contain exactly 10 digits. Amounts must be plain decimal values with no more than two decimal places. Inputs such as `-5`, `1e5` and `1.234` are rejected.

Console input is limited to 256 characters, and the banking service checks amounts again before processing a transaction. Console output replaces control characters and direction-override characters with `?` to prevent them from affecting the terminal display.

### Access control

Banking operations check account ownership against the logged-in user. An account belonging to another user produces the same response as an account that does not exist. Denied access is recorded in the audit log.

Repositories return copies of records, and the transaction ledger is append-only.

### Protecting text-file records

This application does not use a database, so parameterised SQL queries do not apply. For text storage, `RecordCodec` escapes field separators and line breaks so values cannot create extra fields or records. File names are fixed rather than supplied by the user.

### Errors, logging and file writes

Expected errors show a message the user can act on. Unexpected errors show a generic message, while the audit log records the exception type without printing a stack trace on screen.

The audit log records login attempts, lockouts, transactions and denied access. It excludes passwords and password hashes, masks account numbers and cleans log text to reduce the risk of forged log entries. A logging failure does not stop the application.

Storage writes use a temporary file followed by replacement of the target file. Records are written before the corresponding in-memory data is updated. The banking service includes rollback handling when a transaction cannot be saved. Damaged data lines are skipped without displaying their contents.

## Stored data

Each text file contains one record per line. Fields are separated by `|`, with escaping handled by `RecordCodec`.

| File | Fields |
| --- | --- |
| `users.txt` | Username, password hash, creation time |
| `accounts.txt` | Account number, account type, owner, balance, creation time |
| `transactions.txt` | Transaction ID, account number, transaction type, amount, timestamp, balance after the transaction |
| `audit.log` | Timestamp, event, username, detail |

Password hashes use the format `PBKDF2WithHmacSHA256$iterations$salt$hash`. Audit timestamps use UTC.

## Testing

The project includes 55 tests across eight test suites and uses a custom `TestRunner` without external test libraries.

The tests cover account rules, overdraft limits, input validation, password hashing, login failures, lockouts and account ownership. They also cover record escaping, reloading saved data, damaged records, rollback after a write failure, console output handling and log-forging attempts.

Run **All Tests** in IntelliJ or use the terminal command above to check the results in your environment.


I separated file storage from the banking logic so that `BankingService` focuses on account rules, such as checking balances and withdrawal limits. The repository classes handle reading and writing the text files. This makes the code easier to test and maintain, because changes to the file format do not need to change the banking rules.

## Limitations

- Only one application instance should use a data folder at a time.
- Login lockouts are held in memory and are cleared when the application restarts.
- Data files are not encrypted.
- This is an educational project and should only be used with test data.
