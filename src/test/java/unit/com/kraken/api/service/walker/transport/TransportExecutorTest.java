package com.kraken.api.service.walker.transport;

import com.kraken.api.core.script.ScriptStoppedException;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.walker.transport.handler.CanoeHandler;
import com.kraken.api.service.walker.transport.handler.ClearThenWalkHandler;
import com.kraken.api.service.walker.transport.handler.DialogueTransportHandler;
import com.kraken.api.service.walker.transport.handler.FairyRingHandler;
import com.kraken.api.service.walker.transport.handler.GroupingTeleportHandler;
import com.kraken.api.service.walker.transport.handler.HubDialogueHandler;
import com.kraken.api.service.walker.transport.handler.HubResumePauseHandler;
import com.kraken.api.service.walker.transport.handler.ItemTeleportHandler;
import com.kraken.api.service.walker.transport.handler.ObjectTransportHandler;
import com.kraken.api.service.walker.transport.handler.SpellTeleportHandler;
import com.kraken.api.service.walker.transport.handler.WalkTransportHandler;
import com.kraken.api.service.walker.transport.handler.WidgetSelectHandler;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.TestRegistries;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers which handler operates a transport, and that a transport's requirements stop the click
 * whether it comes from the dataset or was registered.
 */
class TransportExecutorTest {

    private static final WorldPoint ORIGIN = new WorldPoint(3200, 3200, 0);
    private static final WorldPoint DESTINATION = new WorldPoint(3300, 3300, 0);
    private static final TransportContext CONTEXT = TransportContext.builder().build();

    private static final TransportDefinition DATASET_SHORTCUT = TransportDefinition
            .object("kraken:upstream-shortcut", ORIGIN, DESTINATION, 1535, "Climb")
            .requirements(DefinitionRequirements.builder().skill(Skill.AGILITY, 50).build())
            .build();

    private final TransportRegistry registry = TestRegistries.over(List.of(DATASET_SHORTCUT));
    private final ObjectTransportHandler objectHandler = mock(ObjectTransportHandler.class);
    private final ClearThenWalkHandler clearThenWalkHandler = mock(ClearThenWalkHandler.class);
    private final WalkTransportHandler walkHandler = mock(WalkTransportHandler.class);
    private final ItemTeleportHandler itemHandler = mock(ItemTeleportHandler.class);
    private final CanoeHandler canoeHandler = mock(CanoeHandler.class);
    private final GroupingTeleportHandler groupingTeleportHandler = mock(GroupingTeleportHandler.class);
    private final PlayerStateReader playerStateReader = mock(PlayerStateReader.class);
    private final TileService tileService = mock(TileService.class);

    private final TransportExecutor executor = new TransportExecutor(
            registry,
            objectHandler,
            clearThenWalkHandler,
            walkHandler,
            mock(DialogueTransportHandler.class),
            mock(HubDialogueHandler.class),
            mock(HubResumePauseHandler.class),
            mock(FairyRingHandler.class),
            itemHandler,
            mock(SpellTeleportHandler.class),
            mock(WidgetSelectHandler.class),
            canoeHandler,
            groupingTeleportHandler);

    @BeforeEach
    void injectCollaborators() throws Exception {
        Field field = TransportExecutor.class.getDeclaredField("playerStateReader");
        field.setAccessible(true);
        field.set(executor, playerStateReader);

        Field tiles = TransportExecutor.class.getDeclaredField("tileService");
        tiles.setAccessible(true);
        tiles.set(executor, tileService);
    }

    @Test
    void aShapeIsOperatedByItsBuiltInHandler() {
        assertSame(objectHandler, executor.resolveHandler(null, TransportShape.SINGLE_CLICK));
        assertSame(walkHandler, executor.resolveHandler(null, TransportShape.WALK));
        assertSame(canoeHandler, executor.resolveHandler(null, TransportShape.CANOE));
        assertSame(groupingTeleportHandler, executor.resolveHandler(null, TransportShape.GROUPING_TELEPORT));
        assertNull(executor.resolveHandler(null, null));
    }

    @Test
    void everyShapeHasABuiltInHandler() {
        for (TransportShape shape : TransportShape.values()) {
            assertNotNull(executor.resolveHandler(null, shape), shape + " has no handler");
        }
    }

    @Test
    void aDatasetTransportWhoseRequirementsAreUnmetIsNotClicked() {
        when(playerStateReader.read(anyCollection()))
                .thenReturn(TransportRequirements.PlayerState.builder().build());

        TransportExecutor.Result result = executor.execute(usage(DATASET_SHORTCUT));

        assertFalse(result.isCrossed());
        assertTrue(result.isRequirementsUnmet(), result.getReason());
        assertTrue(result.getReason().contains("Agility"), result.getReason());
        verify(objectHandler, never()).execute(any());
    }

