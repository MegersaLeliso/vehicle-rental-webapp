/**
 * Truck.java
 *
 * Concrete subclass of Vehicle. Represents a commercial cargo truck.
 *
 * DESIGN CHOICE: Trucks require a commercial license class — a rule
 * that's completely different in *kind* from age-based checks in Car
 * and Motorcycle. This proves checkEligibility() isn't just "different
 * numbers," it's genuinely different LOGIC per subclass.
 */
public class Truck extends Vehicle {

    private final double cargoCapacityTons;
    private static final String REQUIRED_LICENSE_CLASS = "Commercial";

    public Truck(String licensePlate, String make, String model, int year,
                 double baseDailyRate, double cargoCapacityTons) {
        super(licensePlate, make, model, year, baseDailyRate);
        this.cargoCapacityTons = cargoCapacityTons;
    }

    @Override
    public double calculateRentalCost(int days) {
        return getBaseDailyRate() * days;
    }

    @Override
    public boolean checkEligibility(Customer customer) {
        // Trucks don't care about age at all — only license class matters.
        // This asymmetry is exactly why checkEligibility() needed to be
        // abstract rather than shared logic in the parent class.
        return REQUIRED_LICENSE_CLASS.equalsIgnoreCase(customer.getLicenseClass());
    }

    @Override
    public String getVehicleType() {
        return "Truck";
    }

    @Override
    public String getInfo() {
        return super.getInfo() + String.format(" | Cargo Capacity: %.1f tons", cargoCapacityTons);
    }

    public double getCargoCapacityTons() {
        return cargoCapacityTons;
    }
}
