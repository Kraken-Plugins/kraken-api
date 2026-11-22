package com.kraken.api.service.pathfinding;

/** Applies the request budget before every allocation, including allocations made while expanding a node. */
final class BudgetedNodeGraph extends NodeGraph {
    private final SearchBudget budget;

    BudgetedNodeGraph(SearchBudget budget) {
        super(256);
        this.budget = budget;
    }

    /** {@inheritDoc} */
    @Override
    int createStart(int packed) {
        budget.beforeAllocation(size());
        return super.createStart(packed);
    }

    /** {@inheritDoc} */
    @Override
    int createTile(int packed, int from) {
        budget.beforeAllocation(size());
        return super.createTile(packed, from);
    }

    /** {@inheritDoc} */
    @Override
    int createTransport(int packed, int from, int duration, int additionalCost, boolean delayedVisit,
                        int differential) {
        budget.beforeAllocation(size());
        return super.createTransport(packed, from, duration, additionalCost, delayedVisit, differential);
    }

    /** {@inheritDoc} */
    @Override
    int createTeleportNode(TeleportBucket teleportBucket, int from) {
        budget.beforeAllocation(size());
        return super.createTeleportNode(teleportBucket, from);
    }
}
