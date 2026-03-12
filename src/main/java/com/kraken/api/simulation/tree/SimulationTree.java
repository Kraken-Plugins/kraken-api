package com.kraken.api.simulation.tree;

import com.kraken.api.simulation.SimulationScenario;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Simulation tree output from a single scenario run.
 */
@Getter
@AllArgsConstructor
public final class SimulationTree {
    private final SimulationScenario scenario;
    private final SimulationTreeOptions options;
    private final SimulationTreeNode root;
    private final int nodeCount;
    private final int maxDepthReached;
}
