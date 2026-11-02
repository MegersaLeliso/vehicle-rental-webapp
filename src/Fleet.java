import java.util.Arrays;
import java.util.Objects;

/**
 * Manages the vehicles owned by a RentalAgency.
 */
public class Fleet {

    private final Vehicle[] vehicles;
    private int count;

    public Fleet(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Fleet capacity cannot be negative.");
        }
        this.vehicles = new Vehicle[capacity];
    }

    public void addVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null.");
        if (count == vehicles.length) {
            throw new IllegalStateException("Fleet has reached its vehicle capacity.");
        }
        vehicles[count++] = vehicle;
    }

    /**
     * Returns all vehicles currently available to rent.
     */
    public Vehicle[] findAvailableVehicles() {
        return Arrays.stream(vehicles, 0, count)
                .filter(Vehicle::isAvailable)
                .toArray(Vehicle[]::new);
    }

    /**
     * Finds a vehicle by license plate, or null if not found.
     */
    public Vehicle findVehicleByPlate(String plate) {
        return Arrays.stream(vehicles, 0, count)
                .filter(vehicle -> vehicle.getLicensePlate().equalsIgnoreCase(plate))
                .findFirst()
                .orElse(null);
    }

    public Vehicle[] getAllVehicles() {
        return Arrays.copyOf(vehicles, count);
    }

    public void printFleetStatus() {
        System.out.println("=== Fleet Status ===");
        Arrays.stream(vehicles, 0, count)
                .forEach(vehicle -> System.out.println(vehicle.getInfo()));
    }
}
