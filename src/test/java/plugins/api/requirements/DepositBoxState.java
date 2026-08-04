package plugins.api.requirements;

/**
 * The state the deposit box interface must be in when a test starts.
 */
public enum DepositBoxState {
    /** The deposit box interface must be open. */
    OPEN,
    /** The deposit box interface must be closed. */
    CLOSED,
    /** The test does not care either way. */
    ANY
}
