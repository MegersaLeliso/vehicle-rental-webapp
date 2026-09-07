import java.time.LocalDate;

/**
 * RentalTransaction.java
 *
 * Represents a single rental event connecting one Customer to one
 * Vehicle over a period of time.
 *
 * DESIGN CHOICE: This class doesn't decide pricing or eligibility
 * itself — it just RECORDS the outcome of a rental that the
 * RentalAgency already validated and priced. Keeping "decision logic"
 * (in Vehicle/RentalAgency) separate from "record keeping" (here)
 * keeps each class focused on a single responsibility.
 */
public class RentalTransaction {

    private final Vehicle vehicle;
    private final Customer customer;
    private final int rentalDays;
    private final double totalCost;
    private final LocalDate startDate;
    private LocalDate endDate;
    private boolean isReturned;

    public RentalTransaction(Vehicle vehicle, Customer customer, int rentalDays, double totalCost) {
        this.vehicle = vehicle;
        this.customer = customer;
        this.rentalDays = rentalDays;
        this.totalCost = totalCost;
        this.startDate = LocalDate.now();
        this.isReturned = false;
    }

    /**
     * Marks this transaction as complete (vehicle returned).
     * Encapsulation ensures endDate/isReturned can only change together,
     * through this one method — never independently or out of order.
     */
    public void completeTransaction() {
        this.endDate = LocalDate.now();
        this.isReturned = true;
    }

    /**
     * Prints a simple receipt summarizing this transaction.
     */
    public void printReceipt() {
        System.out.println("----- RENTAL RECEIPT -----");
        System.out.println("Customer: " + customer.getName());
        System.out.println("Vehicle:  " + vehicle.getVehicleType() + " - " + vehicle.getMake() + " " + vehicle.getModel());
        System.out.println("Plate:    " + vehicle.getLicensePlate());
        System.out.println("Days:     " + rentalDays);
        System.out.printf("Total:    $%.2f%n", totalCost);
        System.out.println("Status:   " + (isReturned ? "Returned on " + endDate : "Active since " + startDate));
        System.out.println("---------------------------");
    }

    // --- Getters ---

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Customer getCustomer() {
        return customer;
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public boolean isReturned() {
        return isReturned;
    }
}
