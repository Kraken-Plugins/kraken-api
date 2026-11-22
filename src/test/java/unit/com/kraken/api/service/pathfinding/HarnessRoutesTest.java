package com.kraken.api.service.pathfinding;

import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.registry.TransportCategory;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plugins.api.WalkerDestination;
import plugins.api.precondition.SuiteWalker;
import plugins.api.tests.service.TransportTest;
import plugins.api.world.NamedLocation;
import plugins.api.world.RouteInspector;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plans the routes the in-client test harness depends on over the bundled data, so a regenerated
 * dataset or a planner change that breaks them fails the build and not a run in the game.
 *
 * <p>Two things are covered: the trips the runner makes between the places tests run, and the
 * route up and down the Lumbridge Castle stairs that the in-client transport test asserts on.</p>
 */
class HarnessRoutesTest {

    private static final WorldPoint COURTYARD = NamedLocation.LUMBRIDGE_CASTLE.getAnchor();
    private static final WorldPoint CASTLE_BANK = WalkerDestination.LUMBRIDGE_CASTLE_BANK.getTile();
    private static final WorldPoint VARROCK = NamedLocation.VARROCK_EAST_BANK.getAnchor();
    private static final WorldPoint AL_KHARID_PALACE = new WorldPoint(3293, 3180, 0);
    private static final WorldPoint PORT_SARIM_DOCK = new WorldPoint(3029, 3217, 0);
    private static final WorldPoint MUSA_POINT = new WorldPoint(2917, 3155, 0);
    private static final int AMULET_OF_GLORY_4 = 1712;
    private static final int RING_OF_DUELING_8 = 2552;
    private static final int RING_OF_WEALTH_5 = 11980;

    /** What the runner plans a trip with unless consumables are allowed. */
    private static final GlobalPathfinderConfig FREE_TRAVEL = SuiteWalker.travelConfig(false).getPathfinderConfig();

    /** What the runner plans a trip with once consumables are allowed. */
    private static final GlobalPathfinderConfig SPENDING_TRAVEL = SuiteWalker.travelConfig(true).getPathfinderConfig();

    private DatasetSearch search;

    @BeforeEach
    void setup() throws Exception {
        search = new DatasetSearch();
    }

    private GlobalPathfinder.PathResult route(WorldPoint from, WorldPoint to, GlobalPathfinderConfig config) {
        return search.getPathfinder().findPathResult(from, to, config);
    }

    private static List<String> ids(GlobalPathfinder.PathResult result) {
        return result.getTransports().stream()
                .map(usage -> usage.getDefinition().getId())
                .collect(Collectors.toList());
    }

    private static List<TransportCategory> kinds(GlobalPathfinder.PathResult result) {
        return result.getTransports().stream().map(GlobalPathfinder.TransportUsage::getType).collect(Collectors.toList());
    }

    private static boolean spendsAnItem(GlobalPathfinder.TransportUsage usage) {
        return usage.isConsumable()
                || usage.getType() == TransportCategory.TELEPORTATION_ITEM
                || usage.getType() == TransportCategory.TELEPORTATION_SPELL;
    }

    /** An account with every level and quest that carries coins and several ways to teleport. */
    private static TransportRequirements.PlayerState wealthyAccount() {
        int[] levels = new int[Skill.values().length];
        Arrays.fill(levels, 99);
        TransportRequirements.PlayerState.PlayerStateBuilder wealthy = TransportRequirements.PlayerState.builder()
                .skillLevels(levels).combatLevel(126).totalLevel(TransportRequirements.highestTotalLevel())
                .item(ItemID.COINS, 100_000)
                .item(ItemID.POH_TABLET_VARROCKTELEPORT, 5)
                .item(AMULET_OF_GLORY_4, 1)
                .item(RING_OF_DUELING_8, 1)
                .item(RING_OF_WEALTH_5, 1);
        for (Quest quest : Quest.values()) {
            wealthy.completedQuest(quest);
        }
        return wealthy.build();
    }

