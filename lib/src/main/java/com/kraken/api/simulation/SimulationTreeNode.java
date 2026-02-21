package com.kraken.api.simulation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Node in a simulated outcome tree.
 */
@Getter
public final class SimulationTreeNode {
    private final int id;
    private final int depth;
    private final SimulationAction actionFromParent;
    private final SimulationState state;
    private final SimulationTreeNode parent;
    private final List<SimulationTreeNode> children;

    SimulationTreeNode(
            int id,
            int depth,
            SimulationAction actionFromParent,
            SimulationState state,
            SimulationTreeNode parent
    ) {
        this.id = id;
        this.depth = depth;
        this.actionFromParent = actionFromParent;
        this.state = state;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    /**
     * @return true when this node has no children.
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * @return immutable child list.
     */
    public List<SimulationTreeNode> children() {
        return Collections.unmodifiableList(children);
    }

    void addChild(SimulationTreeNode child) {
        if (child != null) {
            children.add(child);
        }
    }
}
