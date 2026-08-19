package unit.com.kraken.api.service.walker;

import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.walker.WalkPlan;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import shortestpath.transport.TransportType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the route arithmetic behind the walk loop.
 */
class WalkPlanTest {

    private static GlobalPathfinder.TransportUsage usage(int pathIndex) {
        return new GlobalPathfinder.TransportUsage(
                pathIndex,
                new WorldPoint(3200, 3200, 0),
                new WorldPoint(3300, 3300, 0),
                TransportType.TRANSPORT,
                "display",
                "Open Door 9398",
                false,
                1);
    }

    private static List<WorldPoint> path(int length) {
        List<WorldPoint> path = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            path.add(new WorldPoint(3200 + i, 3200, 0));
        }
        return path;
    }

    @Test
    void noTransportsYieldsNothing() {
        assertNull(WalkPlan.firstTransport(null));
        assertNull(WalkPlan.firstTransport(Collections.emptyList()));
    }

    @Test
    void theEarliestTransportIsChosenRegardlessOfListOrder() {
        List<GlobalPathfinder.TransportUsage> transports = Arrays.asList(usage(40), usage(7), usage(19));

        assertEquals(7, WalkPlan.firstTransport(transports).getPathIndex());
    }

    @Test
    void nullEntriesAreSkipped() {
        List<GlobalPathfinder.TransportUsage> transports = Arrays.asList(null, usage(12), null);

        assertEquals(12, WalkPlan.firstTransport(transports).getPathIndex());
    }

    @Test
    void theApproachIncludesTheTransportEntrance() {
        List<WorldPoint> approach = WalkPlan.approach(path(20), 5);

        assertEquals(6, approach.size());
        assertEquals(new WorldPoint(3205, 3200, 0), approach.get(approach.size() - 1));
    }

    @Test
    void anApproachAtTheStartIsJustTheFirstTile() {
        assertEquals(1, WalkPlan.approach(path(20), 0).size());
    }

    @Test
    void anApproachPastTheEndIsClamped() {
        List<WorldPoint> path = path(5);

        assertEquals(5, WalkPlan.approach(path, 99).size());
    }

    @Test
    void anEmptyOrNullPathYieldsNothing() {
        assertTrue(WalkPlan.approach(null, 3).isEmpty());
        assertTrue(WalkPlan.approach(Collections.emptyList(), 3).isEmpty());
        assertTrue(WalkPlan.approach(path(5), -1).isEmpty());
    }

    @Test
    void theFirstRoundAlwaysCountsAsProgress() {
        assertTrue(WalkPlan.madeProgress(Integer.MAX_VALUE, 500, 3));
    }

    @Test
    void closingTheGapCountsAsProgress() {
        assertTrue(WalkPlan.madeProgress(100, 90, 3));
        assertTrue(WalkPlan.madeProgress(100, 97, 3));
    }

    @Test
    void barelyMovingDoesNotCountAsProgress() {
        assertFalse(WalkPlan.madeProgress(100, 98, 3));
        assertFalse(WalkPlan.madeProgress(100, 100, 3));
        assertFalse(WalkPlan.madeProgress(100, 120, 3));
    }
}