    @Test
    void aRouteStepThatCarriesNoDefinitionCannotBeOperated() {
        GlobalPathfinder.TransportUsage bare = new GlobalPathfinder.TransportUsage(
                0, ORIGIN, DESTINATION, null, null, "Open Door 9398", false, 1);

        TransportExecutor.Result result = executor.execute(bare);

        assertFalse(result.isCrossed());
        assertFalse(result.isSupported());
    }

    @Test
    void aHandlerRegisteredForAShapeReplacesTheBuiltInOneUntilWithdrawn() {
        TransportHandler canoes = context -> true;
        TransportRegistry.Registration registration = registry.registerHandler(TransportShape.CANOE, canoes);

        assertSame(canoes, executor.resolveHandler(null, TransportShape.CANOE));
        assertSame(objectHandler, executor.resolveHandler(null, TransportShape.SINGLE_CLICK));

        registration.close();
        assertSame(canoeHandler, executor.resolveHandler(null, TransportShape.CANOE));
    }

    @Test
    void aDeclarativeDefinitionUsesTheHandlerForItsShape() {
        TransportDefinition door = TransportDefinition.object("test:door", ORIGIN, DESTINATION, 1535, "Open").build();

        assertSame(objectHandler, executor.resolveHandler(door, door.getShape()));
    }

    @Test
    void anObstacleThatIsClearedThenWalkedThroughHasItsOwnHandler() {
        TransportDefinition web = TransportDefinition.object("test:web", ORIGIN, DESTINATION, 733, "Slash")
                .shape(TransportShape.CLEAR_THEN_WALK)
                .build();

        assertSame(clearThenWalkHandler, executor.resolveHandler(web, web.getShape()));
        assertSame(clearThenWalkHandler, executor.resolveHandler(null, TransportShape.CLEAR_THEN_WALK));
        assertTrue(TransportDefinition.DECLARATIVE_SHAPES.contains(TransportShape.CLEAR_THEN_WALK),
                "a data file can hold such a row, since the handler works from the row's click target");
    }

    @Test
    void anOpenDoorNeedsNoClickButAnOpenWebIsStillHandedToItsHandler() {
        WorldPoint here = new WorldPoint(3200, 3200, 0);
        WorldPoint next = new WorldPoint(3200, 3201, 0);
        when(tileService.isTileReachable(next)).thenReturn(true);

        assertTrue(executor.needsNoOperating(TransportShape.SINGLE_CLICK, here, next, false));
        assertFalse(executor.needsNoOperating(TransportShape.CLEAR_THEN_WALK, here, next, false),
                "the handler walks the player through; skipping it would leave them where they stand");
        assertFalse(executor.needsNoOperating(TransportShape.WALK, here, next, false),
                "a step that is only walked is walked by its handler");
        assertFalse(executor.needsNoOperating(TransportShape.SINGLE_CLICK, here, next, true),
                "a teleport is always operated");
        assertFalse(executor.needsNoOperating(TransportShape.SINGLE_CLICK, here, DESTINATION, false),
                "a destination further than the next tile is not an open door");
    }

    @Test
    void aDefinitionsOwnHandlerWinsOverEveryHandlerForItsShape() {
        TransportHandler own = context -> true;
        registry.registerHandler(TransportShape.SINGLE_CLICK, context -> false);
        TransportDefinition custom = TransportDefinition.object("test:custom", ORIGIN, DESTINATION, 1535, "Open")
                .handler(own)
                .build();

        assertSame(own, executor.resolveHandler(custom, custom.getShape()));
    }

    @Test
    void aRegisteredTransportWhoseRequirementsAreUnmetIsNotClicked() {
        TransportDefinition shortcut = TransportDefinition.object("test:shortcut", ORIGIN, DESTINATION, 1535, "Climb")
                .requirements(DefinitionRequirements.builder().skill(Skill.AGILITY, 50).build())
                .build();
        registry.register(shortcut);
        when(playerStateReader.read(anyCollection()))
                .thenReturn(TransportRequirements.PlayerState.builder().build());

        TransportExecutor.Result result = executor.execute(usage(shortcut));

        assertFalse(result.isCrossed());
        assertTrue(result.isRequirementsUnmet());
        assertTrue(result.getReason().contains("test:shortcut"), result.getReason());
        assertTrue(result.getReason().contains("Agility"), result.getReason());
        verify(objectHandler, never()).execute(any());
    }

