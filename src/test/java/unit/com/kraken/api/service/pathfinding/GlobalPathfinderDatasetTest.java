package com.kraken.api.service.pathfinding;

import com.kraken.api.service.walker.WalkerConfig;
import com.kraken.api.service.walker.transport.GroupingTeleport;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.registry.TransportCategory;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real search over everything that ships with the API: the collision map, the transport
 * dataset and the hand written rows. Only the player is made up.
 *
 * <p>These are the routes a walk depends on most, so each test names what the route has to use or
 * avoid for the player it is planned for.</p>
 */
class GlobalPathfinderDatasetTest {

    private static final WorldPoint LUMBRIDGE_COURTYARD = new WorldPoint(3222, 3218, 0);
    private static final WorldPoint LUMBRIDGE_BANK = new WorldPoint(3208, 3220, 2);
    private static final WorldPoint VARROCK_SQUARE = new WorldPoint(3213, 3424, 0);
    private static final WorldPoint AL_KHARID_PALACE = new WorldPoint(3293, 3180, 0);
    private static final WorldPoint PORT_SARIM_DOCK = new WorldPoint(3029, 3217, 0);
    private static final WorldPoint MUSA_POINT = new WorldPoint(2917, 3155, 0);
    private static final WorldPoint EDGEVILLE = new WorldPoint(3087, 3496, 0);
    private static final WorldPoint CASTLE_WARS = new WorldPoint(2442, 3090, 0);
    private static final int AMULET_OF_GLORY_4 = 1712;
    private static final int FAIRY_TALE_II_VARBIT = 2326;

    /** Holds the minute the minigame teleport was last used. The dataset's rows read it by this id. */
    private static final int MINIGAME_TELEPORT_LAST_USED_VARP = 888;

    private DatasetSearch search;
    private GlobalPathfinder pathfinder;

    @BeforeEach
    void setup() throws Exception {
        search = new DatasetSearch();
        pathfinder = search.getPathfinder();
    }

    private void playing(TransportRequirements.PlayerState state) {
        search.playing(state);
    }

    private static TransportRequirements.PlayerState.PlayerStateBuilder newAccount() {
        return DatasetSearch.newAccount();
    }

    /** Plans with the options a walk uses. */
    private GlobalPathfinder.PathResult route(WorldPoint from, WorldPoint to) {
        return pathfinder.findPathResult(from, to, WalkerConfig.builder().build().getPathfinderConfig());
    }

    /**
     * Plans with the options a walk uses, less the minigame teleports. A new account can use
     * several of those from anywhere, which opens ways across that a test of something else has
     * to rule out.
     */
    private GlobalPathfinder.PathResult routeWithoutMinigameTeleports(WorldPoint from, WorldPoint to) {
        return pathfinder.findPathResult(from, to, WalkerConfig.builder().build().getPathfinderConfig().toBuilder()
                .useTeleportationMinigames(false).build());
    }

    /** Starts the state of a new account that has trained nothing but woodcutting. */
    private static TransportRequirements.PlayerState.PlayerStateBuilder woodcutter(int woodcuttingLevel) {
        int[] levels = new int[Skill.values().length];
        Arrays.fill(levels, 1);
        levels[Skill.WOODCUTTING.ordinal()] = woodcuttingLevel;
        return TransportRequirements.PlayerState.builder().skillLevels(levels).combatLevel(3).totalLevel(31 + woodcuttingLevel);
    }

    private static List<String> ids(GlobalPathfinder.PathResult result) {
        return result.getTransports().stream()
                .map(usage -> usage.getDefinition().getId())
                .collect(Collectors.toList());
    }

    private static List<TransportCategory> kinds(GlobalPathfinder.PathResult result) {
        return result.getTransports().stream().map(GlobalPathfinder.TransportUsage::getType).collect(Collectors.toList());
    }

