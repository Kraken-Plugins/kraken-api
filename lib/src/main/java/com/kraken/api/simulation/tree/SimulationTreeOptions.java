package com.kraken.api.simulation.tree;

import com.kraken.api.simulation.SimulationMovementMode;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

/**
 * Controls simulation tree depth, movement expansion, and node limits.
 */
@Getter
@With
public final class SimulationTreeOptions {
    private static final int DEFAULT_TICKS = 15;
    private static final int DEFAULT_MOVEMENT_RADIUS = 6;
    private static final int DEFAULT_MAX_NODES = 20_000;
    private static final int DEFAULT_MAX_ACTIONS_PER_NODE = 96;
    private static final int DEFAULT_MAX_MOVEMENT_TARGETS = 64;

    private final int ticks;
    private final SimulationMovementMode movementMode;
    private final int movementRadius;
    private final boolean includeWalkActions;
    private final boolean includeRunActions;
    private final int maxNodes;
    private final int maxActionsPerNode;
    private final int maxMovementTargets;

    /**
     * Creates tree options.
     *
     * @param ticks simulation depth in game ticks.
     * @param movementMode movement expansion mode.
     * @param movementRadius movement radius used in {@link SimulationMovementMode#RADIUS}.
     * @param includeWalkActions true to include 1-step-per-tick movement destinations.
     * @param includeRunActions true to include 2-steps-per-tick movement destinations.
     * @param maxNodes hard cap for generated tree nodes.
     * @param maxActionsPerNode hard cap for candidate actions per tree node.
     * @param maxMovementTargets hard cap for movement destinations per node before walk/run variants.
     */
    @Builder(toBuilder = true)
    public SimulationTreeOptions(
            int ticks,
            SimulationMovementMode movementMode,
            int movementRadius,
            boolean includeWalkActions,
            boolean includeRunActions,
            int maxNodes,
            int maxActionsPerNode,
            int maxMovementTargets
    ) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("ticks must be > 0");
        }
        if (movementRadius <= 0) {
            throw new IllegalArgumentException("movementRadius must be > 0");
        }
        if (!includeWalkActions && !includeRunActions) {
            throw new IllegalArgumentException("At least one movement mode must be enabled");
        }
        if (maxNodes <= 1) {
            throw new IllegalArgumentException("maxNodes must be > 1");
        }
        if (maxActionsPerNode <= 0) {
            throw new IllegalArgumentException("maxActionsPerNode must be > 0");
        }
        if (maxMovementTargets <= 0) {
            throw new IllegalArgumentException("maxMovementTargets must be > 0");
        }

        this.ticks = ticks;
        this.movementMode = movementMode == null ? SimulationMovementMode.RADIUS : movementMode;
        this.movementRadius = movementRadius;
        this.includeWalkActions = includeWalkActions;
        this.includeRunActions = includeRunActions;
        this.maxNodes = maxNodes;
        this.maxActionsPerNode = maxActionsPerNode;
        this.maxMovementTargets = maxMovementTargets;
    }

    /**
     * Creates default tree options tuned for deeper future planning.
     *
     * @return default options.
     */
    public static SimulationTreeOptions defaults() {
        return SimulationTreeOptions.builder()
                .ticks(DEFAULT_TICKS)
                .movementMode(SimulationMovementMode.RADIUS)
                .movementRadius(DEFAULT_MOVEMENT_RADIUS)
                .includeWalkActions(true)
                .includeRunActions(true)
                .maxNodes(DEFAULT_MAX_NODES)
                .maxActionsPerNode(DEFAULT_MAX_ACTIONS_PER_NODE)
                .maxMovementTargets(DEFAULT_MAX_MOVEMENT_TARGETS)
                .build();
    }
}