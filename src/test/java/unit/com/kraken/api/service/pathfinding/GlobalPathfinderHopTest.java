package com.kraken.api.service.pathfinding;

import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers recovering the transport a route step took from two consecutive path tiles, teleports
 * usable from anywhere included.
 */
class GlobalPathfinderHopTest {

    private static final WorldPoint AL_KHARID = new WorldPoint(3263, 3227, 0);
    private static final WorldPoint GRAND_EXCHANGE = new WorldPoint(3162, 3480, 0);

    private static final TransportEdge RING = TransportEdge.of(
            TransportDefinition.itemTeleport("test:ring-of-wealth", GRAND_EXCHANGE, 2572).build())[0];
    private static final TransportEdge LADDER = TransportEdge.of(TransportDefinition
            .object("test:ladder", AL_KHARID, new WorldPoint(3263, 3227, 1), 16683, "Climb-up").build())[0];

    @Test
    void adjacentTilesOnTheSamePlaneAreWalking() {
        int here = PackedPoint.pack(AL_KHARID);

        assertTrue(GlobalPathfinder.isWalkNeighbour(here, here));
        assertTrue(GlobalPathfinder.isWalkNeighbour(here, PackedPoint.pack(3264, 3227, 0)));
        assertTrue(GlobalPathfinder.isWalkNeighbour(here, PackedPoint.pack(3264, 3228, 0)));
    }

    @Test
    void aJewelleryLandingAndATileOnAnotherPlaneAreNotWalkingSteps() {
        int here = PackedPoint.pack(AL_KHARID);

        assertFalse(GlobalPathfinder.isWalkNeighbour(here, PackedPoint.pack(GRAND_EXCHANGE)));
        assertFalse(GlobalPathfinder.isWalkNeighbour(here, PackedPoint.pack(3263, 3227, 1)));
    }

    @Test
    void aJumpMatchesATeleportUsableFromAnywhere() {
        TransportEdge hop = GlobalPathfinder.hopBetween(
                TransportNetwork.NO_EDGES, new TransportEdge[]{RING}, PackedPoint.pack(AL_KHARID), RING.destination);

        assertSame(RING, hop);
    }

    @Test
    void walkingOntoATeleportLandingIsNotRubbingTheRing() {
        int beside = PackedPoint.pack(GRAND_EXCHANGE.dx(1));

        assertNull(GlobalPathfinder.hopBetween(
                TransportNetwork.NO_EDGES, new TransportEdge[]{RING}, beside, RING.destination));
    }

    @Test
    void anEdgeThatLeavesTheTileIsPreferredOnAJump() {
        TransportEdge hop = GlobalPathfinder.hopBetween(
                new TransportEdge[]{LADDER}, new TransportEdge[]{RING}, LADDER.origin, LADDER.destination);

        assertSame(LADDER, hop);
    }

    @Test
    void theFirstOfTwoEdgesToTheSameTileIsTheOneReported() {
        TransportEdge other = TransportEdge.of(TransportDefinition
                .object("test:other-ladder", AL_KHARID, new WorldPoint(3263, 3227, 1), 16684, "Climb").build())[0];

        assertSame(other, GlobalPathfinder.hopBetween(
                new TransportEdge[]{other, LADDER}, TransportNetwork.NO_EDGES, LADDER.origin, LADDER.destination));
    }

    @Test
    void aBidirectionalDefinitionGivesAnEdgeEachWay() {
        TransportEdge[] edges = TransportEdge.of(TransportDefinition
                .object("test:gate", AL_KHARID, AL_KHARID.dx(1), 44598, "Open").bidirectional(true).build());

        assertTrue(edges.length == 2 && edges[0].origin == edges[1].destination && edges[0].destination == edges[1].origin);
        assertFalse(edges[0].isTeleport());
        assertTrue(RING.isTeleport());
    }
}
