package plugins.api.requirements;

import com.kraken.api.Context;
import lombok.Value;

import java.util.function.Predicate;

/**
 * A one-off precondition the runner can check but has no general way to establish.
 *
 * <p>This is the escape hatch that keeps {@link TestRequirements} from growing a field per test. A
 * free Grand Exchange slot, "no dialogue is currently open", "the special attack is not already
 * enabled" — each has exactly one consumer, and modelling each as its own field would bloat the
 * value type without making anything clearer.</p>
 *
 * <p>The predicate is evaluated on the runner thread and must not mutate game state.</p>
 */
@Value
public class CustomRequirement {

    /**
     * What the check is looking for, phrased so it reads correctly in a skip reason, for example
     * {@code "a free Grand Exchange slot"}.
     */
    String description;

    /** The check itself. Must be side effect free. */
    Predicate<Context> check;

    /**
     * Creates a named check.
     *
     * @param description what the check is looking for, used verbatim in skip reasons
     * @param check the side effect free predicate to evaluate
     * @return the requirement
     */
    public static CustomRequirement of(String description, Predicate<Context> check) {
        return new CustomRequirement(description, check);
    }

    /**
     * Evaluates the check, treating a thrown exception as an unmet requirement.
     *
     * <p>A check that throws is reporting that it could not determine the answer, which is not the
     * same as the requirement being met. Failing closed keeps a broken check from letting a test run
     * against a world it was never meant to see.</p>
     *
     * @param ctx the API context to evaluate against
     * @return true when the requirement is satisfied
     */
    public boolean isSatisfied(Context ctx) {
        try {
            return check.test(ctx);
        } catch (Exception e) {
            return false;
        }
    }
}
