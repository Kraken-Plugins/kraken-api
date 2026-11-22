package com.kraken.api.service.pathfinding;

import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.walker.transport.PlayerStateReader;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.TestRegistries;
import com.kraken.api.service.walker.transport.registry.TransportCategory;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers which transports reach a search: which definitions are kept for the player and the
 * planning options, and the order the kept ones are offered in.
 */
class TransportPlannerTest {

    private static final WorldPoint ORIGIN = new WorldPoint(3200, 3200, 0);
    private static final WorldPoint DESTINATION = new WorldPoint(3300, 3300, 0);
    private static final WorldPoint ELSEWHERE = new WorldPoint(2900, 3400, 0);
    private static final WorldPoint INSIDE_HOUSE = new WorldPoint(1858, 7051, 0);
    private static final GlobalPathfinderConfig DEFAULTS = GlobalPathfinderConfig.builder().build();

    private final Client client = mock(Client.class);
    private final PlayerStateReader playerStateReader = mock(PlayerStateReader.class);
    private final MagicService magicService = mock(MagicService.class);
    private final List<TransportDefinition> dataset = new ArrayList<>();
    private final TransportRegistry registry = TestRegistries.over(dataset);
    private final TransportPlanner planner = new TransportPlanner(registry, playerStateReader, magicService, client);

    @BeforeEach
    void setup() {
        holding(TransportRequirements.PlayerState.builder().build());
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
    }

    private void holding(TransportRequirements.PlayerState state) {
        when(playerStateReader.read(any(PlayerStateReader.Needs.class))).thenReturn(state);
    }

    private static TransportDefinition.TransportDefinitionBuilder door(String id) {
        return TransportDefinition.object(id, ORIGIN, DESTINATION, 1535, "Open");
    }

    private static TransportDefinition[] definitions(TransportEdge[] edges) {
        TransportDefinition[] definitions = new TransportDefinition[edges.length];
        for (int i = 0; i < edges.length; i++) {
            definitions[i] = edges[i].definition;
        }
        return definitions;
    }

    private TransportDefinition[] leaving(WorldPoint tile, GlobalPathfinderConfig config) {
        return definitions(planner.plan(config).from(PackedPoint.pack(tile)));
    }

    private TransportDefinition[] teleports(GlobalPathfinderConfig config) {
        return definitions(planner.plan(config).teleports());
    }

    @Test
    void registeredTransportsComeBeforeTheDatasetsAndTheLatestRegistrationFirst() {
        TransportDefinition fromDataset = door("kraken:upstream-door").build();
        TransportDefinition first = door("test:first").build();
        TransportDefinition second = door("test:second").build();
        dataset.add(fromDataset);
        registry.register(first);
        registry.register(second);

        assertArrayEquals(new TransportDefinition[]{second, first, fromDataset}, leaving(ORIGIN, DEFAULTS));
    }

    @Test
    void aBidirectionalTransportIsOfferedFromBothEnds() {
        TransportDefinition gate = door("test:gate").bidirectional(true).build();
        registry.register(gate);

        assertArrayEquals(new TransportDefinition[]{gate}, leaving(ORIGIN, DEFAULTS));
        assertArrayEquals(new TransportDefinition[]{gate}, leaving(DESTINATION, DEFAULTS));
    }

    @Test
    void aDatasetTransportIsLeftOutWhenItsCategoryIsOffOrItsRequirementsAreUnmet() {
        TransportDefinition shortcut = door("kraken:upstream-shortcut")
                .category(TransportCategory.AGILITY_SHORTCUT)
                .requirements(DefinitionRequirements.builder().skill(Skill.AGILITY, 50).build())
                .build();
        dataset.add(shortcut);

        assertEquals(0, leaving(ORIGIN, DEFAULTS).length, "the player has no agility level");

        int[] levels = new int[Skill.values().length];
        levels[Skill.AGILITY.ordinal()] = 50;
        holding(TransportRequirements.PlayerState.builder().skillLevels(levels).build());

        assertArrayEquals(new TransportDefinition[]{shortcut}, leaving(ORIGIN, DEFAULTS));
        assertEquals(0, leaving(ORIGIN, GlobalPathfinderConfig.builder().useAgilityShortcuts(false).build()).length);
    }

