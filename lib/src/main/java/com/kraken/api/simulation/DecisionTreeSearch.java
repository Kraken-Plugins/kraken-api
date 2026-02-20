package com.kraken.api.simulation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Depth-limited decision tree search utility for simulation outcomes.
 */
public final class DecisionTreeSearch {
    /**
     * Provides candidate actions for a given node state and remaining search depth.
     */
    @FunctionalInterface
    public interface ActionGenerator {
        List<SimulationAction> generate(SimulationState state, int depthRemaining);
    }

    /**
     * Scores a state, where larger values are considered better.
     */
    @FunctionalInterface
    public interface StateEvaluator {
        double evaluate(SimulationState state);
    }

    /**
     * Search output for the root decision.
     */
    @Getter
    @AllArgsConstructor
    public static final class Result {
        /**
         * Best first action selected at root.
         */
        private final SimulationAction bestAction;
        /**
         * Best score discovered from this root action.
         */
        private final double bestScore;
        /**
         * Total expanded node count for this search call.
         */
        private final int exploredNodes;
        /**
         * Player destination after applying best root action for one tick.
         */
        private final WorldPoint bestPlayerWorldPoint;
    }

    private static final class NodeCounter {
        private int value;
    }

    private final SimulationEngine engine;
    private final int maxNodes;

    /**
     * Creates a search instance with default node cap.
     *
     * @param engine simulation engine used for stepping branches.
     */
    public DecisionTreeSearch(SimulationEngine engine) {
        this(engine, 100_000);
    }

    /**
     * Creates a search instance with explicit node cap.
     *
     * @param engine simulation engine used for stepping branches.
     * @param maxNodes hard limit on expanded nodes per search call.
     */
    public DecisionTreeSearch(@NonNull SimulationEngine engine, int maxNodes) {
        if (maxNodes <= 0) {
            throw new IllegalArgumentException("maxNodes must be > 0");
        }
        this.engine = engine;
        this.maxNodes = maxNodes;
    }

    /**
     * Runs depth-limited best-first root action search.
     *
     * @param root root state.
     * @param depth search depth in ticks.
     * @param actionGenerator candidate action provider.
     * @param evaluator state evaluator where larger values are better.
     * @return best root action result.
     */
    public Result search(
            @NonNull SimulationState root,
            int depth,
            @NonNull ActionGenerator actionGenerator,
            @NonNull StateEvaluator evaluator
    ) {
        if (depth <= 0) {
            throw new IllegalArgumentException("depth must be > 0");
        }

        NodeCounter counter = new NodeCounter();
        List<SimulationAction> rootActions = normalizeActions(actionGenerator.generate(root, depth));

        SimulationAction bestAction = SimulationAction.WAIT;
        double bestScore = Double.NEGATIVE_INFINITY;
        WorldPoint bestPlayerPoint = root.getPlayerWorldPoint();

        for (SimulationAction action : rootActions) {
            if (counter.value >= maxNodes) {
                break;
            }

            SimulationState child = root.copy();
            engine.simulateTick(child, action);
            counter.value++;

            double score = depth == 1
                    ? evaluator.evaluate(child)
                    : searchInternal(child, depth - 1, actionGenerator, evaluator, counter);

            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
                bestPlayerPoint = child.getPlayerWorldPoint();
            }
        }

        if (bestScore == Double.NEGATIVE_INFINITY) {
            bestScore = evaluator.evaluate(root);
        }

        return new Result(bestAction, bestScore, counter.value, bestPlayerPoint);
    }

    private double searchInternal(
            SimulationState state,
            int depthRemaining,
            ActionGenerator actionGenerator,
            StateEvaluator evaluator,
            NodeCounter counter
    ) {
        if (depthRemaining <= 0 || counter.value >= maxNodes) {
            return evaluator.evaluate(state);
        }

        List<SimulationAction> actions = normalizeActions(actionGenerator.generate(state, depthRemaining));
        if (actions.isEmpty()) {
            return evaluator.evaluate(state);
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        for (SimulationAction action : actions) {
            if (counter.value >= maxNodes) {
                break;
            }

            SimulationState child = state.copy();
            engine.simulateTick(child, action);
            counter.value++;

            double score = depthRemaining == 1
                    ? evaluator.evaluate(child)
                    : searchInternal(child, depthRemaining - 1, actionGenerator, evaluator, counter);

            if (score > bestScore) {
                bestScore = score;
            }
        }

        if (bestScore == Double.NEGATIVE_INFINITY) {
            return evaluator.evaluate(state);
        }

        return bestScore;
    }

    private List<SimulationAction> normalizeActions(List<SimulationAction> candidateActions) {
        if (candidateActions == null || candidateActions.isEmpty()) {
            return SimulationAction.standardWalkActions();
        }

        List<SimulationAction> actions = new ArrayList<>(candidateActions.size());
        for (SimulationAction action : candidateActions) {
            if (action != null) {
                actions.add(action);
            }
        }

        if (actions.isEmpty()) {
            return Collections.singletonList(SimulationAction.WAIT);
        }
        return actions;
    }
}
