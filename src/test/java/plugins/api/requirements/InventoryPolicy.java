package plugins.api.requirements;

/**
 * How aggressively the runner should reshape the inventory and worn equipment before a test.
 */
public enum InventoryPolicy {

    /**
     * Resolve automatically: {@link #EXACT} when the test declares anything item shaped, otherwise
     * {@link #NO_CHANGE}. This is the default so that a test declaring no item requirements is never
     * sent on a bank trip it does not need.
     */
    AUTO,

    /**
     * Bank everything carried and worn, then withdraw exactly what was declared. The only policy that
     * produces a deterministic starting inventory, and therefore the one most tests want.
     */
    EXACT,

    /** Leave what is carried alone, but bank any forbidden items and add anything missing. */
    TOP_UP,

    /** Touch nothing. */
    NO_CHANGE
}
