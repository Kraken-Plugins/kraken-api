package plugins.api.suite;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * A snapshot of how a run is going, for the overlay and any future panel.
 *
 * <p>{@link #currentPhase} is what turns a silent forty second pause into "Travelling to Grand
 * Exchange". Without it a long walk is indistinguishable from a hang, which is the difference between
 * a harness you trust and one you keep restarting.</p>
 */
@Value
@Builder(toBuilder = true)
public class SuiteProgress {

    /** Whether a run is in progress. */
    boolean running;

    /** One based position of the test being run. */
    int currentIndex;

    /** How many tests are in the run. */
    int total;

    /** Id of the test being run, or null when idle. */
    String currentTestId;

    /** Display name of the test being run, or null when idle. */
    String currentTestName;

    /** What is happening right now, e.g. "Withdrawing items" or "Running Bank". */
    String currentPhase;

    /** How long the run has been going. */
    Duration elapsed;

    /** Counts so far. */
    int passed;
    int failed;
    int skipped;
    int cancelled;

    /** A progress snapshot representing "no run in progress". */
    public static SuiteProgress idle() {
        return SuiteProgress.builder().elapsed(Duration.ZERO).build();
    }

    /**
     * Renders the run's position for a compact display.
     *
     * @return a string such as {@code "12/31"}, or an empty string when idle
     */
    public String describePosition() {
        return total == 0 ? "" : currentIndex + "/" + total;
    }
}
