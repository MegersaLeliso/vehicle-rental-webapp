/**
 * Car.java
 *
 * Concrete subclass of Vehicle. Represents a standard passenger car.
 *
 * DESIGN CHOICE: Cars have the simplest eligibility rules of the three
 * vehicle types (just a minimum age), making this a good "baseline"
 * example before Motorcycle and Truck add more complex conditions.
 */
public class Car extends Vehicle {

    // Field unique to Car — not every Vehicle needs this, which is
    // exactly why it lives here instead of in the base class.
    private final int numberOfSeats;

    private static final int MINIMUM_AGE = 18;

    public Car(String licensePlate, String make, String model, int year,
               double baseDailyRate, int numberOfSeats) {
        // Reuses the parent constructor for all shared fields —
        // avoids duplicating that setup logic (a benefit of inheritance).
        super(licensePlate, make, model, year, baseDailyRate);
        this.numberOfSeats = numberOfSeats;
    }

    // POLYMORPHISM: This overrides Vehicle's abstract method. Any code
    // holding a reference to this object as type "Vehicle" will still
    // run THIS version of calculateRentalCost() at runtime.
    @Override
    public double calculateRentalCost(int days) {
        // Simple pricing as agreed: dailyRate x days, no surcharges.
        return getBaseDailyRate() * days;
    }

    @Override
    public boolean checkEligibility(Customer customer) {
        // Cars just need a minimum age — no special license class required.
        return customer.getAge() >= MINIMUM_AGE;
    }

    @Override
    public String getVehicleType() {
        return "Car";
    }

    // Extending the parent's getInfo() rather than replacing it —
    // keeps the shared format consistent while adding Car-specific detail.
    @Override
    public String getInfo() {
        return super.getInfo() + String.format(" | Seats: %d", numberOfSeats);
    }

    public int getNumberOfSeats() {
        return numberOfSeats;
    }
}
