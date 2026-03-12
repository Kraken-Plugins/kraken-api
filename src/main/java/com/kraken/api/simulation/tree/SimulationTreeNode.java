package com.kraken.api.simulation.tree;

import com.kraken.api.simulation.SimulationAction;
import com.kraken.api.simulation.SimulationState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Node in a simulated outcome tree.
 */
@Getter
@AllArgsConstructor
public final class SimulationTreeNode {
    private final int id;
    private final int depth;
    private final SimulationAction actionFromParent;
    private final SimulationState state;
    private final SimulationTreeNode parent;
    private final List<SimulationTreeNode> children = new ArrayList<>();


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

    /**
     * Adds a child node to the current tree node.
     * @param child child node.
     */
    public void addChild(SimulationTreeNode child) {
        if (child != null) {
            children.add(child);
        }
    }
}
