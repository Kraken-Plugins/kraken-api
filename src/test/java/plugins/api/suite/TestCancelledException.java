package plugins.api.suite;

/**
 * Thrown when a run is cancelled, either by the user stopping it or by a per-test timeout firing.
 *
 * <p>This is a control signal, not a failure. A test unwound by cancellation must be recorded as
 * cancelled rather than failed, or stopping a run would fill the results with red that looks exactly
 * like a genuine regression.</p>
 */
public class TestCancelledException extends RuntimeException {

    /**
     * Creates a cancellation signal.
     *
     * @param message what was being waited on when the cancellation was noticed
     */
    public TestCancelledException(String message) {
        super(message);
    }
}
