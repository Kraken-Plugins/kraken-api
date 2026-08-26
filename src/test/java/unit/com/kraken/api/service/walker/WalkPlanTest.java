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
    void theApproachStopsOnTheTileBeforeTheTransportEntrance() {
        List<WorldPoint> approach = WalkPlan.approach(path(20), 5);

        assertEquals(5, approach.size());
        assertEquals(new WorldPoint(3204, 3200, 0), approach.get(approach.size() - 1));
    }

    @Test
    void anApproachAtTheStartIsEmpty() {
        assertTrue(WalkPlan.approach(path(20), 0).isEmpty());
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

    @Test
    void progressIgnoresPlaneSoAnUpstairsBankStillHasAGap() {
        WorldPoint farm = new WorldPoint(3230, 3298, 0);
        WorldPoint bank = new WorldPoint(3208, 3218, 2);

        assertEquals(farm.distanceTo2D(bank), WalkPlan.progressDistance(farm, bank));
        assertTrue(WalkPlan.progressDistance(farm, bank) < Integer.MAX_VALUE);
        assertTrue(farm.distanceTo(bank) == Integer.MAX_VALUE);
    }

    @Test
    void closerTileStepsTowardAFarWaypointWithoutLandingOnIt() {
        WorldPoint here = new WorldPoint(3241, 3282, 0);
        WorldPoint farBank = new WorldPoint(3221, 3232, 0);

        WorldPoint step = WalkPlan.closerTile(here, farBank, 16);

        assertEquals(here.getPlane(), step.getPlane());
        assertTrue(here.distanceTo(step) <= 16);
        assertTrue(here.distanceTo(step) >= 1);
        assertTrue(step.distanceTo(farBank) < here.distanceTo(farBank));
        assertFalse(step.equals(farBank));
    }

    @Test
    void closerTileDoesNotReturnTheAimEvenWhenTheStepIsLongerThanTheGap() {
        WorldPoint here = new WorldPoint(3200, 3200, 0);
        WorldPoint aim = new WorldPoint(3210, 3200, 0);

        WorldPoint step = WalkPlan.closerTile(here, aim, 50);

        assertEquals(new WorldPoint(3209, 3200, 0), step);
    }

    @Test
    void closerTileIsNullWhenAlreadyAdjacent() {
        WorldPoint here = new WorldPoint(3200, 3200, 0);

        assertNull(WalkPlan.closerTile(here, new WorldPoint(3201, 3200, 0), 16));
        assertNull(WalkPlan.closerTile(here, here, 16));
        assertNull(WalkPlan.closerTile(null, here, 16));
        assertNull(WalkPlan.closerTile(here, here, 0));
    }

    @Test
    void aCompletePathIsFollowable() {
        assertTrue(WalkPlan.isFollowable(true, path(10), new WorldPoint(3209, 3200, 0), 3));
    }

    @Test
    void anIncompleteShorelineIsNotFollowable() {
        WorldPoint shore = new WorldPoint(2993, 3146, 0);
        WorldPoint karamja = new WorldPoint(2956, 3146, 0);

        assertFalse(WalkPlan.isFollowable(false, Collections.singletonList(shore), karamja, 3));
    }

    @Test
    void anIncompleteStubWithinToleranceIsFollowable() {
        WorldPoint last = new WorldPoint(3202, 3200, 0);
        WorldPoint dest = new WorldPoint(3204, 3200, 0);

        assertTrue(WalkPlan.isFollowable(false, Collections.singletonList(last), dest, 3));
    }

    @Test
    void anEmptyPathIsNotFollowable() {
        assertFalse(WalkPlan.isFollowable(false, Collections.emptyList(), new WorldPoint(3200, 3200, 0), 3));
        assertFalse(WalkPlan.isFollowable(true, null, new WorldPoint(3200, 3200, 0), 3));
    }

    @Test
    void noRouteReasonNamesTheClosestApproach() {
        WorldPoint here = new WorldPoint(3006, 3170, 0);
        WorldPoint dest = new WorldPoint(2956, 3146, 0);
        WorldPoint shore = new WorldPoint(2993, 3146, 0);

        assertEquals(
                "no complete route from " + here + " to " + dest
                        + " (closest approach " + shore + ", 37 tiles short)",
                WalkPlan.noRouteReason(here, dest, Collections.singletonList(shore)));
    }

    @Test
    void noRouteReasonWithoutAPathOmitsTheApproach() {
        WorldPoint here = new WorldPoint(3006, 3170, 0);
        WorldPoint dest = new WorldPoint(2956, 3146, 0);

        assertEquals("no route from " + here + " to " + dest, WalkPlan.noRouteReason(here, dest, null));
    }
}
