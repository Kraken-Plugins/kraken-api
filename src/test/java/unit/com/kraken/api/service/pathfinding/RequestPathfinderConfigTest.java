package com.kraken.api.service.pathfinding;

import net.runelite.api.Client;
import org.junit.jupiter.api.Test;
import shortestpath.ShortestPathConfig;
import shortestpath.WorldPointUtil;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Covers how a search's configuration offers teleports it was handed beside the planner's own.
 */
class RequestPathfinderConfigTest {

    @Test
    void addedTeleportsComeAheadOfTheOnesAlreadyThere() {
        Transport added = teleportTo(3200, 3200);
        Transport dataset = teleportTo(3300, 3300);

        Transport[] combined = RequestPathfinderConfig.combine(new Transport[]{added}, new Transport[]{dataset});

        assertArrayEquals(new Transport[]{added, dataset}, combined);
        assertArrayEquals(new Transport[]{added}, RequestPathfinderConfig.combine(new Transport[]{added}, null));
    }

    @Test
    void aSearchOffersTheTeleportsItWasHandedInBothBankStates() {
        RequestPathfinderConfig config =
                new RequestPathfinderConfig(mock(Client.class), mock(ShortestPathConfig.class));
        Transport added = teleportTo(3200, 3200);

        assertEquals(0, config.getUsableTeleports(false).length, "nothing is offered before a refresh");

        config.addUsableTeleports(new Transport[0]);
        assertEquals(0, config.getUsableTeleports(false).length);

        config.addUsableTeleports(new Transport[]{added});
        assertEquals(1, config.getUsableTeleports(false).length);
        assertSame(added, config.getUsableTeleports(false)[0]);
        assertSame(added, config.getUsableTeleports(true)[0]);
    }

    private static Transport teleportTo(int x, int y) {
        return new Transport.TransportBuilder()
                .type(TransportType.TELEPORTATION_ITEM)
                .destination(WorldPointUtil.packWorldPoint(x, y, 0))
                .build();
    }
}
