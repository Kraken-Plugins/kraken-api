package com.kraken.api.service.pathfinding;

import com.kraken.api.service.walker.transport.registry.TransportCategory;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the search loop itself over the bundled collision map with a handful of transports a test
 * builds, to cover the rules that decide between transports: the wilderness limit of a teleport
 * and the competition between two kinds of transport that lead to the same place.
 */
class GlobalPathfinderSearchTest {

    private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);
    private static final WorldPoint VARROCK = new WorldPoint(3213, 3424, 0);
    private static final WorldPoint DEEP_WILDERNESS = new WorldPoint(3100, 3700, 0);
    private static final int LEVEL_20_LINE = 3680;
    private static final GlobalPathfinderConfig LIMITS = GlobalPathfinderConfig.builder().build();

    private final GlobalPathfinder pathfinder = new GlobalPathfinder();

    private GlobalPathfinder.SearchState search(WorldPoint from, WorldPoint to, TransportNetwork network) {
        return pathfinder.search(PackedPoint.pack(from), PackedPoint.pack(to), network,
                new CollisionMap(WorldCollisionData.bundled()), LIMITS);
    }

    private static TransportNetwork networkOf(TransportDefinition... definitions) {
        TransportNetwork.Builder network = new TransportNetwork.Builder(false);
        for (TransportDefinition definition : definitions) {
            for (TransportEdge edge : TransportEdge.of(definition)) {
                network.add(edge);
            }
        }
        return network.build();
    }

    private static TransportDefinition tablet(String id, int maxWildernessLevel) {
        return TransportDefinition.itemTeleport(id, VARROCK, 8007)
                .duration(4)
                .maxWildernessLevel(maxWildernessLevel)
                .build();
    }

    private static TransportDefinition landingSite(WorldPoint tile) {
        return TransportDefinition.builder()
                .id("test:landing-site")
                .origin(tile)
                .destination(VARROCK)
                .category(TransportCategory.QUETZAL)
                .objectInfo("Travel Renu 13350")
                .displayInfo("Varrock")
                .duration(4)
                .build();
    }

    private static TransportDefinition whistle() {
        return TransportDefinition.itemTeleport("test:whistle", VARROCK, 29271)
                .category(TransportCategory.QUETZAL_WHISTLE)
                .duration(4)
                .build();
    }

    @Test
    void withNoTransportsTheSearchWalks() {
        GlobalPathfinder.SearchState state = search(LUMBRIDGE, LUMBRIDGE.dx(8), TransportNetwork.EMPTY);

        assertTrue(state.complete);
        assertEquals(9, state.path.length, "eight steps across the open courtyard");
        assertTrue(GlobalPathfinder.findTransportUsages(state.path, TransportNetwork.EMPTY).isEmpty());
    }

    @Test
    void aTeleportIsTakenFromTheFirstTileWhereItsWildernessLimitAllowsIt() {
        TransportNetwork network = networkOf(tablet("test:tablet", 20));

        GlobalPathfinder.SearchState state = search(DEEP_WILDERNESS, VARROCK, network);
        List<GlobalPathfinder.TransportUsage> used = GlobalPathfinder.findTransportUsages(state.path, network);

        assertTrue(state.complete);
        assertEquals(1, used.size());
        assertTrue(used.get(0).isTeleport());
        assertTrue(state.path.length > 2, "the search walks out of the deep wilderness first");
        assertTrue(used.get(0).getOrigin().getY() < LEVEL_20_LINE,
                "the tablet is broken at " + used.get(0).getOrigin() + ", below level 20");
        for (int i = 0; i < state.path.length - 2; i++) {
            assertTrue(PackedPoint.y(state.path[i]) >= LEVEL_20_LINE, "every tile before that is still too deep");
        }
    }

    @Test
    void aTeleportThatWorksDeeperIsTakenStraightAway() {
        TransportNetwork network = networkOf(tablet("test:glory", 30));

        GlobalPathfinder.SearchState state = search(DEEP_WILDERNESS, VARROCK, network);

        assertTrue(state.complete);
        assertEquals(2, state.path.length);
        assertEquals(DEEP_WILDERNESS, GlobalPathfinder.findTransportUsages(state.path, network).get(0).getOrigin());
    }

    @Test
    void aNearbyLandingSiteIsFlownFromInPlaceOfSpendingAWhistleCharge() {
        TransportDefinition site = landingSite(LUMBRIDGE.dx(3));
        TransportNetwork network = networkOf(site, whistle());

        GlobalPathfinder.SearchState state = search(LUMBRIDGE, VARROCK, network);
        List<GlobalPathfinder.TransportUsage> used = GlobalPathfinder.findTransportUsages(state.path, network);

        assertTrue(state.complete);
        assertEquals(1, used.size());
        assertSame(site, used.get(0).getDefinition(), "three tiles of walking beat the whistle's extra cost");
        assertFalse(used.get(0).isTeleport());
        assertEquals(5, state.path.length);
    }

    @Test
    void theWhistleIsBlownWhenTheLandingSiteIsFarEnoughAway() {
        TransportDefinition whistle = whistle();
        TransportNetwork network = networkOf(landingSite(LUMBRIDGE.dx(18)), whistle);

        GlobalPathfinder.SearchState state = search(LUMBRIDGE, VARROCK, network);
        List<GlobalPathfinder.TransportUsage> used = GlobalPathfinder.findTransportUsages(state.path, network);

        assertTrue(state.complete);
        assertEquals(2, state.path.length);
        assertSame(whistle, used.get(0).getDefinition(), "eighteen tiles of walking cost more than the whistle");
    }

    @Test
    void aSearchThatRunsOutOfNodesReturnsTheClosestTileItFound() {
        GlobalPathfinder.SearchState state = pathfinder.search(PackedPoint.pack(LUMBRIDGE), PackedPoint.pack(VARROCK),
                TransportNetwork.EMPTY, new CollisionMap(WorldCollisionData.bundled()),
                GlobalPathfinderConfig.builder().maxSearchNodes(500).build());

        assertFalse(state.complete);
        assertTrue(state.path.length > 1);
        assertTrue(PackedPoint.y(state.path[state.path.length - 1]) > LUMBRIDGE.getY(), "it got closer to Varrock");
    }
}
