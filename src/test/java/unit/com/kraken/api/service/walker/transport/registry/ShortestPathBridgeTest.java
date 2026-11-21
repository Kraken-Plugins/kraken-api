package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.magic.spellbook.Standard;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.PlayerStateReader;
import com.kraken.api.service.walker.transport.TransportRequirements;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.WorldPointUtil;
import shortestpath.pathfinder.PathfinderConfig;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportType;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers how registered transports reach a shortest-path search: which edges are added, which
 * dataset edges are hidden, and how a route step is traced back to its definition.
 */
class ShortestPathBridgeTest {

    private static final WorldPoint ORIGIN = new WorldPoint(3200, 3200, 0);
    private static final WorldPoint DESTINATION = new WorldPoint(3300, 3300, 0);
    private static final WorldPoint ELSEWHERE = new WorldPoint(3400, 3400, 0);
    private static final int PACKED_ORIGIN = WorldPointUtil.packWorldPoint(ORIGIN);
    private static final int PACKED_DESTINATION = WorldPointUtil.packWorldPoint(DESTINATION);

    private final PrimitiveIntHashMap<Transport[]> withoutBank = new PrimitiveIntHashMap<>(8);
    private final PrimitiveIntHashMap<Transport[]> withBank = new PrimitiveIntHashMap<>(8);
    private final MagicService magicService = mock(MagicService.class);
    private final PathfinderConfig pathfinderConfig = mock(PathfinderConfig.class);

    @Test
    void aDefinitionCompilesToAGenericEdgeCarryingItsClickTarget() {
        TransportDefinition definition = TransportDefinition.object("test:door", ORIGIN, DESTINATION, 1535, "Open")
                .duration(4)
                .displayInfo("Somewhere")
                .build();

        Transport edge = ShortestPathBridge.compile(definition, ORIGIN, DESTINATION);

        assertEquals(TransportType.TRANSPORT, edge.getType());
        assertEquals(PACKED_ORIGIN, edge.getOrigin());
        assertEquals(PACKED_DESTINATION, edge.getDestination());
        assertEquals(4, edge.getDuration());
        assertEquals("Open 1535", edge.getObjectInfo());
        assertEquals("Somewhere", edge.getDisplayInfo());
    }

    @Test
    void aUsableDefinitionIsAddedToBothBankStatesAndTracedBackByIdentity() {
        TransportDefinition door = door("test:door").build();

        ShortestPathBridge.Injection injection =
                ShortestPathBridge.inject(List.of(door), Set.of(), withoutBank, withBank);

        Transport edge = withoutBank.get(PACKED_ORIGIN)[0];
        assertSame(edge, withBank.get(PACKED_ORIGIN)[0]);
        assertSame(door, injection.definitionOf(edge));
        assertNull(injection.definitionOf(ShortestPathBridge.compile(door, ORIGIN, DESTINATION)),
                "an equal looking edge that was not the one added is not a registered edge");
        assertNull(injection.definitionOf(null));
    }

    @Test
    void aRegisteredEdgeIsFoundAheadOfADatasetEdgeBetweenTheSameTiles() {
        TransportDefinition door = door("test:door").build();
        Transport dataset = datasetEdge(ORIGIN, DESTINATION);
        withoutBank.put(PACKED_ORIGIN, new Transport[]{dataset});

        ShortestPathBridge.Injection injection =
                ShortestPathBridge.inject(List.of(door), Set.of(), withoutBank, withBank);

        Transport chosen = GlobalPathfinder.hopBetween(
                withoutBank.get(PACKED_ORIGIN), new Transport[0], PACKED_ORIGIN, PACKED_DESTINATION);
        assertSame(door, injection.definitionOf(chosen));
        assertEquals(2, withoutBank.get(PACKED_ORIGIN).length, "the dataset edge stays unless it is replaced");
    }

