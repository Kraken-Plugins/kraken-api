package unit.com.kraken.api.service.pathfinding;

import com.kraken.api.service.pathfinding.GlobalPathfinder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shortestpath.WorldPointUtil;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportLoader;
import shortestpath.transport.TransportType;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers recovering player-centered teleports (jewellery, tabs) from consecutive path tiles.
 */
class GlobalPathfinderHopTest {

    private static Transport itemTeleport;
    private static Transport tileTransport;

    @BeforeAll
    static void loadSampleEdges() {
        for (Set<Transport> set : TransportLoader.loadAllFromResources().values()) {
            for (Transport transport : set) {
                if (itemTeleport == null
                        && transport.getType() == TransportType.TELEPORTATION_ITEM
                        && transport.getOrigin() == Transport.UNDEFINED_ORIGIN) {
                    itemTeleport = transport;
                }
                if (tileTransport == null
                        && transport.getOrigin() != Transport.UNDEFINED_ORIGIN
                        && transport.getDestination() != Transport.UNDEFINED_DESTINATION) {
                    tileTransport = transport;
                }
            }
        }
    }

    @Test
    void adjacentTilesOnTheSamePlaneAreWalking() {
        int here = WorldPointUtil.packWorldPoint(3263, 3227, 0);

        assertTrue(GlobalPathfinder.isWalkNeighbour(here, here));
        assertTrue(GlobalPathfinder.isWalkNeighbour(
                here, WorldPointUtil.packWorldPoint(3264, 3227, 0)));
        assertTrue(GlobalPathfinder.isWalkNeighbour(
                here, WorldPointUtil.packWorldPoint(3264, 3228, 0)));
    }

    @Test
    void aJewelleryLandingIsNotAWalkingStep() {
        int here = WorldPointUtil.packWorldPoint(3263, 3227, 0);
        int ge = WorldPointUtil.packWorldPoint(3162, 3480, 0);

        assertFalse(GlobalPathfinder.isWalkNeighbour(here, ge));
    }

    @Test
    void aJumpMatchesAPlayerCenteredTeleport() {
        assertNotNull(itemTeleport, "dataset should include an inventory teleport");

        int here = WorldPointUtil.packWorldPoint(3263, 3227, 0);
        Transport hop = GlobalPathfinder.hopBetween(
                new Transport[0], new Transport[]{itemTeleport}, here, itemTeleport.getDestination());

        assertEquals(itemTeleport, hop);
    }

    @Test
    void walkingOntoATeleportLandingIsNotRubbingTheRing() {
        assertNotNull(itemTeleport);

        int landing = itemTeleport.getDestination();
        int beside = WorldPointUtil.packWorldPoint(
                WorldPointUtil.unpackWorldX(landing) + 1,
                WorldPointUtil.unpackWorldY(landing),
                WorldPointUtil.unpackWorldPlane(landing));

        assertNull(GlobalPathfinder.hopBetween(
                new Transport[0], new Transport[]{itemTeleport}, beside, landing));
    }

    @Test
    void aTileOriginEdgeIsPreferredOnAJump() {
        assertNotNull(tileTransport);

        Transport hop = GlobalPathfinder.hopBetween(
                new Transport[]{tileTransport},
                new Transport[]{itemTeleport},
                tileTransport.getOrigin(),
                tileTransport.getDestination());

        assertEquals(tileTransport, hop);
    }
}
