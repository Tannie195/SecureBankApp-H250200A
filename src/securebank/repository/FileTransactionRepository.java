package securebank.repository;

import securebank.model.Transaction;
import securebank.model.TransactionType;
import securebank.util.FileStorageUtil;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Transactions in {@code transactions.txt}: {@code id|accountNumber|type|amount|timestamp|balanceAfter}
 * Append-only: new records go to the end of the file and old ones are never changed
 */
public final class FileTransactionRepository implements TransactionRepository {

    private final Path file;
    private final List<Transaction> transactions = new ArrayList<>();

    public FileTransactionRepository(Path file) {
        this.file = file;
        int skipped = 0;
        for (String line : FileStorageUtil.readAllLines(file)) {
            try {
                List<String> f = RecordCodec.decode(line);
                if (f.size() != 6) {
                    throw new IllegalArgumentException();
                }
                transactions.add(new Transaction(f.get(0), f.get(1), TransactionType.valueOf(f.get(2)),
                        new BigDecimal(f.get(3)), LocalDateTime.parse(f.get(4)), new BigDecimal(f.get(5))));
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        if (skipped > 0) {
            System.err.println("[WARN] transactions.txt: " + skipped + " unreadable record(s) skipped.");
        }
    }

    @Override
    public void save(Transaction t) {
        FileStorageUtil.appendLine(file, RecordCodec.encode(Arrays.asList(t.getTransactionId(),
                t.getAccountNumber(), t.getType().name(), t.getAmount().toPlainString(),
                t.getTimestamp().toString(), t.getResultingBalance().toPlainString())));
        transactions.add(t); // memory only after the file write succeeded
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) {
        List<Transaction> matches = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.getAccountNumber().equals(accountNumber)) {
                matches.add(t);
            }
        }
        return matches;
    }

    @Override
    public int count() {
        return transactions.size();
    }
}
