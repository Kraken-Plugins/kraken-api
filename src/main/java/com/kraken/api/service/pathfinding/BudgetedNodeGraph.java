package com.kraken.api.service.pathfinding;

import shortestpath.pathfinder.AbstractNodeKind;
import shortestpath.pathfinder.NodeGraph;

/** Applies the request budget before every allocation, including allocations inside getNeighbors. */
final class BudgetedNodeGraph extends NodeGraph {
    private final SearchBudget budget;

    BudgetedNodeGraph(SearchBudget budget) {
        super(256);
        this.budget = budget;
    }

    /** {@inheritDoc} */
    @Override
    public int createStart(int position) {
        budget.beforeAllocation(size());
        return super.createStart(position);
    }

    /** {@inheritDoc} */
    @Override
    public int createTile(int position, int previous, boolean bankVisited) {
        budget.beforeAllocation(size());
        return super.createTile(position, previous, bankVisited);
    }

    /** {@inheritDoc} */
    @Override
    public int createTransport(int position, int previous, int cost, int differentialCost,
                               boolean bankVisited, boolean delayedVisit, int duration) {
        budget.beforeAllocation(size());
        return super.createTransport(position, previous, cost, differentialCost, bankVisited, delayedVisit, duration);
    }

    /** {@inheritDoc} */
    @Override
    public int createAbstract(AbstractNodeKind kind, int previous, boolean bankVisited) {
        budget.beforeAllocation(size());
        return super.createAbstract(kind, previous, bankVisited);
    }
}
