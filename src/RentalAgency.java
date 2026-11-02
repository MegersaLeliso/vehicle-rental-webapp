import java.util.Arrays;
import java.util.Objects;

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
    private final Customer[] customers;
    private int customerCount;

    public RentalAgency(String agencyName, int vehicleCapacity, int customerCapacity) {
        if (customerCapacity < 0) {
            throw new IllegalArgumentException("Customer capacity cannot be negative.");
        }
        this.agencyName = agencyName;
        this.fleet = new Fleet(vehicleCapacity);
        this.customers = new Customer[customerCapacity];
    }

    public synchronized void addVehicleToFleet(Vehicle vehicle) {
        fleet.addVehicle(vehicle);
    }

    public synchronized void registerCustomer(Customer customer) {
        Objects.requireNonNull(customer, "Customer cannot be null.");
        if (customerCount == customers.length) {
            throw new IllegalStateException("Agency has reached its customer capacity.");
        }
        customers[customerCount++] = customer;
    }

    /**
     * Attempts to rent a vehicle to a customer.
     *
     * Throws specific, meaningful exceptions instead of returning null
     * or a boolean — this forces calling code (see Main.java) to
     * explicitly handle each failure case rather than silently
     * ignoring a bad rental in the server router.
     */
    public synchronized RentalTransaction rentVehicle(Customer customer, String licensePlate, int days)
            throws VehicleUnavailableException, EligibilityException {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Rental days must be greater than zero.");
        }
        boolean registered = Arrays.stream(customers, 0, customerCount)
                .anyMatch(registeredCustomer -> registeredCustomer == customer);
        if (!registered) {
            throw new IllegalArgumentException("Customer is not registered with this agency.");
        }

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
        RentalTransaction transaction = new RentalTransaction(vehicle, customer, days, cost);
        customer.addRentalRecord(transaction);
        vehicle.rent();

        return transaction;
    }

    /**
     * Processes the return of a vehicle, completing its transaction
     * and making it available again.
     */
    public synchronized boolean returnVehicle(String licensePlate) {
        Vehicle vehicle = fleet.findVehicleByPlate(licensePlate);
        if (vehicle == null || vehicle.isAvailable()) {
            return false;
        }

        RentalTransaction transaction = Arrays.stream(customers, 0, customerCount)
                .flatMap(customer -> Arrays.stream(customer.getRentalHistory().getTransactions()))
                .filter(rental -> !rental.isReturned()
                        && rental.getVehicle().getLicensePlate().equalsIgnoreCase(licensePlate))
                .findFirst()
                .orElse(null);
        if (transaction == null) {
            return false;
        }

        vehicle.returnVehicle();
        transaction.completeTransaction();
        return true;
    }

    public synchronized Customer[] getCustomers() {
        return Arrays.copyOf(customers, customerCount);
    }

    public synchronized Customer findCustomerByName(String name) {
        if (name == null) {
            return null;
        }
        return Arrays.stream(customers, 0, customerCount)
                .filter(customer -> customer.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public synchronized double getTotalRevenue() {
        return Arrays.stream(customers, 0, customerCount)
                .mapToDouble(customer -> customer.getRentalHistory().getTotalSpent())
                .sum();
    }

    /**
     * Generates a simple summary report of agency activity.
     */
    public synchronized void generateReport() {
        System.out.println("=== " + agencyName + " - Agency Report ===");
        System.out.println("Total vehicles in fleet: " + fleet.getAllVehicles().length);
        System.out.println("Available vehicles: " + fleet.findAvailableVehicles().length);
        System.out.println("Registered customers: " + customerCount);

        double totalRevenue = getTotalRevenue();
        System.out.printf("Total revenue collected: $%.2f%n", totalRevenue);
    }

    public synchronized Fleet getFleet() {
        return fleet;
    }

    public synchronized String getAgencyName() {
        return agencyName;
    }
}
