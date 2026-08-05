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
    MOVES_PLAYER,
    TELEPORTS,
    HOPS_WORLDS,
    EMPTIES_INVENTORY,
    STRIPS_EQUIPMENT,
    LEAVES_GEAR_EQUIPPED,
    DROPS_ITEMS,
    CONSUMES_ITEMS,
    LEAVES_INTERFACE_OPEN,
    CHANGES_CAMERA,
    TOGGLES_PRAYER,
    TOGGLES_RUN,
    TRADES_ON_GE
}