    @Test
    void aHiddenEdgeRemovesTheDatasetTransportInThatDirectionOnly() {
        TransportDefinition there = door("kraken:upstream-there").build();
        TransportDefinition back = TransportDefinition
                .object("kraken:upstream-back", DESTINATION, ORIGIN, 1535, "Open").build();
        TransportDefinition registered = door("test:door").build();
        dataset.add(there);
        dataset.add(back);
        registry.register(registered);
        registry.disableEdge(ORIGIN, DESTINATION);

        assertArrayEquals(new TransportDefinition[]{registered}, leaving(ORIGIN, DEFAULTS),
                "a registered transport between the same tiles is not hidden");
        assertArrayEquals(new TransportDefinition[]{back}, leaving(DESTINATION, DEFAULTS));
    }

    @Test
    void teleportsAreOfferedFromAnywhereWithRegisteredOnesFirst() {
        TransportDefinition datasetTablet = TransportDefinition
                .itemTeleport("kraken:upstream-tablet", DESTINATION, 8007).build();
        TransportDefinition registeredTablet = TransportDefinition.itemTeleport("test:tablet", ELSEWHERE, 8008).build();
        dataset.add(datasetTablet);
        registry.register(registeredTablet);
        holding(TransportRequirements.PlayerState.builder().item(8007, 1).item(8008, 1).build());

        TransportNetwork network = planner.plan(DEFAULTS);

        assertArrayEquals(new TransportDefinition[]{registeredTablet, datasetTablet}, definitions(network.teleports()));
        assertTrue(network.teleports()[0].isTeleport());
        assertEquals(0, network.from(PackedPoint.pack(ORIGIN)).length);
    }

    @Test
    void anItemTeleportIsOnlyOfferedWhileItsItemIsHeldAndItemTeleportsAreOn() {
        dataset.add(TransportDefinition.itemTeleport("kraken:upstream-ring-of-wealth", DESTINATION, 2572, 11980).build());

        assertEquals(0, teleports(DEFAULTS).length);

        holding(TransportRequirements.PlayerState.builder().item(11980, 1).build());

        assertEquals(1, teleports(DEFAULTS).length, "charged jewellery that is carried is used");
        assertEquals(0, teleports(GlobalPathfinderConfig.builder().useTeleportationItems(false).build()).length);
    }

    @Test
    void noTeleportIsOfferedWhileThePlayerIsAboardABoat() {
        TransportDefinition tablet = TransportDefinition.itemTeleport("test:tablet", DESTINATION, 8007).build();
        TransportDefinition door = door("test:door").build();
        registry.register(tablet);
        registry.register(door);
        holding(TransportRequirements.PlayerState.builder().item(8007, 1).build());
        when(client.getVarbitValue(VarbitID.SAILING_BOARDED_BOAT)).thenReturn(1);

        TransportNetwork network = planner.plan(DEFAULTS);

        assertEquals(0, network.teleports().length);
        assertArrayEquals(new TransportDefinition[]{door}, definitions(network.from(PackedPoint.pack(ORIGIN))));
    }

    @Test
    void aSpellIsOnlyOfferedWhileTheClientReportsItCastable() {
        TransportDefinition spell = TransportDefinition
                .spellTeleport("kraken:upstream-varrock", DESTINATION, "Varrock Teleport").build();
        dataset.add(spell);

        when(magicService.castRefusal(any())).thenReturn("Check required runes");
        assertEquals(0, teleports(DEFAULTS).length);

        when(magicService.castRefusal(any())).thenReturn(null);
        assertArrayEquals(new TransportDefinition[]{spell}, teleports(DEFAULTS));
    }

    @Test
    void aSpellThatSomethingElseCastsIsNotAskedAboutAndASpellNoBookNamesIsLeftOut() {
        TransportDefinition unknown = TransportDefinition
                .spellTeleport("kraken:upstream-unknown", DESTINATION, "Teleport to Nowhere").build();
        TransportDefinition ownHandler = TransportDefinition
                .spellTeleport("test:own", ELSEWHERE, "Teleport to Nowhere").handler(context -> true).build();
        dataset.add(unknown);
        registry.register(ownHandler);

        assertArrayEquals(new TransportDefinition[]{ownHandler}, teleports(DEFAULTS));

        registry.registerHandler(TransportShape.SPELL, context -> true);

        assertArrayEquals(new TransportDefinition[]{ownHandler, unknown}, teleports(DEFAULTS));
    }