    @Test
    void whenTwoDefinitionsJoinTheSameTilesTheLatestRegistrationIsTheOneOperated() {
        TransportDefinition earlier = door("first:door").build();
        TransportDefinition later = door("second:door").build();

        ShortestPathBridge.Injection injection =
                ShortestPathBridge.inject(List.of(earlier, later), Set.of(), withoutBank, withBank);

        Transport chosen = GlobalPathfinder.hopBetween(
                withoutBank.get(PACKED_ORIGIN), new Transport[0], PACKED_ORIGIN, PACKED_DESTINATION);
        assertSame(later, injection.definitionOf(chosen));
    }

    @Test
    void aBidirectionalDefinitionAddsAnEdgeEachWayForTheSameDefinition() {
        TransportDefinition door = door("test:door").bidirectional(true).build();

        ShortestPathBridge.Injection injection =
                ShortestPathBridge.inject(List.of(door), Set.of(), withoutBank, withBank);

        Transport back = withoutBank.get(PACKED_DESTINATION)[0];
        assertEquals(PACKED_ORIGIN, back.getDestination());
        assertSame(door, injection.definitionOf(back));
        assertSame(door, injection.definitionOf(withoutBank.get(PACKED_ORIGIN)[0]));
    }

    @Test
    void aDisabledEdgeIsRemovedFromBothBankStatesAndItsNeighboursAreKept() {
        Transport unwanted = datasetEdge(ORIGIN, DESTINATION);
        Transport kept = datasetEdge(ORIGIN, ELSEWHERE);
        withoutBank.put(PACKED_ORIGIN, new Transport[]{unwanted, kept});
        withBank.put(PACKED_ORIGIN, new Transport[]{unwanted, kept});

        ShortestPathBridge.inject(List.of(), Set.of(new TransportRegistry.Edge(ORIGIN, DESTINATION)), withoutBank, withBank);

        assertEquals(1, withoutBank.get(PACKED_ORIGIN).length);
        assertSame(kept, withoutBank.get(PACKED_ORIGIN)[0]);
        assertSame(kept, withBank.get(PACKED_ORIGIN)[0]);
    }

    @Test
    void aReplacingDefinitionThatIsNotUsableStillHidesTheDatasetEdge() {
        TransportRegistry registry = new TransportRegistry(List.of());
        registry.register(door("test:toll")
                .replaces(true)
                .requirements(DefinitionRequirements.builder().skill(Skill.AGILITY, 99).build())
                .build());
        withoutBank.put(PACKED_ORIGIN, new Transport[]{datasetEdge(ORIGIN, DESTINATION)});
        withBank.put(PACKED_ORIGIN, new Transport[]{datasetEdge(ORIGIN, DESTINATION)});

        ShortestPathBridge.Injection injection = bridge(registry).apply(GlobalPathfinderConfig.builder().build(), config());

        assertEquals(0, withoutBank.get(PACKED_ORIGIN).length);
        assertEquals(0, withBank.get(PACKED_ORIGIN).length);
        assertSame(ShortestPathBridge.Injection.NONE, injection);
    }

    @Test
    void oneMapSharedByBothBankStatesReceivesEachEdgeOnce() {
        ShortestPathBridge.inject(List.of(door("test:door").build()), Set.of(), withoutBank, withoutBank);

        assertEquals(1, withoutBank.get(PACKED_ORIGIN).length);
    }

    @Test
    void aTeleportUsableFromAnywhereCompilesToATeleportWithNoOriginAndAWildernessLimit() {
        TransportDefinition glory = TransportDefinition.itemTeleport("test:glory", DESTINATION, 1712)
                .menuOption("Rub")
                .displayInfo("Edgeville")
                .maxWildernessLevel(30)
                .duration(4)
                .build();

        Transport teleport = ShortestPathBridge.compileTeleport(glory);

        assertEquals(TransportType.TELEPORTATION_ITEM, teleport.getType());
        assertTrue(teleport.getType().isTeleport());
        assertEquals(Transport.UNDEFINED_ORIGIN, teleport.getOrigin());
        assertEquals(PACKED_DESTINATION, teleport.getDestination());
        assertEquals(4, teleport.getDuration());
        assertEquals("Edgeville", teleport.getDisplayInfo());
        assertEquals("Rub", teleport.getObjectInfo());
        assertTrue(teleport.isUsableAtWildernessLevel(30));
        assertFalse(teleport.isUsableAtWildernessLevel(31));
    }

