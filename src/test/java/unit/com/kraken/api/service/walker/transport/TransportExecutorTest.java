package com.kraken.api.service.walker.transport;

import com.kraken.api.core.script.ScriptStoppedException;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.walker.transport.handler.DialogueTransportHandler;
import com.kraken.api.service.walker.transport.handler.FairyRingHandler;
import com.kraken.api.service.walker.transport.handler.HubDialogueHandler;
import com.kraken.api.service.walker.transport.handler.HubResumePauseHandler;
import com.kraken.api.service.walker.transport.handler.ItemTeleportHandler;
import com.kraken.api.service.walker.transport.handler.ObjectTransportHandler;
import com.kraken.api.service.walker.transport.handler.SpellTeleportHandler;
import com.kraken.api.service.walker.transport.handler.UnsupportedTransportHandler;
import com.kraken.api.service.walker.transport.handler.WidgetSelectHandler;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shortestpath.WorldPointUtil;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportType;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Covers which handler operates a transport once the registry is involved, and that a registered
 * transport's own requirements stop the click.
 */
class TransportExecutorTest {

    private static final WorldPoint ORIGIN = new WorldPoint(3200, 3200, 0);
    private static final WorldPoint DESTINATION = new WorldPoint(3300, 3300, 0);
    private static final TransportContext CONTEXT = TransportContext.builder().build();

    private final TransportRegistry registry = new TransportRegistry();
    private final ObjectTransportHandler objectHandler = mock(ObjectTransportHandler.class);
    private final UnsupportedTransportHandler unsupportedHandler = mock(UnsupportedTransportHandler.class);
    private final PlayerStateReader playerStateReader = mock(PlayerStateReader.class);

    private final TransportExecutor executor = new TransportExecutor(
            registry,
            objectHandler,
            mock(DialogueTransportHandler.class),
            mock(HubDialogueHandler.class),
            mock(HubResumePauseHandler.class),
            mock(FairyRingHandler.class),
            mock(ItemTeleportHandler.class),
            mock(SpellTeleportHandler.class),
            mock(WidgetSelectHandler.class),
            unsupportedHandler);

    @BeforeEach
    void injectCollaborators() throws Exception {
        Field field = TransportExecutor.class.getDeclaredField("playerStateReader");
        field.setAccessible(true);
        field.set(executor, playerStateReader);
    }

    @Test
    void aDatasetTransportUsesTheBuiltInHandlerForItsShape() {
        assertSame(objectHandler, executor.resolveHandler(null, TransportShape.SINGLE_CLICK));
        assertSame(unsupportedHandler, executor.resolveHandler(null, TransportShape.CANOE));
        assertNull(executor.resolveHandler(null, null));
    }

    @Test
    void aHandlerRegisteredForAShapeReplacesTheBuiltInOneUntilWithdrawn() {
        TransportHandler canoes = context -> true;
        TransportRegistry.Registration registration = registry.registerHandler(TransportShape.CANOE, canoes);

        assertSame(canoes, executor.resolveHandler(null, TransportShape.CANOE));
        assertSame(objectHandler, executor.resolveHandler(null, TransportShape.SINGLE_CLICK));

        registration.close();
        assertSame(unsupportedHandler, executor.resolveHandler(null, TransportShape.CANOE));
    }

    @Test
    void aDeclarativeDefinitionUsesTheHandlerForItsShape() {
        TransportDefinition door = TransportDefinition.object("test:door", ORIGIN, DESTINATION, 1535, "Open").build();

        assertSame(objectHandler, executor.resolveHandler(door, door.getShape()));
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
    void onlyTheBuiltInPlaceholderReportsATransportAsUnsupported() {
        TransportExecutor.Result builtIn = executor.operate(unsupportedHandler, CONTEXT, "CANOE", "no description");
        TransportExecutor.Result registered = executor.operate(context -> false, CONTEXT, "CANOE", "no description");

        assertFalse(builtIn.isSupported());
        assertTrue(registered.isSupported());
        assertFalse(registered.isCrossed());
        assertTrue(registered.getReason().contains("could not operate CANOE"), registered.getReason());
    }

    private static GlobalPathfinder.TransportUsage usage(TransportDefinition definition) {
        Transport edge = new Transport.TransportBuilder()
                .type(TransportType.TRANSPORT)
                .origin(WorldPointUtil.packWorldPoint(definition.getOrigin()))
                .destination(WorldPointUtil.packWorldPoint(definition.getDestination()))
                .build();

        return new GlobalPathfinder.TransportUsage(
                0,
                definition.getOrigin(),
                definition.getDestination(),
                edge.getType(),
                definition.getDisplayInfo(),
                definition.getObjectInfo(),
                false,
                definition.getDuration(),
                edge,
                definition);
    }
}
