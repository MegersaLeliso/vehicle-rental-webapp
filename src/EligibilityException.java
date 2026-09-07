/**
 * EligibilityException.java
 *
 * Thrown when a Customer fails a Vehicle's checkEligibility() rule
 * (e.g., underage motorcycle rental, wrong license class for a truck).
 *
 * Kept separate from VehicleUnavailableException even though both are
 * "rental failed" scenarios, because they represent DIFFERENT problems
 * with different fixes: one is about vehicle state, the other is about
 * customer qualifications. Callers may want to handle them differently
 * (e.g., suggest a different vehicle vs. suggest waiting for a return).
 */
public class EligibilityException extends Exception {
    public EligibilityException(String message) {
        super(message);
    }
}