    @Test
    void aRegisteredTeleportIsRefusedAboveItsWildernessLevel() {
        TransportDefinition tablet = TransportDefinition.itemTeleport("test:tab", DESTINATION, 8007).build();
        TransportDefinition glory = TransportDefinition.itemTeleport("test:glory", DESTINATION, 1712)
                .maxWildernessLevel(30)
                .build();

        assertTrue(TransportExecutor.usableAtWildernessLevel(tablet, 0));
        assertTrue(TransportExecutor.usableAtWildernessLevel(tablet, 20));
        assertFalse(TransportExecutor.usableAtWildernessLevel(tablet, 30));
        assertTrue(TransportExecutor.usableAtWildernessLevel(glory, 30));
        assertFalse(TransportExecutor.usableAtWildernessLevel(glory, 31));
    }

    @Test
    void aRegisteredItemTeleportIsOperatedByTheItemHandler() {
        TransportDefinition tablet = TransportDefinition.itemTeleport("test:tab", DESTINATION, 8007).build();

        assertSame(itemHandler, executor.resolveHandler(tablet, tablet.getShape()));
    }

    @Test
    void aTransportUnregisteredAfterItsRouteWasPlannedIsNotOperated() {
        TransportHandler own = mock(TransportHandler.class);
        TransportDefinition custom = TransportDefinition.object("test:custom", ORIGIN, DESTINATION, 1535, "Open")
                .handler(own)
                .build();
        TransportRegistry.Registration registration = registry.register(custom);
        GlobalPathfinder.TransportUsage planned = usage(custom);

        registration.close();
        TransportExecutor.Result result = executor.execute(planned);

        assertFalse(result.isCrossed());
        assertTrue(result.getReason().contains("unregistered"), result.getReason());
        verify(own, never()).execute(any());
    }

    @Test
    void aHandlerThatOperatesTheTransportLeavesNothingToReport() {
        assertNull(executor.operate(context -> true, CONTEXT, "test:door", "Open 1535"));
    }

    @Test
    void aRegisteredHandlerThatThrowsFailsTheCrossingInsteadOfTheWalk() {
        TransportExecutor.Result result = executor.operate(context -> {
            throw new IllegalStateException("plugin fault");
        }, CONTEXT, "test:door", "Open 1535");

        assertFalse(result.isCrossed());
        assertTrue(result.isSupported());
        assertTrue(result.getReason().contains("test:door") && result.getReason().contains("plugin fault"),
                result.getReason());
    }

    @Test
    void aHandlerThatSaysWhyItFailedHasItsReasonReported() {
        TransportContext context = TransportContext.builder().build();

        TransportExecutor.Result explained = executor.operate(
                crossing -> crossing.fail("operated Slash Web 733 5 times but the way never opened"),
                context, "kraken:web", "Slash Web 733");
        TransportExecutor.Result unexplained = executor.operate(crossing -> false, CONTEXT, "kraken:web", "Slash Web 733");

        assertFalse(explained.isCrossed());
        assertTrue(explained.isSupported());
        assertTrue(explained.getReason().equals("kraken:web: operated Slash Web 733 5 times but the way never opened"),
                explained.getReason());
        assertTrue(unexplained.getReason().equals("could not operate kraken:web (Slash Web 733)"), unexplained.getReason());
    }

    @Test
    void aStoppingScriptUnwindsThroughARegisteredHandler() {
        assertThrows(ScriptStoppedException.class, () -> executor.operate(context -> {
            throw new ScriptStoppedException("stopped");
        }, CONTEXT, "test:door", "Open 1535"));
    }

    @Test
    void aBuiltInHandlerThatThrowsIsNotSwallowed() {
        when(objectHandler.execute(any())).thenThrow(new IllegalStateException("built in fault"));

        assertThrows(IllegalStateException.class,
                () -> executor.operate(objectHandler, CONTEXT, "TRANSPORT", "Open 1535"));
    }

    @Test
    void aBuiltInHandlerThatRefusesIsAFailedCrossingAndNotAnUnsupportedOne() {
        when(canoeHandler.execute(any())).thenReturn(false);

        TransportExecutor.Result builtIn = executor.operate(canoeHandler, CONTEXT, "CANOE", "no description");
        TransportExecutor.Result registered = executor.operate(context -> false, CONTEXT, "CANOE", "no description");

        for (TransportExecutor.Result result : List.of(builtIn, registered)) {
            assertTrue(result.isSupported());
            assertFalse(result.isCrossed());
            assertTrue(result.getReason().contains("could not operate CANOE"), result.getReason());
        }
    }

    private static GlobalPathfinder.TransportUsage usage(TransportDefinition definition) {
        return new GlobalPathfinder.TransportUsage(
                0,
                definition.getOrigin(),
                definition.getDestination(),
                definition.getCategory(),
                definition.getDisplayInfo(),
                definition.getObjectInfo(),
                false,
                definition.getDuration(),
                definition);
    }
}