    /**
     * Plans the trip between every two places the suite runs and checks each one arrives without
     * using up an item, a charge or a rune.
     */
    private void assertEveryTripArrivesAndUsesUpNothing(GlobalPathfinderConfig config) {
        for (NamedLocation from : NamedLocation.values()) {
            for (NamedLocation to : NamedLocation.values()) {
                if (from == to || from.getAnchor() == null || to.getAnchor() == null) {
                    continue;
                }

                GlobalPathfinder.PathResult trip = route(from.getAnchor(), to.getAnchor(), config);
                String label = from + " to " + to + " through " + ids(trip);

                assertTrue(trip.isComplete(), label);
                assertTrue(trip.getTransports().stream().noneMatch(HarnessRoutesTest::spendsAnItem), label);
            }
        }
    }

    @Test
    void everyTripBetweenSuiteStopsArrivesForANewAccount() {
        assertEveryTripArrivesAndUsesUpNothing(FREE_TRAVEL);
        assertEveryTripArrivesAndUsesUpNothing(PathfinderLiveConfig.resolve(FREE_TRAVEL, false));
    }

    @Test
    void aTripUsesUpNothingHoweverMuchThePlayerCarries() {
        search.playing(wealthyAccount());

        assertEveryTripArrivesAndUsesUpNothing(FREE_TRAVEL);
    }

    @Test
    void aTripUsesACarriedTabletOnceConsumablesAreAllowed() {
        search.playing(DatasetSearch.newAccount().item(ItemID.POH_TABLET_VARROCKTELEPORT, 1).build());
        WorldPoint lumbridge = NamedLocation.LUMBRIDGE_GRAVEYARD.getAnchor();

        GlobalPathfinder.PathResult free = route(lumbridge, VARROCK, FREE_TRAVEL);
        GlobalPathfinder.PathResult spending = route(lumbridge, VARROCK, SPENDING_TRAVEL);

        assertTrue(free.getTransports().stream().noneMatch(HarnessRoutesTest::spendsAnItem), ids(free).toString());
        assertTrue(kinds(spending).contains(TransportCategory.TELEPORTATION_ITEM), ids(spending).toString());
    }

    @Test
    void theAlKharidTollIsOnlyPaidOnceConsumablesAreAllowed() {
        search.playing(DatasetSearch.newAccount().item(ItemID.COINS, 10).build());

        GlobalPathfinder.PathResult free = route(COURTYARD, AL_KHARID_PALACE, FREE_TRAVEL);
        GlobalPathfinder.PathResult spending = route(COURTYARD, AL_KHARID_PALACE, SPENDING_TRAVEL);

        assertTrue(ids(free).stream().noneMatch(id -> id.endsWith("-toll")), ids(free).toString());
        assertTrue(ids(spending).stream().anyMatch(id -> id.endsWith("-toll")), ids(spending).toString());
    }

    @Test
    void aShipsFareIsOnlyPaidOnceConsumablesAreAllowed() {
        search.playing(DatasetSearch.newAccount().item(ItemID.COINS, 100_000).build());

        GlobalPathfinder.PathResult free = route(PORT_SARIM_DOCK, MUSA_POINT, FREE_TRAVEL);
        GlobalPathfinder.PathResult spending = route(PORT_SARIM_DOCK, MUSA_POINT, SPENDING_TRAVEL);

        assertFalse(kinds(free).contains(TransportCategory.SHIP), ids(free).toString());
        assertFalse(free.isComplete(), "with nothing spent on the ship there is no way across: " + ids(free));
        assertTrue(kinds(spending).contains(TransportCategory.SHIP), ids(spending).toString());
        assertTrue(spending.isComplete());
    }

    @Test
    void theHomeTeleportIsPlannedWheneverItSavesTheWalk() {
        // It costs nothing, so a trip takes it whether or not consumables are allowed, and no
        // planning option turns it off. This player has never cast it, so it is off cooldown.
        GlobalPathfinder.PathResult fromVarrock = route(VARROCK, COURTYARD, FREE_TRAVEL);
        GlobalPathfinder.PathResult fromTheBank = route(CASTLE_BANK, COURTYARD, FREE_TRAVEL);

        assertEquals(List.of(TransportCategory.TELEPORTATION_SPELL_HOME), kinds(fromVarrock), ids(fromVarrock).toString());
        assertTrue(fromVarrock.isComplete());

        // The courtyard is where it lands, which is why the transport test does not walk down to
        // the courtyard: this route takes no stairs at all.
        assertEquals(List.of(TransportCategory.TELEPORTATION_SPELL_HOME), kinds(fromTheBank), ids(fromTheBank).toString());
        assertFalse(RouteInspector.climbs(fromTheBank, 2, 0));
    }

