import java.util.ArrayList;
import java.util.List;

/**
 * RentalAgency.java
 *
 * The top-level orchestrator that ties Fleet, Customers, and
 * RentalTransactions together. This is where the actual BUSINESS LOGIC
 * of renting a vehicle lives — validation, pricing, and record keeping
 * all get coordinated from here.
 *
 * DESIGN CHOICE: RentalAgency depends only on the abstract Vehicle
 * type when processing a rental. It calls vehicle.checkEligibility()
 * and vehicle.calculateRentalCost() without ever checking "if this is
 * a Car" or "if this is a Truck" — POLYMORPHISM eliminates the need
 * for that branching entirely.
 */
public class RentalAgency {

    private final String agencyName;
    private final Fleet fleet; // COMPOSITION: agency owns and manages its fleet
    private final List<Customer> customers;

    public RentalAgency(String agencyName) {
        this.agencyName = agencyName;
        this.fleet = new Fleet();
        this.customers = new ArrayList<>();
    }

    public void addVehicleToFleet(Vehicle vehicle) {
        fleet.addVehicle(vehicle);
    }

    public void registerCustomer(Customer customer) {
        customers.add(customer);
    }

    /**
     * Attempts to rent a vehicle to a customer.
     *
     * Throws specific, meaningful exceptions instead of returning null
     * or a boolean — this forces calling code (see Main.java) to
     * explicitly handle each failure case rather than silently
     * ignoring a bad rental.
     */
    public RentalTransaction rentVehicle(Customer customer, String licensePlate, int days)
            throws VehicleUnavailableException, EligibilityException {

        Vehicle vehicle = fleet.findVehicleByPlate(licensePlate);

        if (vehicle == null) {
            // Treated as an unavailable-style failure since, from the
            // customer's perspective, there's nothing to rent either way.
            throw new VehicleUnavailableException(
                    "No vehicle found with plate: " + licensePlate);
        }

        if (!vehicle.isAvailable()) {
            throw new VehicleUnavailableException(
                    vehicle.getVehicleType() + " (" + licensePlate + ") is currently rented out.");
        }

        // POLYMORPHISM: this calls Car's, Motorcycle's, or Truck's
        // own checkEligibility() implementation — RentalAgency has no
        // idea which one, and doesn't need to.
        if (!vehicle.checkEligibility(customer)) {
            throw new EligibilityException(
                    customer.getName() + " does not meet the eligibility requirements for this "
                            + vehicle.getVehicleType() + ".");
        }

        double cost = vehicle.calculateRentalCost(days);
        vehicle.rent();

        RentalTransaction transaction = new RentalTransaction(vehicle, customer, days, cost);
        customer.addRentalRecord(transaction);

        return transaction;
    }

    /**
     * Processes the return of a vehicle, completing its transaction
     * and making it available again.
     */
    public void returnVehicle(String licensePlate, RentalTransaction transaction) {
        Vehicle vehicle = fleet.findVehicleByPlate(licensePlate);
        if (vehicle == null) {
            System.out.println("Cannot return vehicle: plate not found in fleet.");
            return;
        }
        vehicle.returnVehicle();
        transaction.completeTransaction();
        System.out.println(vehicle.getVehicleType() + " (" + licensePlate + ") has been returned. Thank you!");
    }

    /**
     * Generates a simple summary report of agency activity.
     */
    public void generateReport() {
        System.out.println("=== " + agencyName + " - Agency Report ===");
        System.out.println("Total vehicles in fleet: " + fleet.getAllVehicles().size());
        System.out.println("Available vehicles: " + fleet.findAvailableVehicles().size());
        System.out.println("Registered customers: " + customers.size());

        double totalRevenue = 0.0;
        for (Customer c : customers) {
            totalRevenue += c.getRentalHistory().getTotalSpent();
        }
        System.out.printf("Total revenue collected: $%.2f%n", totalRevenue);
    }

    public Fleet getFleet() {
        return fleet;
    }

    public String getAgencyName() {
        return agencyName;
    }
}
