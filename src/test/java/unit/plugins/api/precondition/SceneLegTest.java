package unit.plugins.api.precondition;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import plugins.api.precondition.SuiteWalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the scene clipping that lets a short range movement primitive walk a long route.
 *
 * <p>This is the one genuinely tricky part of the walker that can be tested without a client: the
 * loop around it is all blocking client calls, but deciding how much of a route is currently walkable
 * is pure arithmetic.</p>
 */
class SceneLegTest {

    /** Matches SuiteWalker: the scene is 104 tiles square. */
    private static final int BASE_X = 3200;
    private static final int BASE_Y = 3400;
    private static final int MARGIN = 8;

    @Test
    void anEmptyRouteYieldsAnEmptyLeg() {
        assertTrue(SuiteWalker.sceneLeg(Collections.emptyList(), BASE_X, BASE_Y, 0, MARGIN).isEmpty());
    }

    @Test
    void aNullRouteIsHandled() {
        assertTrue(SuiteWalker.sceneLeg(null, BASE_X, BASE_Y, 0, MARGIN).isEmpty());
    }

    @Test
    void aRouteEntirelyInsideTheSceneIsReturnedWhole() {
        List<WorldPoint> route = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            route.add(new WorldPoint(BASE_X + 20 + i, BASE_Y + 20 + i, 0));
        }

        assertEquals(route, SuiteWalker.sceneLeg(route, BASE_X, BASE_Y, 0, MARGIN));
    }

    @Test
    void aRouteIsTruncatedWhereItLeavesTheScene() {
        List<WorldPoint> route = new ArrayList<>();
        route.add(new WorldPoint(BASE_X + 20, BASE_Y + 20, 0));
        route.add(new WorldPoint(BASE_X + 40, BASE_Y + 20, 0));
        // Beyond baseX + 103 - margin, so outside the usable window.
        route.add(new WorldPoint(BASE_X + 200, BASE_Y + 20, 0));
        route.add(new WorldPoint(BASE_X + 300, BASE_Y + 20, 0));

        List<WorldPoint> leg = SuiteWalker.sceneLeg(route, BASE_X, BASE_Y, 0, MARGIN);

        assertEquals(2, leg.size());
        assertEquals(route.get(1), leg.get(1));
    }

    @Test
    void truncationStopsAtTheFirstOutsidePointEvenIfLaterOnesReturn() {
        // A route that leaves and comes back must not be stitched together: the tiles in between are
        // not clickable, so walking the later section would teleport the click across the gap.
        List<WorldPoint> route = new ArrayList<>();
        route.add(new WorldPoint(BASE_X + 20, BASE_Y + 20, 0));
        route.add(new WorldPoint(BASE_X + 500, BASE_Y + 20, 0));
        route.add(new WorldPoint(BASE_X + 25, BASE_Y + 20, 0));

        List<WorldPoint> leg = SuiteWalker.sceneLeg(route, BASE_X, BASE_Y, 0, MARGIN);

        assertEquals(1, leg.size());
    }

    @Test
    void aPlaneChangeEndsTheLeg() {
        // Stairs and ladders are transports the walker does not operate, so the leg must stop at the
        // point the route changes level rather than trying to click a tile on another floor.
        List<WorldPoint> route = new ArrayList<>();
        route.add(new WorldPoint(BASE_X + 20, BASE_Y + 20, 0));
        route.add(new WorldPoint(BASE_X + 21, BASE_Y + 20, 0));
        route.add(new WorldPoint(BASE_X + 22, BASE_Y + 20, 1));

        List<WorldPoint> leg = SuiteWalker.sceneLeg(route, BASE_X, BASE_Y, 0, MARGIN);

        assertEquals(2, leg.size());
    }

    @Test
    void theMarginExcludesTilesAtTheSceneEdge() {
        // The outer ring of the scene is routinely unwalkable or unrendered; clicking it produces a
        // stall rather than movement.
        WorldPoint justInsideRawBounds = new WorldPoint(BASE_X + 1, BASE_Y + 1, 0);
        List<WorldPoint> route = Collections.singletonList(justInsideRawBounds);

        assertTrue(SuiteWalker.sceneLeg(route, BASE_X, BASE_Y, 0, MARGIN).isEmpty());
        assertEquals(1, SuiteWalker.sceneLeg(route, BASE_X, BASE_Y, 0, 0).size());
    }

    @Test
    void aRouteStartingOutsideTheSceneYieldsNothing() {
        List<WorldPoint> route = Collections.singletonList(new WorldPoint(BASE_X + 900, BASE_Y, 0));

        assertTrue(SuiteWalker.sceneLeg(route, BASE_X, BASE_Y, 0, MARGIN).isEmpty());
    }

    @Test
    void theUsableWindowSpansTheSceneMinusBothMargins() {
        int lowest = BASE_X + MARGIN;
        int highest = BASE_X + 104 - 1 - MARGIN;

        List<WorldPoint> route = new ArrayList<>();
        route.add(new WorldPoint(lowest, BASE_Y + MARGIN, 0));
        route.add(new WorldPoint(highest, BASE_Y + MARGIN, 0));

        assertEquals(2, SuiteWalker.sceneLeg(route, BASE_X, BASE_Y, 0, MARGIN).size());

        List<WorldPoint> tooFar =
                Collections.singletonList(new WorldPoint(highest + 1, BASE_Y + MARGIN, 0));
        assertTrue(SuiteWalker.sceneLeg(tooFar, BASE_X, BASE_Y, 0, MARGIN).isEmpty());
    }
}
