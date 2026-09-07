/**
 * Vehicle.java
 *
 * Abstract base class representing any rentable vehicle in the fleet.
 *
 * DESIGN CHOICE: This class is declared 'abstract' because a generic
 * "Vehicle" should never be instantiated on its own — every real vehicle
 * in our system is a specific type (Car, Motorcycle, Truck). Abstraction
 * forces subclasses to define their own pricing and eligibility rules.
 */
public abstract class Vehicle {

    // ENCAPSULATION: fields are private. Nothing outside this class can
    // directly mutate mileage or availability — they can only be changed
    // through controlled methods like rent(), returnVehicle(), addMileage().
    private final String licensePlate;
    private final String make;
    private final String model;
    private final int year;
    private final double baseDailyRate;
    private double mileage;
    private boolean isAvailable;

    public Vehicle(String licensePlate, String make, String model, int year, double baseDailyRate) {
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.year = year;
        this.baseDailyRate = baseDailyRate;
        this.mileage = 0.0;
        this.isAvailable = true; // every new vehicle starts available
    }

    // --- ABSTRACT METHODS ---
    // Every subclass MUST provide its own implementation. This is where
    // POLYMORPHISM comes in later: calling vehicle.calculateRentalCost()
    // will run different code depending on whether 'vehicle' is a Car,
    // Motorcycle, or Truck — without the caller needing to know which.

    /**
     * Calculates the total rental cost for a given number of days.
     * Each vehicle type may apply its own rate logic.
     */
    public abstract double calculateRentalCost(int days);

    /**
     * Checks whether a given customer is eligible to rent this vehicle.
     * Each vehicle type enforces its own rules (age, license class, etc.).
     */
    public abstract boolean checkEligibility(Customer customer);

    /**
     * Returns a short label describing the vehicle type.
     * Useful for reporting/logging without using instanceof checks.
     */
    public abstract String getVehicleType();

    // --- SHARED BEHAVIOR (not abstract — same for every vehicle) ---

    /**
     * Marks the vehicle as rented. Encapsulation ensures this is the
     * ONLY way isAvailable can become false.
     */
    public void rent() {
        this.isAvailable = false;
    }

    /**
     * Marks the vehicle as available again after being returned.
     */
    public void returnVehicle() {
        this.isAvailable = true;
    }

    /**
     * Adds mileage after a completed rental. Guards against invalid
     * (negative) input — a core benefit of encapsulation.
     */
    public void addMileage(double milesDriven) {
        if (milesDriven < 0) {
            throw new IllegalArgumentException("Mileage driven cannot be negative.");
        }
        this.mileage += milesDriven;
    }

    /**
     * Returns a human-readable summary of this vehicle.
     * Subclasses can call this via super.getInfo() and append extra details.
     */
    public String getInfo() {
        return String.format(
            "[%s] %d %s %s | Plate: %s | Rate: $%.2f/day | Mileage: %.1f | Available: %s",
            getVehicleType(), year, make, model, licensePlate, baseDailyRate, mileage, isAvailable
        );
    }

    // --- GETTERS (controlled read access — no public setters for these) ---

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public double getBaseDailyRate() {
        return baseDailyRate;
    }

    public double getMileage() {
        return mileage;
    }

    public boolean isAvailable() {
        return isAvailable;
    }
}
