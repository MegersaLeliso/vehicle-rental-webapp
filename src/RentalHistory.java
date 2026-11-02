import java.util.Arrays;

/**
 * RentalHistory.java
 *
 * Belongs to a single Customer — tracks every rental transaction
 * they've ever made.
 *
 * DESIGN CHOICE: This class only manages a fixed array of transactions.
 * It doesn't know about Vehicles, pricing, or eligibility — its whole
 * job is bookkeeping. This is intentional: if we later wanted to swap
 * the storage (e.g., persist to a database), only this class changes,
 * not Customer or RentalAgency.
 */
public class RentalHistory {

    private static final int DEFAULT_CAPACITY = 100;

    private final RentalTransaction[] transactions;
    private int count;

    public RentalHistory() {
        this(DEFAULT_CAPACITY);
    }

    public RentalHistory(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Rental history capacity cannot be negative.");
        }
        this.transactions = new RentalTransaction[capacity];
    }

    public void addTransaction(RentalTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Transaction cannot be null.");
        }
        if (count == transactions.length) {
            throw new IllegalStateException("Rental history has reached its transaction capacity.");
        }
        transactions[count++] = transaction;
    }

    /**
     * Returns a defensive copy so external code cannot mutate the internal history.
     */
    public RentalTransaction[] getTransactions() {
        return Arrays.copyOf(transactions, count);
    }

    public double getTotalSpent() {
        return Arrays.stream(transactions, 0, count)
                .mapToDouble(RentalTransaction::getTotalCost)
                .sum();
    }

    public void printHistory() {
        if (count == 0) {
            System.out.println("No rental history yet.");
            return;
        }
        System.out.println("=== Rental History ===");
        Arrays.stream(transactions, 0, count).forEach(RentalTransaction::printReceipt);
        System.out.printf("Total spent across all rentals: $%.2f%n", getTotalSpent());
    }
}
