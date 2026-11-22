package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.TransportHandler;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.TransportShape;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the client wide registry: what a registration adds, what closing its handle takes away,
 * and what a search is allowed to plan through.
 */
class TransportRegistryTest {

    private static final WorldPoint ORIGIN = new WorldPoint(3200, 3200, 0);
    private static final WorldPoint DESTINATION = new WorldPoint(3300, 3300, 0);
    private static final GlobalPathfinderConfig DEFAULTS = GlobalPathfinderConfig.builder().build();
    private static final TransportRequirements.PlayerState NOTHING_HELD =
            TransportRequirements.PlayerState.builder().build();

    private final TransportRegistry registry = new TransportRegistry(List.of());

    @Test
    void anEmptyRegistryHoldsNothing() {
        assertTrue(registry.snapshot().getDefinitions().isEmpty());
        assertTrue(registry.snapshot().getDataset().isEmpty());
        assertTrue(registry.snapshot().getDisabledEdges().isEmpty());
    }

    @Test
    void theDatasetSitsUnderTheRegisteredTransportsAndCannotBeUnregistered() {
        TransportDefinition datasetDoor = door("kraken:upstream-door").build();
        TransportRegistry overDataset = new TransportRegistry(List.of(), () -> List.of(datasetDoor));
        TransportDefinition registered = door("test:door").build();
        TransportRegistry.Registration registration = overDataset.register(registered);

        assertEquals(List.of(datasetDoor), overDataset.snapshot().getDataset());
        assertEquals(List.of(registered), overDataset.snapshot().getDefinitions());
        assertTrue(overDataset.snapshot().holds(datasetDoor));
        assertTrue(overDataset.snapshot().holds(registered));

        assertFalse(overDataset.unregister("kraken:upstream-door"), "a dataset transport is hidden, not unregistered");
        assertEquals(0, overDataset.unregisterAll("kraken"));
        registration.close();

        assertTrue(overDataset.snapshot().holds(datasetDoor));
        assertFalse(overDataset.snapshot().holds(registered));
        assertFalse(overDataset.snapshot().holds(null));
    }

    @Test
    void aRegisteredTransportIsInTheNextSnapshotAndNotInAnEarlierOne() {
        TransportRegistry.Snapshot before = registry.snapshot();
        TransportDefinition door = door("test:door").build();

        registry.register(door);

        assertTrue(before.getDefinitions().isEmpty(), "a snapshot a search already took must not change");
        assertEquals(List.of(door), registry.snapshot().getDefinitions());
    }

    @Test
    void closingTheHandleRemovesTheTransportAndClosingAgainDoesNothing() {
        TransportRegistry.Registration registration = registry.register(door("test:door").build());

        registration.close();
        registration.close();

        assertTrue(registry.snapshot().getDefinitions().isEmpty());
    }

    @Test
    void theSameIdReplacesTheEarlierDefinition() {
        TransportDefinition first = door("test:door").build();
        TransportDefinition second = door("test:door").duration(5).build();

        registry.register(first);
        registry.register(second);

        assertEquals(List.of(second), registry.snapshot().getDefinitions());
    }

    @Test
    void removingAReplacementDoesNotBringBackWhatItReplaced() {
        registry.register(door("test:door").build());
        TransportRegistry.Registration replacement = registry.register(door("test:door").duration(5).build());

        replacement.close();

        assertTrue(registry.snapshot().getDefinitions().isEmpty());
    }

    @Test
    void aRegistryStartsWithTheDefinitionsItIsGiven() {
        TransportDefinition first = door("bundled:a").build();
        TransportDefinition second = door("bundled:b").replaces(true).build();

        TransportRegistry loaded = new TransportRegistry(List.of(first, second));

        assertEquals(List.of(first, second), loaded.snapshot().getDefinitions());
        assertEquals(1, loaded.snapshot().getDisabledEdges().size());
        assertTrue(loaded.unregister("bundled:a"));
    }

    @Test
    void aStaleHandleDoesNotRemoveTheDefinitionThatReplacedIt() {
        TransportDefinition replacement = door("test:door").duration(5).build();
        TransportRegistry.Registration stale = registry.register(door("test:door").build());
        registry.register(replacement);

        stale.close();

        assertEquals(List.of(replacement), registry.snapshot().getDefinitions());
    }

    @Test
    void unregisterRemovesByIdAndReportsWhetherAnythingWasThere() {
        registry.register(door("test:door").build());

        assertTrue(registry.unregister("test:door"));
        assertFalse(registry.unregister("test:door"));
        assertFalse(registry.unregister(null));
    }

    @Test
    void unregisterAllRemovesOneOwnersTransportsOnly() {
        registry.register(door("first:a").build());
        registry.register(door("first:b").build());
        registry.register(door("second:a").build());

        assertEquals(2, registry.unregisterAll("first"));

        assertEquals(List.of("second:a"), ids(registry.snapshot().getDefinitions()));
        assertEquals(0, registry.unregisterAll("first"));
    }

    @Test
    void aRegisteredShapeHandlerTakesOverUntilItIsWithdrawn() {
        TransportHandler canoes = context -> true;

        assertNull(registry.snapshot().handlerFor(TransportShape.CANOE));
        TransportRegistry.Registration registration = registry.registerHandler(TransportShape.CANOE, canoes);

        assertSame(canoes, registry.snapshot().handlerFor(TransportShape.CANOE));
        assertNull(registry.snapshot().handlerFor(TransportShape.SINGLE_CLICK));
        assertNull(registry.snapshot().handlerFor(null));

        registration.close();
        assertNull(registry.snapshot().handlerFor(TransportShape.CANOE));
    }

