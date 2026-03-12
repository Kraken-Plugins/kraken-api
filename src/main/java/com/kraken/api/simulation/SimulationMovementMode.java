package com.kraken.api.simulation;

/**
 * Strategy for generating movement destinations during tree expansion.
 */
public enum SimulationMovementMode {
    /**
     * Expands movement actions to reachable tiles inside a configured radius from the player.
     */
    RADIUS,

    /**
     * Expands movement actions to all reachable tiles within the remaining simulation horizon.
     */
    REACHABLE
}