    @Test
    void aNewAccountWalksFromLumbridgeToVarrock() {
        GlobalPathfinder.PathResult result = route(LUMBRIDGE_COURTYARD, VARROCK_SQUARE);

        assertTrue(result.isComplete());
        assertEquals(LUMBRIDGE_COURTYARD, result.getPath().get(0));
        assertEquals(VARROCK_SQUARE, result.getPath().get(result.getPath().size() - 1));
        assertTrue(result.getPath().size() > 206 && result.getPath().size() < 320,
                "the walk is " + result.getPath().size() + " tiles for 206 in a straight line");
        assertTrue(kinds(result).stream().allMatch(kind -> kind == TransportCategory.GENERAL),
                "a new account has nothing but doors and gates to use: " + ids(result));
    }

    @Test
    void theCastleBankIsReachedByTheStairs() {
        GlobalPathfinder.PathResult result = route(LUMBRIDGE_COURTYARD, LUMBRIDGE_BANK);

        assertTrue(result.isComplete());
        assertTrue(result.getTransports().size() >= 2, "two flights of stairs lead to the bank: " + ids(result));
        for (GlobalPathfinder.TransportUsage climb : result.getTransports()) {
            assertEquals(TransportCategory.GENERAL, climb.getType());
            assertFalse(climb.isTeleport());
            assertTrue(climb.getDefinition().getId().startsWith("kraken:upstream-transports-"), climb.getDefinition().getId());
        }
        assertEquals(2, result.getPath().get(result.getPath().size() - 1).getPlane());
    }

    @Test
    void theAlKharidGateIsOperatedByTheRowThePlayerCanUse() {
        GlobalPathfinder.PathResult broke = route(LUMBRIDGE_COURTYARD, AL_KHARID_PALACE);
        playing(newAccount().item(ItemID.COINS, 10).build());
        GlobalPathfinder.PathResult paying = route(LUMBRIDGE_COURTYARD, AL_KHARID_PALACE);
        playing(newAccount().completedQuest(Quest.PRINCE_ALI_RESCUE).build());
        GlobalPathfinder.PathResult friendOfTheEmir = route(LUMBRIDGE_COURTYARD, AL_KHARID_PALACE);

        // The collision map has the gate standing open, so the route crosses its tiles whoever
        // plans it. What the rows decide is which transport, if any, the walker operates there.
        assertTrue(ids(broke).stream().noneMatch(id -> id.contains("gate")),
                "a player without coins is offered no gate to operate: " + ids(broke));
        assertTrue(ids(paying).stream().anyMatch(id -> id.startsWith("kraken:al-kharid-gate-") && id.endsWith("-toll")),
                ids(paying).toString());
        assertTrue(ids(friendOfTheEmir).stream().anyMatch(id -> id.startsWith("kraken:al-kharid-gate-") && id.endsWith("-quest")),
                ids(friendOfTheEmir).toString());
        assertTrue(ids(paying).stream().noneMatch(id -> id.startsWith("kraken:upstream-transports-3267-322")
                        || id.startsWith("kraken:upstream-transports-3268-322")),
                "the dataset's free gate is hidden: " + ids(paying));
    }

    @Test
    void karamjaNeedsTheFareForTheShip() {
        GlobalPathfinder.PathResult stranded = routeWithoutMinigameTeleports(PORT_SARIM_DOCK, MUSA_POINT);
        playing(newAccount().item(ItemID.COINS, 30).build());
        GlobalPathfinder.PathResult sailing = routeWithoutMinigameTeleports(PORT_SARIM_DOCK, MUSA_POINT);

        assertFalse(stranded.isComplete(), "without 30 coins there is no way across: " + ids(stranded));
        assertTrue(sailing.isComplete());
        assertTrue(kinds(sailing).contains(TransportCategory.SHIP), ids(sailing).toString());
    }

    @Test
    void aMinigameTeleportIsPlannedWhereItIsTheWayAcross() {
        GlobalPathfinder.PathResult result = route(PORT_SARIM_DOCK, MUSA_POINT);

        // Without the fare the only way onto Karamja is the Fight Pit teleport into the volcano,
        // which asks nothing of a new account on a members world but its cooldown.
        assertTrue(result.isComplete(), ids(result).toString());
        GlobalPathfinder.TransportUsage first = result.getTransports().get(0);
        assertEquals(TransportCategory.TELEPORTATION_MINIGAME, first.getType());
        assertEquals(TransportShape.GROUPING_TELEPORT, first.getDefinition().getShape());
        assertTrue(first.isTeleport());
        assertEquals("TzHaar Fight Pit", GroupingTeleport.parse(first.getDisplayInfo()).getActivity());
    }

