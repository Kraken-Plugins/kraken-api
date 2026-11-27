package com.kraken.api.simulation.tick;

import com.kraken.api.simulation.world.SimState;

/**
 * A planner's seam for its own per tick measures (into {@code SimState.accLane}): called once per tick in P4, after
 * the player's move and attack and the core's sight exposure count, before scheduled spawns. Null by default.
 */
@FunctionalInterface
public interface TickObserver {
    /**
     * Records this tick's measures.
     * @param st the state being advanced
     */
    void afterPlayerTurn(SimState st);
}
