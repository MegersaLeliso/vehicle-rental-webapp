/**
 * VehicleUnavailableException.java
 *
 * DESIGN CHOICE: A custom checked exception, thrown when someone tries
 * to rent a vehicle that's already rented out. Using a specific
 * exception class (rather than a generic RuntimeException or a boolean
 * return code) makes error handling explicit and self-documenting at
 * the call site: catch (VehicleUnavailableException e) tells you
 * exactly what went wrong without reading the method body.
 *
 * This also demonstrates INHERITANCE within Java's own exception
 * hierarchy — we extend Exception, inheriting its message-handling
 * and stack-trace behavior for free.
 */
public class VehicleUnavailableException extends Exception {
    public VehicleUnavailableException(String message) {
        super(message);
    }
}
