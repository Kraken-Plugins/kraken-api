package com.kraken.api.simulation;

import lombok.Getter;

/**
 * Controls simulation tree depth, movement expansion, and node limits.
 */
@Getter
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
        return new SimulationTreeOptions(
                DEFAULT_TICKS,
                SimulationMovementMode.RADIUS,
                DEFAULT_MOVEMENT_RADIUS,
                true,
                true,
                DEFAULT_MAX_NODES,
                DEFAULT_MAX_ACTIONS_PER_NODE,
                DEFAULT_MAX_MOVEMENT_TARGETS
        );
    }

    /**
     * Creates a copy with a new depth.
     *
     * @param ticks depth.
     * @return copied options.
     */
    public SimulationTreeOptions withTicks(int ticks) {
        return new SimulationTreeOptions(
                ticks,
                movementMode,
                movementRadius,
                includeWalkActions,
                includeRunActions,
                maxNodes,
                maxActionsPerNode,
                maxMovementTargets
        );
    }

    /**
     * Creates a copy with a new movement mode.
     *
     * @param movementMode movement mode.
     * @return copied options.
     */
    public SimulationTreeOptions withMovementMode(SimulationMovementMode movementMode) {
        return new SimulationTreeOptions(
                ticks,
                movementMode,
                movementRadius,
                includeWalkActions,
                includeRunActions,
                maxNodes,
                maxActionsPerNode,
                maxMovementTargets
        );
    }

    /**
     * Creates a copy with a new movement radius.
     *
     * @param movementRadius movement radius.
     * @return copied options.
     */
    public SimulationTreeOptions withMovementRadius(int movementRadius) {
        return new SimulationTreeOptions(
                ticks,
                movementMode,
                movementRadius,
                includeWalkActions,
                includeRunActions,
                maxNodes,
                maxActionsPerNode,
                maxMovementTargets
        );
    }

    /**
     * Creates a copy with walk/run movement toggles.
     *
     * @param includeWalkActions include walk destinations.
     * @param includeRunActions include run destinations.
     * @return copied options.
     */
    public SimulationTreeOptions withMovementTypes(boolean includeWalkActions, boolean includeRunActions) {
        return new SimulationTreeOptions(
                ticks,
                movementMode,
                movementRadius,
                includeWalkActions,
                includeRunActions,
                maxNodes,
                maxActionsPerNode,
                maxMovementTargets
        );
    }

    /**
     * Creates a copy with a new node cap.
     *
     * @param maxNodes max nodes.
     * @return copied options.
     */
    public SimulationTreeOptions withMaxNodes(int maxNodes) {
        return new SimulationTreeOptions(
                ticks,
                movementMode,
                movementRadius,
                includeWalkActions,
                includeRunActions,
                maxNodes,
                maxActionsPerNode,
                maxMovementTargets
        );
    }

    /**
     * Creates a copy with action and movement target caps.
     *
     * @param maxActionsPerNode action cap per node.
     * @param maxMovementTargets movement destination cap per node.
     * @return copied options.
     */
    public SimulationTreeOptions withActionCaps(int maxActionsPerNode, int maxMovementTargets) {
        return new SimulationTreeOptions(
                ticks,
                movementMode,
                movementRadius,
                includeWalkActions,
                includeRunActions,
                maxNodes,
                maxActionsPerNode,
                maxMovementTargets
        );
    }
}
