package plugins.api.suite;

import lombok.Builder;
import lombok.Value;

/**
 * Knobs for a single run.
 */
@Value
@Builder(toBuilder = true)
public class SuiteOptions {

    /**
     * Whether to include tests marked destructive. Off by default: those spend real resources, such
     * as the grand exchange test placing live orders.
     */
    @Builder.Default
    boolean includeDestructive = false;

    /**
     * Whether the runner establishes each test's declared preconditions before running it.
     *
     * <p>Turning this off runs tests exactly as they stand, which is the quickest way to tell whether
     * a failure is a real regression or the setup putting the world in the wrong state.</p>
     */
    @Builder.Default
    boolean establishPreconditions = true;

    /** How long any single test may run before it is cancelled and recorded as failed. */
    @Builder.Default
    long perTestTimeoutMs = 180_000;

    /** Whether to abort the whole run when the self check fails. */
    @Builder.Default
    boolean abortOnSelfCheckFailure = true;

    /** Default options. */
    public static SuiteOptions defaults() {
        return SuiteOptions.builder().build();
    }
}