    @Test
    void theLatestShapeHandlerWinsAndWithdrawingItRestoresTheOneBefore() {
        TransportHandler first = context -> true;
        TransportHandler second = context -> false;
        registry.registerHandler(TransportShape.CANOE, first);
        TransportRegistry.Registration latest = registry.registerHandler(TransportShape.CANOE, second);

        assertSame(second, registry.snapshot().handlerFor(TransportShape.CANOE));

        latest.close();
        assertSame(first, registry.snapshot().handlerFor(TransportShape.CANOE));
    }

    @Test
    void aShapeHandlerRegistersNoTransport() {
        registry.registerHandler(TransportShape.CANOE, context -> true);

        assertTrue(registry.snapshot().getDefinitions().isEmpty());
        assertTrue(registry.snapshot().getDisabledEdges().isEmpty());
    }

    @Test
    void aDisabledEdgeStaysHiddenUntilEveryoneWhoDisabledItLetsGo() {
        TransportRegistry.Registration first = registry.disableEdge(ORIGIN, DESTINATION);
        TransportRegistry.Registration second = registry.disableEdge(ORIGIN, DESTINATION);
        TransportRegistry.Edge edge = new TransportRegistry.Edge(ORIGIN, DESTINATION);

        first.close();
        first.close();
        assertTrue(registry.snapshot().getDisabledEdges().contains(edge));

        second.close();
        assertTrue(registry.snapshot().getDisabledEdges().isEmpty());
    }

    @Test
    void aReplacingDefinitionHidesTheDatasetEdgeInEachDirectionItCovers() {
        registry.register(door("test:one-way").replaces(true).build());
        assertEquals(1, registry.snapshot().getDisabledEdges().size());
        assertTrue(registry.snapshot().getDisabledEdges().contains(new TransportRegistry.Edge(ORIGIN, DESTINATION)));

        registry.register(door("test:both-ways").replaces(true).bidirectional(true).build());
        assertTrue(registry.snapshot().getDisabledEdges().contains(new TransportRegistry.Edge(DESTINATION, ORIGIN)));
    }

    @Test
    void nullRegistrationsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
        assertThrows(IllegalArgumentException.class, () -> registry.registerHandler(null, context -> true));
        assertThrows(IllegalArgumentException.class, () -> registry.registerHandler(TransportShape.CANOE, null));
        assertThrows(IllegalArgumentException.class, () -> registry.disableEdge(null, DESTINATION));
    }

    @Test
    void aSearchMayOnlyUseTransportsWhoseCategoryIsTurnedOn() {
        registry.register(door("test:door").build());
        registry.register(door("test:boat").category(TransportCategory.BOAT).build());

        GlobalPathfinderConfig noBoats = GlobalPathfinderConfig.builder().useBoats(false).build();

        assertEquals(List.of("test:door", "test:boat"), ids(registry.snapshot().usable(DEFAULTS, NOTHING_HELD)));
        assertEquals(List.of("test:door"), ids(registry.snapshot().usable(noBoats, NOTHING_HELD)));
    }

    @Test
    void aSearchMayOnlyUseTransportsWhoseRequirementsAreMet() {
        registry.register(door("test:shortcut")
                .requirements(DefinitionRequirements.builder().skill(Skill.AGILITY, 50).build())
                .build());

        int[] levels = new int[Skill.values().length];
        levels[Skill.AGILITY.ordinal()] = 50;
        TransportRequirements.PlayerState agile = TransportRequirements.PlayerState.builder().skillLevels(levels).build();

        assertTrue(registry.snapshot().usable(DEFAULTS, NOTHING_HELD).isEmpty());
        assertEquals(List.of("test:shortcut"), ids(registry.snapshot().usable(DEFAULTS, agile)));
    }

    @Test
    void aSearchMayOnlyUseTransportsWhoseOwnConditionHolds() {
        boolean[] open = {false};
        registry.register(door("test:conditional").availableWhen(() -> open[0]).build());

        assertTrue(registry.snapshot().usable(DEFAULTS, NOTHING_HELD).isEmpty());

        open[0] = true;
        assertEquals(List.of("test:conditional"), ids(registry.snapshot().usable(DEFAULTS, NOTHING_HELD)));
    }

    @Test
    void aConditionThatThrowsHidesItsOwnTransportAndNoOther() {
        registry.register(door("test:broken").availableWhen(() -> {
            throw new IllegalStateException("plugin fault");
        }).build());
        registry.register(door("test:door").build());

        assertEquals(List.of("test:door"), ids(registry.snapshot().usable(DEFAULTS, NOTHING_HELD)));
    }

    @Test
    void requirementsAreListedOncePerDefinitionForASingleStateRead() {
        DefinitionRequirements agility = DefinitionRequirements.builder().skill(Skill.AGILITY, 50).build();
        registry.register(door("test:shortcut").requirements(agility).build());
        registry.register(door("test:door").build());

        assertEquals(List.of(agility, DefinitionRequirements.NONE), registry.snapshot().requirements());
    }

    private static TransportDefinition.TransportDefinitionBuilder door(String id) {
        return TransportDefinition.object(id, ORIGIN, DESTINATION, 1535, "Open");
    }

    private static List<String> ids(List<TransportDefinition> definitions) {
        return definitions.stream().map(TransportDefinition::getId).collect(Collectors.toList());
    }
}
