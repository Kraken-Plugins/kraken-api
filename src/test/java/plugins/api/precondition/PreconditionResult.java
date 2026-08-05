package plugins.api.precondition;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The outcome of trying to put the world into the state a test asked for.
 *
 * <p>The distinction between {@link Outcome#UNSATISFIABLE} and {@link Outcome#SETUP_FAILED} is the
 * point of this type. "You have no rune platebody" is an environment problem and says nothing about
 * the API. "I clicked a bank booth three times and the interface never opened" is a possible
 * regression that happens to have surfaced during setup. Both keep the test out of the red column,
 * but they are not the same signal and a run summary should separate them.</p>
 */
@Getter
public class PreconditionResult {

    /** How the attempt ended. */
    public enum Outcome {
        /** The world is in the requested state. */
        SATISFIED,
        /** A requirement cannot be met here and no amount of retrying would help. */
        UNSATISFIABLE,
        /** The world did not respond as expected while being set up. Possibly an API regression. */
        SETUP_FAILED,
        /** The run was cancelled part way through setup. */
        CANCELLED
    }

    private final Outcome outcome;
    private final String reason;
    private final List<String> steps;

    private PreconditionResult(Outcome outcome, String reason, List<String> steps) {
        this.outcome = outcome;
        this.reason = reason;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    /**
     * Reports whether the world ended up in the requested state.
     *
     * @return true only for {@link Outcome#SATISFIED}
     */
    public boolean satisfied() {
        return outcome == Outcome.SATISFIED;
    }

    /**
     * The world is ready.
     *
     * @param steps what was done to get there, for the log and the run report
     * @return the result
     */
    public static PreconditionResult satisfied(List<String> steps) {
        return new PreconditionResult(Outcome.SATISFIED, null, steps);
    }

    /**
     * A requirement cannot be met in this environment.
     *
     * @param reason what is missing, phrased for a human
     * @param steps what had been done before giving up
     * @return the result
     */
    public static PreconditionResult unsatisfiable(String reason, List<String> steps) {
        return new PreconditionResult(Outcome.UNSATISFIABLE, reason, steps);
    }

    /**
     * The world did not respond as expected during setup.
     *
     * @param reason what failed, phrased for a human
     * @param steps what had been done before giving up
     * @return the result
     */
    public static PreconditionResult setupFailed(String reason, List<String> steps) {
        return new PreconditionResult(Outcome.SETUP_FAILED, reason, steps);
    }

    /**
     * The run was cancelled during setup.
     *
     * @param steps what had been done before cancelling
     * @return the result
     */
    public static PreconditionResult cancelled(List<String> steps) {
        return new PreconditionResult(Outcome.CANCELLED, "Cancelled during setup", steps);
    }

    @Override
    public String toString() {
        return outcome + (reason == null ? "" : ": " + reason);
    }
}