    @Test
    void aMinigameTeleportIsLeftOutWhileItCoolsDownOrWhenItsOptionIsOff() {
        long now = 30_000_000L;
        playing(newAccount().epochMinutes(now).varPlayer(MINIGAME_TELEPORT_LAST_USED_VARP, (int) now - 5).build());
        GlobalPathfinder.PathResult coolingDown = route(PORT_SARIM_DOCK, MUSA_POINT);
        playing(newAccount().epochMinutes(now).varPlayer(MINIGAME_TELEPORT_LAST_USED_VARP, (int) now - 21).build());
        GlobalPathfinder.PathResult cooledDown = route(PORT_SARIM_DOCK, MUSA_POINT);
        GlobalPathfinder.PathResult turnedOff = routeWithoutMinigameTeleports(PORT_SARIM_DOCK, MUSA_POINT);

        assertFalse(kinds(coolingDown).contains(TransportCategory.TELEPORTATION_MINIGAME), ids(coolingDown).toString());
        assertTrue(kinds(cooledDown).contains(TransportCategory.TELEPORTATION_MINIGAME), ids(cooledDown).toString());
        assertFalse(kinds(turnedOff).contains(TransportCategory.TELEPORTATION_MINIGAME), ids(turnedOff).toString());
    }

    @Test
    void aCanoeIsPlannedForAPlayerWhoCanBuildTheHullTheTripNeeds() {
        WorldPoint gnomeStronghold = new WorldPoint(2461, 3444, 0);
        String wakaToTheStronghold = "kraken:upstream-canoes-2439-3135-0-to-2523-3408-0";

        playing(woodcutter(57).item(ItemID.BRONZE_AXE, 1).build());
        GlobalPathfinder.PathResult withAWaka = routeWithoutMinigameTeleports(CASTLE_WARS, gnomeStronghold);
        GlobalPathfinder.PathResult canoesOff = pathfinder.findPathResult(CASTLE_WARS, gnomeStronghold,
                WalkerConfig.builder().build().getPathfinderConfig().toBuilder()
                        .useTeleportationMinigames(false).useCanoes(false).build());
        playing(woodcutter(56).item(ItemID.BRONZE_AXE, 1).build());
        GlobalPathfinder.PathResult belowTheLevel = routeWithoutMinigameTeleports(CASTLE_WARS, gnomeStronghold);
        playing(woodcutter(57).build());
        GlobalPathfinder.PathResult withoutAnAxe = routeWithoutMinigameTeleports(CASTLE_WARS, gnomeStronghold);

        assertTrue(withAWaka.isComplete());
        assertTrue(ids(withAWaka).contains(wakaToTheStronghold), ids(withAWaka).toString());
        GlobalPathfinder.TransportUsage canoe = withAWaka.getTransports().get(0);
        assertEquals(TransportShape.CANOE, canoe.getDefinition().getShape());
        assertEquals("Tree Gnome Stronghold", canoe.getDisplayInfo());
        assertFalse(canoe.isTeleport());

        assertFalse(kinds(canoesOff).contains(TransportCategory.CANOE), ids(canoesOff).toString());
        assertFalse(ids(belowTheLevel).contains(wakaToTheStronghold), ids(belowTheLevel).toString());
        assertFalse(kinds(withoutAnAxe).contains(TransportCategory.CANOE), ids(withoutAnAxe).toString());
    }

    @Test
    void aBoatCanBeTurnedOffByItsPlanningOption() {
        playing(newAccount().item(ItemID.COINS, 30).build());

        GlobalPathfinder.PathResult result = pathfinder.findPathResult(PORT_SARIM_DOCK, MUSA_POINT,
                WalkerConfig.builder().build().getPathfinderConfig().toBuilder()
                        .useShips(false).useBoats(false).useCharterShips(false)
                        .useTeleportationMinigames(false).build());

        assertFalse(kinds(result).contains(TransportCategory.SHIP));
        assertFalse(result.isComplete(), "nothing else crosses once the Fight Pit teleport is off too");
    }

