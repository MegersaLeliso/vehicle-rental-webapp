import java.util.ArrayList;
import java.util.List;

/**
 * RentalHistory.java
 *
 * Belongs to a single Customer — tracks every rental transaction
 * they've ever made.
 *
 * DESIGN CHOICE: This class only manages a List<RentalTransaction>.
 * It doesn't know about Vehicles, pricing, or eligibility — its whole
 * job is bookkeeping. This is intentional: if we later wanted to swap
 * the storage (e.g., persist to a database), only this class changes,
 * not Customer or RentalAgency.
 */
public class RentalHistory {

    // ENCAPSULATION: the list itself is private. We never return this
    // exact reference — see getTransactions() below — to prevent
    // outside code from adding/removing entries without going through
    // addTransaction().
    private final List<RentalTransaction> transactions;

    public RentalHistory() {
        this.transactions = new ArrayList<>();
    }

    public void addTransaction(RentalTransaction transaction) {
        transactions.add(transaction);
    }

    /**
     * Returns a defensive copy so external code can read the history
     * without being able to mutate the internal list directly.
     */
    public List<RentalTransaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public double getTotalSpent() {
        double total = 0.0;
        for (RentalTransaction t : transactions) {
            total += t.getTotalCost();
        }
        return total;
    }

    public void printHistory() {
        if (transactions.isEmpty()) {
            System.out.println("No rental history yet.");
            return;
        }
        System.out.println("=== Rental History ===");
        for (RentalTransaction t : transactions) {
            t.printReceipt();
        }
        System.out.printf("Total spent across all rentals: $%.2f%n", getTotalSpent());
    }
}
