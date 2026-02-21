package com.kraken.api.simulation;

import com.kraken.api.simulation.tree.SimulationTree;
import com.kraken.api.simulation.tree.SimulationTreeNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.coords.WorldPoint;

/**
 * Searches a generated simulation tree and returns the best root decision.
 */
public final class DecisionTreeSearch {
    /**
     * Scores a tree node; larger is better.
     */
    @FunctionalInterface
    public interface NodeEvaluator {
        /**
         * @param node tree node.
         * @return node score.
         */
        double evaluate(SimulationTreeNode node);
    }

    /**
     * Search output for the root decision.
     */
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

    /**
     * Searches a generated tree.
     *
     * @param tree generated tree.
     * @param evaluator node evaluator.
     * @return best root action result.
     */
    public Result search(@NonNull SimulationTree tree, @NonNull NodeEvaluator evaluator) {
        SimulationTreeNode root = tree.getRoot();
        NodeCounter counter = new NodeCounter();

        if (root.children().isEmpty()) {
            counter.value = 1;
            double score = evaluator.evaluate(root);
            return new Result(
                    SimulationAction.WAIT,
                    score,
                    counter.value,
                    root.getState().getPlayerWorldPoint()
            );
        }

        SimulationAction bestAction = SimulationAction.WAIT;
        double bestScore = Double.NEGATIVE_INFINITY;
        WorldPoint bestPoint = root.getState().getPlayerWorldPoint();

        for (SimulationTreeNode child : root.children()) {
            double branchScore = bestSubtreeScore(child, evaluator, counter);
            if (branchScore > bestScore) {
                bestScore = branchScore;
                bestAction = child.getActionFromParent();
                bestPoint = child.getState().getPlayerWorldPoint();
            }
        }

        if (bestScore == Double.NEGATIVE_INFINITY) {
            bestScore = evaluator.evaluate(root);
        }

        return new Result(bestAction, bestScore, counter.value, bestPoint);
    }

    private double bestSubtreeScore(SimulationTreeNode node, NodeEvaluator evaluator, NodeCounter counter) {
        counter.value++;
        double best = evaluator.evaluate(node);
        for (SimulationTreeNode child : node.children()) {
            best = Math.max(best, bestSubtreeScore(child, evaluator, counter));
        }
        return best;
    }
}
