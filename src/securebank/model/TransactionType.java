package securebank.model;

/**
 * transaction the bank records  ,an enum (rather than free text)
 * so an invalid type can never be created or loaded
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL
}