    @Test
    void aSpellCompilesToASpellTeleportThatStopsAtLevelTwentyByDefault() {
        Transport teleport = ShortestPathBridge.compileTeleport(
                TransportDefinition.spellTeleport("test:varrock", DESTINATION, "Varrock Teleport").build());

        assertEquals(TransportType.TELEPORTATION_SPELL, teleport.getType());
        assertEquals("Varrock Teleport", teleport.getDisplayInfo());
        assertTrue(teleport.isUsableAtWildernessLevel(20));
        assertFalse(teleport.isUsableAtWildernessLevel(30));
    }

    @Test
    void aTeleportIsHandedBackForTheSearchInsteadOfBeingFiledUnderATile() {
        TransportDefinition tablet = TransportDefinition.itemTeleport("test:tab", DESTINATION, 8007).build();

        ShortestPathBridge.Injection injection =
                ShortestPathBridge.inject(List.of(tablet), Set.of(), withoutBank, withBank);

        assertEquals(0, withoutBank.size());
        assertEquals(0, withBank.size());
        assertEquals(1, injection.getTeleports().length);
        assertSame(tablet, injection.definitionOf(injection.getTeleports()[0]));
    }

    @Test
    void theLatestRegisteredTeleportToATileIsTheOneARouteStepIsTracedTo() {
        TransportDefinition earlier = TransportDefinition.itemTeleport("first:tab", DESTINATION, 8007).build();
        TransportDefinition later = TransportDefinition.itemTeleport("second:tab", DESTINATION, 8008).build();

        ShortestPathBridge.Injection injection =
                ShortestPathBridge.inject(List.of(earlier, later), Set.of(), withoutBank, withBank);

        Transport chosen = GlobalPathfinder.hopBetween(
                null, injection.getTeleports(), PACKED_ORIGIN, PACKED_DESTINATION);
        assertSame(later, injection.definitionOf(chosen));
    }

    @Test
    void aSearchWithNoRegisteredTeleportHandsBackNone() {
        ShortestPathBridge.Injection injection =
                ShortestPathBridge.inject(List.of(door("test:door").build()), Set.of(), withoutBank, withBank);

        assertEquals(0, injection.getTeleports().length);
        assertEquals(0, ShortestPathBridge.Injection.NONE.getTeleports().length);
    }

    @Test
    void teleportsAreLeftOutWhileThePlayerIsAboardABoatAndOtherTransportsStay() {
        TransportRegistry registry = new TransportRegistry(List.of());
        TransportDefinition door = door("test:door").build();
        registry.register(door);
        registry.register(TransportDefinition.itemTeleport("test:tab", ELSEWHERE, 8007).build());
        when(pathfinderConfig.isOnSailingBoat()).thenReturn(true);

        ShortestPathBridge.Injection injection = bridge(registry, heldItem(8007))
                .apply(GlobalPathfinderConfig.builder().build(), config());

        assertEquals(0, injection.getTeleports().length);
        assertSame(door, injection.definitionOf(withoutBank.get(PACKED_ORIGIN)[0]));
    }

    @Test
    void anItemTeleportIsPlannedOnlyWhileOneOfItsItemsIsHeld() {
        TransportRegistry registry = new TransportRegistry(List.of());
        registry.register(TransportDefinition.itemTeleport("test:tab", DESTINATION, 8007).build());
        GlobalPathfinderConfig defaults = GlobalPathfinderConfig.builder().build();

        assertEquals(0, bridge(registry).apply(defaults, config()).getTeleports().length);
        assertEquals(1, bridge(registry, heldItem(8007)).apply(defaults, config()).getTeleports().length);
        assertEquals(0, bridge(registry, heldItem(8007))
                .apply(GlobalPathfinderConfig.builder().useTeleportationItems(false).build(), config())
                .getTeleports().length);
    }

