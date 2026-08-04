package plugins.api.requirements;

/**
 * Something a test does to the world that outlives it.
 *
 * <p>Declarations here are <b>advisory</b>. The precondition engine always re-establishes state from
 * what it observes rather than from what was declared, so a missing side effect degrades ordering and
 * skips a cheap cleanup, but can never produce a wrong result. Only the order planner and the
 * baseline restorer read them.</p>
 */
public enum SideEffect {

    /** Leaves the player somewhere other than where the test started. */
    MOVES_PLAYER,

    /** Teleports the player, potentially to another part of the map. */
    TELEPORTS,

    /** Hops worlds, reloading the scene and invalidating every cached entity. */
    HOPS_WORLDS,

    /** Deposits the whole inventory. */
    EMPTIES_INVENTORY,

    /** Deposits everything worn. */
    STRIPS_EQUIPMENT,

    /** Finishes with gear still equipped that the test withdrew. */
    LEAVES_GEAR_EQUIPPED,

    /** Drops items on the floor. */
    DROPS_ITEMS,

    /** Consumes items permanently, by eating, cooking, crafting or depositing them. */
    CONSUMES_ITEMS,

    /** May finish with the bank, deposit box or exchange interface still open. */
    LEAVES_INTERFACE_OPEN,

    /** Moves the camera away from wherever it was. */
    CHANGES_CAMERA,

    /** Turns prayers on. */
    TOGGLES_PRAYER,

    /** Enables the special attack. */
    ENABLES_SPEC,

    /** Places real orders on the Grand Exchange, spending real coins. */
    TRADES_ON_GE
}
