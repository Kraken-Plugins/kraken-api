package com.kraken.api.service.pathfinding;

import org.junit.jupiter.api.Test;
import shortestpath.pathfinder.AbstractNodeKind;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class SearchBudgetTest {
    @Test
    void deadlineIsMonotonicAndDoesNotResetOnProgress() {
        AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 500_000);
        SearchBudget budget = new SearchBudget(1, 100, clock::get);
        budget.beforeAllocation(0);
        clock.addAndGet(999_999); // Includes signed nanoTime wraparound.
        budget.beforeAllocation(1);
        clock.incrementAndGet();
        assertThrows(SearchBudget.Exhausted.class, () -> budget.beforeAllocation(2));
    }

    @Test
    void everyGraphNodeKindRespectsAllocationCeiling() {
        BudgetedNodeGraph graph = new BudgetedNodeGraph(new SearchBudget(1000, 1, () -> 0));
        int start = graph.createStart(0);
        assertThrows(SearchBudget.Exhausted.class, () -> graph.createStart(1));
        assertThrows(SearchBudget.Exhausted.class, () -> graph.createTile(1, start, false));
        assertThrows(SearchBudget.Exhausted.class,
                () -> graph.createTransport(1, start, 1, 0, false, false, 0));
        assertThrows(SearchBudget.Exhausted.class,
                () -> graph.createAbstract(AbstractNodeKind.GLOBAL_TELEPORTS_NORMAL, start, false));
        assertEquals(1, graph.size());
        graph.release();
    }

    @Test
    void interruptionStopsAllocationAndPreservesInterruptStatus() {
        SearchBudget budget = new SearchBudget(1000, 100, () -> 0);
        Thread.currentThread().interrupt();
        try {
            assertTrue(budget.exhausted());
            assertThrows(SearchBudget.Exhausted.class, () -> budget.beforeAllocation(0));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
