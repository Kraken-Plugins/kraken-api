package unit.com.kraken.api.service.walker;

import com.kraken.api.service.walker.ObstacleRecovery;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers deciding what counts as an obstacle and how to get through it.
 *
 * <p>These are the parts of door recovery that can be checked without a client. Finding the blocking
 * tile and clicking the door both need live scene data, but choosing wrongly here is what would send
 * the walker clicking at the wrong scenery, so it is worth pinning down.</p>
 */
class ObstacleRecoveryTest {

    @Test
    void doorsGatesAndCurtainsAreObstacles() {
        assertTrue(ObstacleRecovery.isObstacle("Door"));
        assertTrue(ObstacleRecovery.isObstacle("Gate"));
        assertTrue(ObstacleRecovery.isObstacle("Curtain"));
    }

    @Test
    void theNameMatchIsCaseInsensitiveAndPartial() {
        assertTrue(ObstacleRecovery.isObstacle("Large door"));
        assertTrue(ObstacleRecovery.isObstacle("METAL GATE"));
        assertTrue(ObstacleRecovery.isObstacle("Odd-looking wall curtain"));
    }

    @Test
    void otherSceneryIsNotAnObstacle() {
        assertFalse(ObstacleRecovery.isObstacle("Bank booth"));
        assertFalse(ObstacleRecovery.isObstacle("Oak tree"));
        assertFalse(ObstacleRecovery.isObstacle("Staircase"));
        assertFalse(ObstacleRecovery.isObstacle(null));
    }

    @Test
    void openIsPreferredWhenOffered() {
        assertEquals("Open", ObstacleRecovery.chooseAction(new String[]{"Open", "Examine"}));
    }

    @Test
    void openWinsOverClimbingTheGate() {
        assertEquals("Open", ObstacleRecovery.chooseAction(new String[]{"Climb-over", "Open"}));
    }

    @Test
    void alternativeWaysThroughAreAccepted() {
        assertEquals("Pass", ObstacleRecovery.chooseAction(new String[]{"Examine", "Pass"}));
        assertEquals("Squeeze-through", ObstacleRecovery.chooseAction(new String[]{"Squeeze-through"}));
        assertEquals("Go-through", ObstacleRecovery.chooseAction(new String[]{"Go-through", "Examine"}));
        assertEquals("Pay-toll(10gp)", ObstacleRecovery.chooseAction(new String[]{"Pay-toll(10gp)", "Examine"}));
    }

    @Test
    void theClientsSpellingIsReturnedNotTheCandidates() {
        assertEquals("open", ObstacleRecovery.chooseAction(new String[]{"open"}));
    }

    @Test
    void nullEntriesAreSkipped() {
        assertEquals("Open", ObstacleRecovery.chooseAction(new String[]{null, "Open", null}));
    }

    @Test
    void somethingWithNoWayThroughIsReported() {
        assertNull(ObstacleRecovery.chooseAction(new String[]{"Pick-lock", "Examine"}));
        assertNull(ObstacleRecovery.chooseAction(new String[]{}));
        assertNull(ObstacleRecovery.chooseAction(null));
    }

    @Test
    void aDoorNextToThePlayerIsInRange() {
        WorldPoint here = new WorldPoint(3200, 3200, 0);
        WorldPoint door = new WorldPoint(3201, 3200, 0);

        assertTrue(ObstacleRecovery.isWithinInteractRange(here, door, here));
    }

    @Test
    void standingBesideTheNearSideOfADoorwayCounts() {
        WorldPoint here = new WorldPoint(3200, 3200, 0);
        WorldPoint nearSide = new WorldPoint(3201, 3200, 0);
        WorldPoint blocked = new WorldPoint(3202, 3200, 0);

        assertTrue(ObstacleRecovery.isWithinInteractRange(here, blocked, nearSide));
    }

    @Test
    void aDoorFourTilesAwayIsInClickRangeButNotInteractRange() {
        WorldPoint here = new WorldPoint(3253, 3434, 0);
        WorldPoint door = new WorldPoint(3253, 3430, 0);

        assertTrue(ObstacleRecovery.isWithinClickRange(here, door, here));
        assertFalse(ObstacleRecovery.isWithinInteractRange(here, door, here));
    }

    @Test
    void aDoorElevenTilesAwayIsOutOfClickRange() {
        WorldPoint here = new WorldPoint(3200, 3200, 0);
        WorldPoint door = new WorldPoint(3211, 3200, 0);

        assertFalse(ObstacleRecovery.isWithinClickRange(here, door, here));
        assertFalse(ObstacleRecovery.isWithinInteractRange(here, door, here));
    }

    @Test
    void aCompressedJumpToAFarTileIsOutOfClickRange() {
        WorldPoint here = new WorldPoint(3241, 3282, 0);
        WorldPoint farBank = new WorldPoint(3221, 3232, 0);

        assertFalse(ObstacleRecovery.isWithinClickRange(here, farBank, here));
        assertFalse(ObstacleRecovery.isWithinInteractRange(here, farBank, here));
        assertFalse(ObstacleRecovery.isWithinInteractRange(here, Arrays.asList(here, farBank), 1));
        assertFalse(ObstacleRecovery.isWithinInteractRange(here, Arrays.asList(farBank), 0));
    }

    @Test
    void openingFromAFewTilesAwayWaitsLonger() {
        WorldPoint here = new WorldPoint(3253, 3434, 0);
        WorldPoint door = new WorldPoint(3253, 3430, 0);

        assertTrue(ObstacleRecovery.openTimeoutMs(here, door) > ObstacleRecovery.openTimeoutMs(here, here));
    }

    @Test
    void missingArgumentsAreOutOfRange() {
        WorldPoint here = new WorldPoint(3200, 3200, 0);

        assertFalse(ObstacleRecovery.isWithinInteractRange(null, here, here));
        assertFalse(ObstacleRecovery.isWithinInteractRange(here, (WorldPoint) null, null));
        assertFalse(ObstacleRecovery.isWithinInteractRange(here, Arrays.asList(here), 1));
        assertFalse(ObstacleRecovery.isWithinInteractRange(here, Arrays.asList(), 0));
    }
}
