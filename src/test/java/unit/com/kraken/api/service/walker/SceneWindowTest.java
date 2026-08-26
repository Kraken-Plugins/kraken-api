package unit.com.kraken.api.service.walker;

import com.kraken.api.service.walker.SceneWindow;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the scene clipping that lets a short range movement primitive walk a long route.
 */
class SceneWindowTest {

    private static final int BASE_X = 3200;
    private static final int BASE_Y = 3400;
    private static final int MARGIN = SceneWindow.DEFAULT_MARGIN;

    @Test
    void aNullRouteYieldsAnEmptyLeg() {
        assertTrue(SceneWindow.clip(null, BASE_X, BASE_Y, 0, MARGIN).isEmpty());
    }

    @Test
    void anEmptyRouteYieldsAnEmptyLeg() {
        assertTrue(SceneWindow.clip(Collections.emptyList(), BASE_X, BASE_Y, 0, MARGIN).isEmpty());
    }

    @Test
    void aRouteEntirelyInsideTheSceneIsReturnedWhole() {
        List<WorldPoint> route = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            route.add(new WorldPoint(BASE_X + 20 + i, BASE_Y + 20 + i, 0));
        }

        assertEquals(route, SceneWindow.clip(route, BASE_X, BASE_Y, 0, MARGIN));
    }

    @Test
    void aRouteIsTruncatedWhereItLeavesTheScene() {
        WorldPoint inside = new WorldPoint(BASE_X + 20, BASE_Y + 20, 0);
        WorldPoint alsoInside = new WorldPoint(BASE_X + 21, BASE_Y + 21, 0);
        WorldPoint outside = new WorldPoint(BASE_X + SceneWindow.SCENE_SIZE + 50, BASE_Y + 21, 0);

        List<WorldPoint> leg = SceneWindow.clip(Arrays.asList(inside, alsoInside, outside), BASE_X, BASE_Y, 0, MARGIN);

        assertEquals(Arrays.asList(inside, alsoInside), leg);
    }

    @Test
    void aRouteThatLeavesAndReturnsIsNotStitchedBackTogether() {
        WorldPoint inside = new WorldPoint(BASE_X + 20, BASE_Y + 20, 0);
        WorldPoint outside = new WorldPoint(BASE_X + SceneWindow.SCENE_SIZE + 50, BASE_Y + 20, 0);
        WorldPoint insideAgain = new WorldPoint(BASE_X + 22, BASE_Y + 20, 0);

        List<WorldPoint> leg = SceneWindow.clip(Arrays.asList(inside, outside, insideAgain), BASE_X, BASE_Y, 0, MARGIN);

        assertEquals(Collections.singletonList(inside), leg);
    }

    @Test
    void aPlaneChangeEndsTheLeg() {
        WorldPoint groundFloor = new WorldPoint(BASE_X + 20, BASE_Y + 20, 0);
        WorldPoint upstairs = new WorldPoint(BASE_X + 21, BASE_Y + 20, 1);

        List<WorldPoint> leg = SceneWindow.clip(Arrays.asList(groundFloor, upstairs), BASE_X, BASE_Y, 0, MARGIN);

        assertEquals(Collections.singletonList(groundFloor), leg);
    }

    @Test
    void theMarginTrimsBothEdges() {
        WorldPoint justInsideTheMargin = new WorldPoint(BASE_X + MARGIN, BASE_Y + MARGIN, 0);
        WorldPoint justOutsideTheMargin = new WorldPoint(BASE_X + MARGIN - 1, BASE_Y + MARGIN, 0);

        assertEquals(1, SceneWindow.clip(Collections.singletonList(justInsideTheMargin), BASE_X, BASE_Y, 0, MARGIN).size());
        assertTrue(SceneWindow.clip(Collections.singletonList(justOutsideTheMargin), BASE_X, BASE_Y, 0, MARGIN).isEmpty());
    }

    @Test
    void theFarEdgeIsTrimmedToo() {
        int lastUsable = BASE_X + SceneWindow.SCENE_SIZE - 1 - MARGIN;
        WorldPoint onTheEdge = new WorldPoint(lastUsable, BASE_Y + 20, 0);
        WorldPoint pastTheEdge = new WorldPoint(lastUsable + 1, BASE_Y + 20, 0);

        assertEquals(1, SceneWindow.clip(Collections.singletonList(onTheEdge), BASE_X, BASE_Y, 0, MARGIN).size());
        assertTrue(SceneWindow.clip(Collections.singletonList(pastTheEdge), BASE_X, BASE_Y, 0, MARGIN).isEmpty());
    }

    @Test
    void aRouteThatStartsOutsideYieldsNothing() {
        WorldPoint outside = new WorldPoint(BASE_X - 500, BASE_Y, 0);
        WorldPoint inside = new WorldPoint(BASE_X + 20, BASE_Y + 20, 0);

        assertTrue(SceneWindow.clip(Arrays.asList(outside, inside), BASE_X, BASE_Y, 0, MARGIN).isEmpty());
    }

    @Test
    void towardATileSouthOfTheSceneLandsOnTheSouthEdge() {
        WorldPoint here = new WorldPoint(BASE_X + 50, BASE_Y + 50, 0);
        WorldPoint stairs = new WorldPoint(BASE_X + 40, BASE_Y - 80, 0);
        int southEdge = BASE_Y + MARGIN;

        WorldPoint edge = SceneWindow.toward(here, stairs, BASE_X, BASE_Y, MARGIN);

        assertEquals(new WorldPoint(BASE_X + 40, southEdge, 0), edge);
    }

    @Test
    void towardKeepsThePlayersPlane() {
        WorldPoint here = new WorldPoint(BASE_X + 50, BASE_Y + 50, 0);
        WorldPoint upstairs = new WorldPoint(BASE_X + 40, BASE_Y - 80, 2);

        WorldPoint edge = SceneWindow.toward(here, upstairs, BASE_X, BASE_Y, MARGIN);

        assertEquals(0, edge.getPlane());
    }

    @Test
    void towardReturnsNothingWhenAlreadyOnThatEdge() {
        int southEdge = BASE_Y + MARGIN;
        WorldPoint here = new WorldPoint(BASE_X + 40, southEdge, 0);
        WorldPoint stairs = new WorldPoint(BASE_X + 40, BASE_Y - 80, 0);

        assertEquals(null, SceneWindow.toward(here, stairs, BASE_X, BASE_Y, MARGIN));
    }

    @Test
    void towardReturnsNothingWhenTheNextTileIsTheSameSpotOnAnotherPlane() {
        WorldPoint here = new WorldPoint(3205, 3209, 1);
        WorldPoint upstairs = new WorldPoint(3205, 3209, 2);

        assertEquals(null, SceneWindow.toward(here, upstairs, 3136, 3136, MARGIN));
    }

    @Test
    void towardReturnsNothingWhenTheTargetIsMissing() {
        WorldPoint here = new WorldPoint(BASE_X + 50, BASE_Y + 50, 0);

        assertEquals(null, SceneWindow.toward(here, null, BASE_X, BASE_Y, MARGIN));
    }
}
