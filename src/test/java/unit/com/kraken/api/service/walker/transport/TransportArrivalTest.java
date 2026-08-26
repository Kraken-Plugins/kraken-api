package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.TransportArrival;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers deciding whether a transport crossing has happened from the player's tiles.
 *
 * <p>The trap is an adjacent destination while the player has not moved: dataset doors are often one
 * tile apart, and treating that proximity as success is what made a closed door look like a
 * crossing. A walkable tile five tiles through a wall is not an open door: that is how the Varrock
 * underwall skip stalled on the tunnel origin. A walkable jewellery landing uses
 * {@code landingWalkable} so the walker walks instead of teleporting; {@code skipOperating} stays
 * false so it is not counted as already crossed. After a click, {@code arrived} is the wait: an
 * adjacent door that opened underfoot yes, a reachable tile five tiles through a wall no. Walking
 * along this side of a three-tile ditch is also not arrival while the far bank is unreachable.</p>
 */
class TransportArrivalTest {

    private static final WorldPoint ORIGIN = new WorldPoint(3200, 3200, 0);
    private static final WorldPoint NEXT_TILE = new WorldPoint(3201, 3200, 0);
    private static final WorldPoint FAR = new WorldPoint(3220, 3200, 0);
    private static final WorldPoint UPSTAIRS = new WorldPoint(3200, 3200, 1);

    @Test
    void standingStillOnTheOriginIsNotACrossing() {
        assertFalse(TransportArrival.crossed(ORIGIN, ORIGIN, NEXT_TILE));
    }

    @Test
    void leavingTheOriginForAnAdjacentDestinationIsACrossing() {
        assertTrue(TransportArrival.crossed(ORIGIN, NEXT_TILE, NEXT_TILE));
    }

    @Test
    void aPlaneChangeIsACrossing() {
        assertTrue(TransportArrival.crossed(ORIGIN, UPSTAIRS, UPSTAIRS));
        assertTrue(TransportArrival.crossed(ORIGIN, UPSTAIRS, null));
    }

    @Test
    void aLongMoveIsACrossing() {
        assertTrue(TransportArrival.crossed(ORIGIN, FAR, FAR));
        assertTrue(TransportArrival.crossed(ORIGIN, FAR, null));
    }

    @Test
    void aShortStepThatMissesTheDestinationIsNotACrossing() {
        WorldPoint beside = new WorldPoint(3200, 3201, 0);
        WorldPoint farDest = new WorldPoint(3300, 3300, 0);
        assertFalse(TransportArrival.crossed(ORIGIN, beside, farDest));
    }

    @Test
    void aNullNowIsNotACrossing() {
        assertFalse(TransportArrival.crossed(ORIGIN, null, NEXT_TILE));
    }

    @Test
    void aNullBeforeArrivingNearTheDestinationCounts() {
        assertTrue(TransportArrival.crossed(null, NEXT_TILE, NEXT_TILE));
    }

    @Test
    void anAlreadyReachableNeighbourNeedsNoClick() {
        assertTrue(TransportArrival.alreadyOpen(ORIGIN, NEXT_TILE, true));
    }

    @Test
    void aClosedDoorIsNotAlreadyOpen() {
        assertFalse(TransportArrival.alreadyOpen(ORIGIN, NEXT_TILE, false));
    }

    @Test
    void aReachableTileOnAnotherPlaneIsStillATransport() {
        assertFalse(TransportArrival.alreadyOpen(ORIGIN, UPSTAIRS, true));
    }

    @Test
    void aNullTileIsNotAlreadyOpen() {
        assertFalse(TransportArrival.alreadyOpen(null, NEXT_TILE, true));
        assertFalse(TransportArrival.alreadyOpen(ORIGIN, null, true));
    }

    @Test
    void theFarSideOfAWallIsNotAnOpenDoor() {
        WorldPoint underwallOrigin = new WorldPoint(3142, 3513, 0);
        WorldPoint underwallDest = new WorldPoint(3137, 3516, 0);

        assertFalse(TransportArrival.alreadyOpen(underwallOrigin, underwallDest, true));
        assertFalse(TransportArrival.skipOperating(underwallOrigin, underwallDest, true, false));
    }

    @Test
    void anOpenDoorIsSkippedAndAWalkableTeleportLandingIsNot() {
        assertTrue(TransportArrival.skipOperating(ORIGIN, NEXT_TILE, true, false));
        assertFalse(TransportArrival.skipOperating(ORIGIN, NEXT_TILE, true, true));
        assertFalse(TransportArrival.skipOperating(ORIGIN, NEXT_TILE, false, false));
    }

    @Test
    void aWalkableJewelleryLandingIsWalkedNotSkipped() {
        WorldPoint musa = new WorldPoint(2900, 3161, 0);
        WorldPoint glory = new WorldPoint(2918, 3176, 0);

        assertTrue(TransportArrival.landingWalkable(musa, glory, true));
        assertFalse(TransportArrival.alreadyOpen(musa, glory, true));
        assertFalse(TransportArrival.skipOperating(musa, glory, true, true));
    }

    @Test
    void aReachableFarSideIsNotArrivalUntilThePlayerMoves() {
        WorldPoint underwallOrigin = new WorldPoint(3142, 3513, 0);
        WorldPoint underwallDest = new WorldPoint(3137, 3516, 0);

        assertFalse(TransportArrival.arrived(underwallOrigin, underwallOrigin, underwallDest, true));
        assertTrue(TransportArrival.arrived(underwallOrigin, underwallDest, underwallDest, true));
    }

    @Test
    void aDoorThatOpensUnderfootIsArrival() {
        assertTrue(TransportArrival.arrived(ORIGIN, ORIGIN, NEXT_TILE, true));
        assertFalse(TransportArrival.arrived(ORIGIN, ORIGIN, NEXT_TILE, false));
    }

    @Test
    void ditchLandingIsArrivalAndWalkingTheSouthBankIsNot() {
        WorldPoint south = new WorldPoint(3137, 3520, 0);
        WorldPoint alongBank = new WorldPoint(3138, 3520, 0);
        WorldPoint north = new WorldPoint(3137, 3523, 0);

        assertFalse(TransportArrival.arrived(south, south, north, false));
        assertFalse(TransportArrival.arrived(south, alongBank, north, false));
        assertTrue(TransportArrival.arrived(south, north, north, true));
    }
}