    @Test
    void seasonalTransportsNeedASeasonalWorld() {
        TransportDefinition relic = TransportDefinition.itemTeleport("kraken:upstream-relic", DESTINATION, 30000)
                .category(TransportCategory.SEASONAL).build();
        dataset.add(relic);
        holding(TransportRequirements.PlayerState.builder().item(30000, 1).build());

        assertEquals(0, teleports(DEFAULTS).length);

        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS, WorldType.SEASONAL));

        assertArrayEquals(new TransportDefinition[]{relic}, teleports(DEFAULTS));
    }

    @Test
    void transportsThroughAPlayerOwnedHouseNeedOneOfTheHouseOptions() {
        TransportDefinition houseTablet = TransportDefinition
                .itemTeleport("kraken:upstream-house-tablet", INSIDE_HOUSE, 8013).build();
        TransportDefinition jewelleryBox = TransportDefinition.builder()
                .id("kraken:upstream-jewellery-box")
                .origin(INSIDE_HOUSE)
                .destination(DESTINATION)
                .category(TransportCategory.TELEPORTATION_BOX)
                .objectInfo("Teleport Menu Ornate Jewellery Box 37520")
                .build();
        dataset.add(houseTablet);
        dataset.add(jewelleryBox);
        holding(TransportRequirements.PlayerState.builder().item(8013, 1).build());

        TransportNetwork withHouse = planner.plan(DEFAULTS);
        assertEquals(1, withHouse.teleports().length);
        assertSame(jewelleryBox, withHouse.from(PackedPoint.pack(INSIDE_HOUSE))[0].definition);

        TransportNetwork portalsOnly = planner.plan(GlobalPathfinderConfig.builder().useTeleportationBoxes(false).build());
        assertEquals(1, portalsOnly.teleports().length, "the house is still planned through for its portals");
        assertEquals(0, portalsOnly.from(PackedPoint.pack(INSIDE_HOUSE)).length);

        TransportNetwork withoutHouse = planner.plan(GlobalPathfinderConfig.builder()
                .useTeleportationBoxes(false).useTeleportationPortalsPoh(false).build());
        assertEquals(0, withoutHouse.teleports().length, "nothing leads into a house that is turned off");
        assertEquals(0, withoutHouse.from(PackedPoint.pack(INSIDE_HOUSE)).length);
    }

    @Test
    void aFareAboveTheCurrencyThresholdKeepsADatasetTransportOut() {
        dataset.add(door("kraken:upstream-charter")
                .requirements(DefinitionRequirements.builder()
                        .item(DefinitionRequirements.RequiredItem.of(ItemID.COINS, 3_200))
                        .build())
                .build());
        holding(TransportRequirements.PlayerState.builder().item(ItemID.COINS, 50_000).build());

        assertEquals(1, leaving(ORIGIN, DEFAULTS).length);
        assertEquals(0, leaving(ORIGIN, GlobalPathfinderConfig.builder().currencyThreshold(1_000).build()).length);
    }

    @Test
    void aConditionThatThrowsOrSaysNoKeepsARegisteredTransportOut() {
        registry.register(door("test:broken").availableWhen(() -> {
            throw new IllegalStateException("plugin fault");
        }).build());
        registry.register(door("test:closed").availableWhen(() -> false).build());
        TransportDefinition open = door("test:open").availableWhen(() -> true).build();
        registry.register(open);

        assertArrayEquals(new TransportDefinition[]{open}, leaving(ORIGIN, DEFAULTS));
    }

    @Test
    void aSearchThatAvoidsTheWildernessRefusesAStepIntoIt() {
        int edgeville = PackedPoint.pack(3094, 3520, 0);
        int wilderness = PackedPoint.pack(3094, 3530, 0);

        TransportNetwork avoiding = planner.plan(GlobalPathfinderConfig.builder().avoidWilderness(true).build());
        TransportNetwork careless = planner.plan(DEFAULTS);

        assertTrue(avoiding.avoidWilderness(edgeville, wilderness, false));
        assertTrue(!avoiding.avoidWilderness(edgeville, wilderness, true), "a target in the wilderness lifts the rule");
        assertTrue(!avoiding.avoidWilderness(wilderness, PackedPoint.pack(3094, 3531, 0), false));
        assertTrue(!careless.avoidWilderness(edgeville, wilderness, false));
    }
}
