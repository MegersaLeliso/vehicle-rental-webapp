/**
 * Motorcycle.java
 *
 * Concrete subclass of Vehicle. Represents a motorcycle rental.
 *
 * DESIGN CHOICE: Motorcycles have a STRICTER minimum age than cars,
 * plus an extra requirement (helmet safety certification). This shows
 * how subclasses can have eligibility rules that are structurally
 * similar to a sibling class (Car) but functionally different —
 * a good example of why polymorphism is useful: the RentalAgency
 * doesn't need an if/else chain to know which rule applies to which type.
 */
public class Motorcycle extends Vehicle {

    private final boolean requiresHelmetCert;

    private static final int MINIMUM_AGE = 21; // stricter than Car's 18

    public Motorcycle(String licensePlate, String make, String model, int year,
                       double baseDailyRate, boolean requiresHelmetCert) {
        super(licensePlate, make, model, year, baseDailyRate);
        this.requiresHelmetCert = requiresHelmetCert;
    }

    @Override
    public double calculateRentalCost(int days) {
        // Same simple formula, but note it's still independently defined
        // here — if we wanted a motorcycle-specific discount later,
        // this is the only place we'd need to change it.
        return getBaseDailyRate() * days;
    }

    @Override
    public boolean checkEligibility(Customer customer) {
        boolean meetsAge = customer.getAge() >= MINIMUM_AGE;

        // If this motorcycle requires helmet certification, the customer
        // must have it. This is a rule Car and Truck simply don't have.
        if (requiresHelmetCert) {
            return meetsAge && customer.hasHelmetCertification();
        }
        return meetsAge;
    }

    @Override
    public String getVehicleType() {
        return "Motorcycle";
    }

    @Override
    public String getInfo() {
        return super.getInfo() + String.format(" | Helmet Cert Required: %s", requiresHelmetCert);
    }

    public boolean isHelmetCertRequired() {
        return requiresHelmetCert;
    }
}
