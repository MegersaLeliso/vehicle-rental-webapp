/**
 * Customer.java
 *
 * Represents a person renting vehicles from the agency.
 *
 * DESIGN CHOICE: Customer doesn't know anything about pricing or
 * eligibility rules — it just exposes its own attributes (age, license
 * class, certifications). It's the VEHICLE's job to decide whether
 * those attributes are good enough. This keeps responsibilities clean:
 * Customer describes itself; Vehicle subclasses interpret that data.
 */
public class Customer {

    // ENCAPSULATION: private fields, accessed only through getters.
    // No setters for identity fields (name, licenseNumber) since a
    // customer's core identity shouldn't silently change mid-program.
    private final String name;
    private final String licenseNumber;
    private final int age;
    private final String licenseClass; // e.g., "Standard" or "Commercial"
    private final boolean helmetCertification;

    // COMPOSITION: a Customer "has-a" RentalHistory. The Customer object
    // owns this history — it's created here and lives as long as the
    // Customer does. RentalHistory has no meaning without a Customer.
    private final RentalHistory rentalHistory;

    public Customer(String name, String licenseNumber, int age,
                     String licenseClass, boolean helmetCertification) {
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.age = age;
        this.licenseClass = licenseClass;
        this.helmetCertification = helmetCertification;
        this.rentalHistory = new RentalHistory(); // every customer starts with an empty history
    }

    /**
     * Records a completed or active rental transaction into this
     * customer's history. Delegates the actual storage logic to
     * RentalHistory rather than managing a list here directly —
     * each class handles only what it's responsible for.
     */
    public void addRentalRecord(RentalTransaction transaction) {
        rentalHistory.addTransaction(transaction);
    }

    public RentalHistory getRentalHistory() {
        return rentalHistory;
    }

    // --- Getters used by Vehicle subclasses for eligibility checks ---

    public String getName() {
        return name;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public int getAge() {
        return age;
    }

    public String getLicenseClass() {
        return licenseClass;
    }

    public boolean hasHelmetCertification() {
        return helmetCertification;
    }

    @Override
    public String toString() {
        return String.format("%s (Age: %d, License: %s [%s])",
                name, age, licenseNumber, licenseClass);
    }
}
