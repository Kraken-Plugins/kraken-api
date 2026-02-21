package com.kraken.api.simulation;

import lombok.Getter;
import lombok.NonNull;

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
    public SimulationActionPolicyContext(@NonNull SimulationEngine engine, @NonNull SimulationState state, int depthRemaining) {
        this.engine = engine;
        this.state = state;
        this.depthRemaining = depthRemaining;
    }
}