    @Test
    void theCastleBankIsClimbedToAndFromByTheStairs() {
        for (GlobalPathfinderConfig config : List.of(FREE_TRAVEL, PathfinderLiveConfig.resolve(FREE_TRAVEL, false))) {
            GlobalPathfinder.PathResult up = route(COURTYARD, CASTLE_BANK, config);

            assertTrue(up.isComplete());
            assertTrue(RouteInspector.climbs(up, 0, 2), RouteInspector.describe(up).toString());
            assertEquals(2, RouteInspector.planeChanges(up).size(), "two flights of stairs lead to the bank");

            // The transport test walks back down to the tile the first flight is entered from,
            // which keeps the way down on the stairs although the home teleport is available.
            WorldPoint foot = RouteInspector.planeChanges(up).get(0).getOrigin();
            GlobalPathfinder.PathResult down = route(CASTLE_BANK, foot, config);

            assertTrue(down.isComplete());
            assertTrue(RouteInspector.climbs(down, 2, 0), RouteInspector.describe(down).toString());
        }
    }

    @Test
    void theTransportTestsProbeIsPlannedThroughUntilItsHandleIsClosed() {
        TransportDefinition probe = TransportTest.planningProbe(COURTYARD, CASTLE_BANK);

        GlobalPathfinder.PathResult registered;
        try (TransportRegistry.Registration registration = search.getRegistry().register(probe)) {
            registered = route(COURTYARD, CASTLE_BANK, FREE_TRAVEL);
        }
        GlobalPathfinder.PathResult unregistered = route(COURTYARD, CASTLE_BANK, FREE_TRAVEL);

        assertTrue(RouteInspector.usesDefinition(registered, probe.getId()), ids(registered).toString());
        assertTrue(registered.isComplete());
        assertFalse(RouteInspector.usesDefinition(unregistered, probe.getId()), ids(unregistered).toString());
        assertTrue(RouteInspector.climbs(unregistered, 0, 2));
    }

    @Test
    void aHiddenStairEdgeIsRoutedAroundUntilItsHandleIsClosed() {
        GlobalPathfinder.TransportUsage climb =
                RouteInspector.planeChanges(route(COURTYARD, CASTLE_BANK, FREE_TRAVEL)).get(0);

        GlobalPathfinder.PathResult hidden;
        try (TransportRegistry.Registration handle =
                     search.getRegistry().disableEdge(climb.getOrigin(), climb.getDestination())) {
            hidden = route(COURTYARD, CASTLE_BANK, FREE_TRAVEL);
        }
        GlobalPathfinder.PathResult restored = route(COURTYARD, CASTLE_BANK, FREE_TRAVEL);

        assertFalse(RouteInspector.usesEdge(hidden, climb.getOrigin(), climb.getDestination()), ids(hidden).toString());
        assertTrue(hidden.isComplete() && RouteInspector.climbs(hidden, 0, 2),
                "the staircase is entered from another tile: " + RouteInspector.describe(hidden));
        assertTrue(RouteInspector.usesEdge(restored, climb.getOrigin(), climb.getDestination()), ids(restored).toString());
    }

    @Test
    void aRouteThatStaysOnOnePlaneClimbsNothing() {
        GlobalPathfinder.PathResult acrossTheCity = route(VARROCK, NamedLocation.GRAND_EXCHANGE.getAnchor(), FREE_TRAVEL);

        assertTrue(RouteInspector.planeChanges(acrossTheCity).isEmpty());
        assertTrue(RouteInspector.climbs(acrossTheCity, 0, 0));
        assertFalse(RouteInspector.climbs(acrossTheCity, 0, 2));
    }

    @Test
    void noRouteAtAllUsesNothing() {
        assertEquals(1, RouteInspector.describe(null).size());
        assertTrue(RouteInspector.planeChanges(null).isEmpty());
        assertFalse(RouteInspector.climbs(null, 0, 2));
        assertFalse(RouteInspector.usesDefinition(null, "kraken:anything"));
        assertFalse(RouteInspector.usesEdge(null, COURTYARD, CASTLE_BANK));
    }
}