    @Test
    void aCarriedGloryIsRubbedForAFarAwayEdgeville() {
        playing(newAccount().item(AMULET_OF_GLORY_4, 1).build());

        GlobalPathfinder.PathResult result = route(AL_KHARID_PALACE, EDGEVILLE);

        assertTrue(result.isComplete());
        GlobalPathfinder.TransportUsage first = result.getTransports().get(0);
        assertEquals(TransportCategory.TELEPORTATION_ITEM, first.getType());
        assertTrue(first.isTeleport());
        assertTrue(first.getDefinition().getItemIds().contains(AMULET_OF_GLORY_4));
        assertEquals("Edgeville", first.getDisplayInfo());
        assertTrue(first.isConsumable());
        assertEquals(List.of(AL_KHARID_PALACE, EDGEVILLE), result.getPath());
    }

    @Test
    void fairyRingsAreUsedWithAStaffOnceTheQuestAllowsThem() {
        WorldPoint nearZanarisRing = new WorldPoint(2412, 4436, 0);
        WorldPoint nearRingAtMudskipperPoint = new WorldPoint(2996, 3116, 0);
        playing(newAccount().item(ItemID.DRAMEN_STAFF, 1).build());
        GlobalPathfinder.PathResult beforeTheQuest = route(nearZanarisRing, nearRingAtMudskipperPoint);
        playing(newAccount().item(ItemID.DRAMEN_STAFF, 1).varbit(FAIRY_TALE_II_VARBIT, 40).build());
        GlobalPathfinder.PathResult withStaff = route(nearZanarisRing, nearRingAtMudskipperPoint);
        playing(newAccount().varbit(FAIRY_TALE_II_VARBIT, 40).build());
        GlobalPathfinder.PathResult withoutStaff = route(nearZanarisRing, nearRingAtMudskipperPoint);

        assertFalse(kinds(beforeTheQuest).contains(TransportCategory.FAIRY_RING));
        assertTrue(withStaff.isComplete());
        assertTrue(kinds(withStaff).contains(TransportCategory.FAIRY_RING), ids(withStaff).toString());
        assertFalse(kinds(withoutStaff).contains(TransportCategory.FAIRY_RING));
    }

    @Test
    void aLongRouteAcrossTheMainlandCompletesForAnAccountWithEverything() {
        WorldPoint ardougneMarket = new WorldPoint(2662, 3305, 0);
        int[] levels = new int[Skill.values().length];
        Arrays.fill(levels, 99);
        TransportRequirements.PlayerState.PlayerStateBuilder maxed = TransportRequirements.PlayerState.builder()
                .skillLevels(levels).combatLevel(126).totalLevel(TransportRequirements.highestTotalLevel())
                .item(ItemID.COINS, 100_000).item(ItemID.DRAMEN_STAFF, 1).varbit(FAIRY_TALE_II_VARBIT, 40);
        for (Quest quest : Quest.values()) {
            maxed.completedQuest(quest);
        }
        playing(maxed.build());

        GlobalPathfinder.PathResult result = route(LUMBRIDGE_COURTYARD, ardougneMarket);

        assertTrue(result.isComplete(), "nodes checked: " + result.getNodesChecked());
        assertEquals(ardougneMarket, result.getPath().get(result.getPath().size() - 1));
        assertTrue(result.getPath().size() < 700, "the route is " + result.getPath().size() + " tiles: " + ids(result));
        for (GlobalPathfinder.TransportUsage usage : result.getTransports()) {
            assertEquals(result.getPath().get(usage.getPathIndex() + 1), usage.getDestination());
            assertTrue(usage.getDefinition().getCategory().isEnabled(result.getConfig()), usage.getDefinition().getId());
        }
    }

    @Test
    void theDataIsReadOnceAndSharedBetweenSearches() {
        route(LUMBRIDGE_COURTYARD, LUMBRIDGE_BANK);
        WorldCollisionData map = WorldCollisionData.bundled();

        assertTrue(route(LUMBRIDGE_COURTYARD, VARROCK_SQUARE).isComplete());

        assertTrue(map == WorldCollisionData.bundled());
    }
}
