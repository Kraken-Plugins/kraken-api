package plugins.api.requirements;

/**
 * The state the bank interface must be in when a test starts.
 *
 * <p>This exists because tests genuinely disagree: the spell service test needs the bank already open
 * so it can withdraw runes, while the bank service test asserts it can open the bank itself and so
 * must start with it closed. Before this was declared, running them in either order failed one of
 * them for reasons that had nothing to do with the API.</p>
 */
public enum BankState {
    /** The bank interface must be open. */
    OPEN,
    /** The bank interface must be closed. */
    CLOSED,
    /** The test does not care either way. */
    ANY
}
