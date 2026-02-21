package com.kraken.api.simulation;

import lombok.Getter;

/**
 * Simulation tree output from a single scenario run.
 */
@Getter
public final class SimulationTree {
    private final SimulationScenario scenario;
    private final SimulationTreeOptions options;
    private final SimulationTreeNode root;
    private final int nodeCount;
    private final int maxDepthReached;

    SimulationTree(
            SimulationScenario scenario,
            SimulationTreeOptions options,
            SimulationTreeNode root,
            int nodeCount,
            int maxDepthReached
    ) {
        this.scenario = scenario;
        this.options = options;
        this.root = root;
        this.nodeCount = nodeCount;
        this.maxDepthReached = maxDepthReached;
    }
}
