import java.util.ArrayList;
import java.util.List;

/**
 * Fleet.java
 *
 * Manages the collection of Vehicles owned by a RentalAgency.
 *
 * DESIGN CHOICE: Fleet works entirely in terms of the abstract Vehicle
 * type. It never checks "is this a Car or Truck?" — it doesn't need to.
 * This is POLYMORPHISM at the collection level: a List<Vehicle> can
 * transparently hold Cars, Motorcycles, and Trucks together, and every
 * method here (like findAvailableVehicles) works identically regardless
 * of the concrete subtype.
 */
public class Fleet {

    private final List<Vehicle> vehicles;

    public Fleet() {
        this.vehicles = new ArrayList<>();
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
    }

    /**
     * Returns all vehicles currently available to rent.
     */
    public List<Vehicle> findAvailableVehicles() {
        List<Vehicle> available = new ArrayList<>();
        for (Vehicle v : vehicles) {
            if (v.isAvailable()) {
                available.add(v);
            }
        }
        return available;
    }

    /**
     * Finds a vehicle by license plate, or null if not found.
     * RentalAgency is responsible for handling a null result —
     * Fleet just reports facts about the collection it manages.
     */
    public Vehicle findVehicleByPlate(String plate) {
        for (Vehicle v : vehicles) {
            if (v.getLicensePlate().equalsIgnoreCase(plate)) {
                return v;
            }
        }
        return null;
    }

    public List<Vehicle> getAllVehicles() {
        return new ArrayList<>(vehicles); // defensive copy, same reasoning as RentalHistory
    }

    public void printFleetStatus() {
        System.out.println("=== Fleet Status ===");
        for (Vehicle v : vehicles) {
            // POLYMORPHISM IN ACTION: v.getInfo() calls each vehicle's
            // own overridden version, even though 'v' is typed as Vehicle.
            System.out.println(v.getInfo());
        }
    }
}
