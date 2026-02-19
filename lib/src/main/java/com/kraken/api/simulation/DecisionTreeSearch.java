package com.kraken.api.simulation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Depth-limited decision tree search utility for simulation outcomes.
 */
public final class DecisionTreeSearch {
    @FunctionalInterface
    public interface ActionGenerator {
        List<SimulationAction> generate(SimulationState state, int depthRemaining);
    }

    @FunctionalInterface
    public interface StateEvaluator {
        double evaluate(SimulationState state);
    }

    @Getter
    @AllArgsConstructor
    public static final class Result {
        private final SimulationAction bestAction;
        private final double bestScore;
        private final int exploredNodes;
        private final WorldPoint bestPlayerWorldPoint;
    }

    private static final class NodeCounter {
        private int value;
    }

    private final SimulationEngine engine;
    private final int maxNodes;

    public DecisionTreeSearch(SimulationEngine engine) {
        this(engine, 100_000);
    }

    public DecisionTreeSearch(SimulationEngine engine, int maxNodes) {
        if (engine == null) {
            throw new IllegalArgumentException("engine cannot be null");
        }
        if (maxNodes <= 0) {
            throw new IllegalArgumentException("maxNodes must be > 0");
        }
        this.engine = engine;
        this.maxNodes = maxNodes;
    }

    public Result search(
            SimulationState root,
            int depth,
            ActionGenerator actionGenerator,
            StateEvaluator evaluator
    ) {
        if (root == null) {
            throw new IllegalArgumentException("root cannot be null");
        }
        if (depth <= 0) {
            throw new IllegalArgumentException("depth must be > 0");
        }
        if (actionGenerator == null) {
            throw new IllegalArgumentException("actionGenerator cannot be null");
        }
        if (evaluator == null) {
            throw new IllegalArgumentException("evaluator cannot be null");
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