    @Test
    void aSpellTeleportIsPlannedOnlyWhileTheSpellCanBeCast() {
        TransportRegistry registry = new TransportRegistry(List.of());
        registry.register(TransportDefinition.spellTeleport("test:varrock", DESTINATION, "Varrock Teleport").build());
        GlobalPathfinderConfig defaults = GlobalPathfinderConfig.builder().build();

        when(magicService.castRefusal(Standard.VARROCK_TELEPORT)).thenReturn("Wrong spellbook: LUNAR");
        assertEquals(0, bridge(registry).apply(defaults, config()).getTeleports().length);

        when(magicService.castRefusal(Standard.VARROCK_TELEPORT)).thenReturn(null);
        assertEquals(1, bridge(registry).apply(defaults, config()).getTeleports().length);
    }

    @Test
    void aSpellTeleportThatNamesNoKnownSpellIsNeverPlanned() {
        TransportRegistry registry = new TransportRegistry(List.of());
        registry.register(TransportDefinition.spellTeleport("test:made-up", DESTINATION, "Definitely Not A Spell").build());

        assertEquals(0, bridge(registry).apply(GlobalPathfinderConfig.builder().build(), config()).getTeleports().length);
    }

    @Test
    void aSpellThatBringsItsOwnHandlerIsNotJudgedByTheMagicService() {
        TransportRegistry registry = new TransportRegistry(List.of());
        registry.register(TransportDefinition.spellTeleport("test:custom", DESTINATION, "Definitely Not A Spell")
                .handler(context -> true)
                .build());

        assertEquals(1, bridge(registry).apply(GlobalPathfinderConfig.builder().build(), config()).getTeleports().length);
        verify(magicService, never()).castRefusal(any());
    }

    @Test
    void anEmptyRegistryLeavesTheSearchAloneWithoutReadingPlayerState() {
        PlayerStateReader reader = mock(PlayerStateReader.class);
        ShortestPathBridge bridge = new ShortestPathBridge(new TransportRegistry(List.of()), reader, magicService);

        ShortestPathBridge.Injection injection = bridge.apply(GlobalPathfinderConfig.builder().build(), config());

        assertSame(ShortestPathBridge.Injection.NONE, injection);
        verifyNoInteractions(reader);
        assertEquals(0, withoutBank.size());
    }

    @Test
    void applyAddsOnlyTheDefinitionsTheSearchMayUse() {
        TransportRegistry registry = new TransportRegistry(List.of());
        TransportDefinition door = door("test:door").build();
        registry.register(door);
        registry.register(TransportDefinition.object("test:boat", ELSEWHERE, DESTINATION, 1, "Board")
                .category(TransportCategory.BOAT)
                .build());

        ShortestPathBridge.Injection injection = bridge(registry)
                .apply(GlobalPathfinderConfig.builder().useBoats(false).build(), config());

        assertSame(door, injection.definitionOf(withoutBank.get(PACKED_ORIGIN)[0]));
        assertNull(withoutBank.get(WorldPointUtil.packWorldPoint(ELSEWHERE)));
    }

    private ShortestPathBridge bridge(TransportRegistry registry) {
        return bridge(registry, TransportRequirements.PlayerState.builder().build());
    }

    private ShortestPathBridge bridge(TransportRegistry registry, TransportRequirements.PlayerState state) {
        PlayerStateReader reader = mock(PlayerStateReader.class);
        when(reader.read(anyCollection())).thenReturn(state);
        return new ShortestPathBridge(registry, reader, magicService);
    }

    private static TransportRequirements.PlayerState heldItem(int itemId) {
        return TransportRequirements.PlayerState.builder().item(itemId, 1).build();
    }

    private PathfinderConfig config() {
        when(pathfinderConfig.getTransportsPacked(false)).thenReturn(withoutBank);
        when(pathfinderConfig.getTransportsPacked(true)).thenReturn(withBank);
        return pathfinderConfig;
    }

    private static TransportDefinition.TransportDefinitionBuilder door(String id) {
        return TransportDefinition.object(id, ORIGIN, DESTINATION, 1535, "Open");
    }

    private static Transport datasetEdge(WorldPoint origin, WorldPoint destination) {
        return new Transport.TransportBuilder()
                .type(TransportType.TRANSPORT)
                .origin(WorldPointUtil.packWorldPoint(origin))
                .destination(WorldPointUtil.packWorldPoint(destination))
                .build();
    }
}
