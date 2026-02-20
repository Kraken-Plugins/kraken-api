package com.kraken.api.simulation;

import lombok.Getter;

/**
 * Context passed to action-policy providers and scoring rules.
 */
@Getter
public final class SimulationActionPolicyContext {
    private final SimulationEngine engine;
    private final SimulationState state;
    private final int depthRemaining;

    /**
     * Creates policy context.
     *
     * @param engine simulation engine in use.
     * @param state simulation state for this policy callback.
     * @param depthRemaining remaining decision-tree depth.
     */
    public SimulationActionPolicyContext(SimulationEngine engine, SimulationState state, int depthRemaining) {
        if (engine == null) {
            throw new IllegalArgumentException("engine cannot be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        this.engine = engine;
        this.state = state;
        this.depthRemaining = depthRemaining;
    }
}
