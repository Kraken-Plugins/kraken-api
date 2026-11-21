package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import com.kraken.api.service.walker.transport.PlayerStateReader;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.ShortestPathBridge;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.pathfinder.CollisionMap;
import shortestpath.pathfinder.PathfinderConfig;
import shortestpath.pathfinder.SplitFlagMap;
import shortestpath.transport.Transport;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs the real search over the shipped collision map to show that a transport added to the
 * registry is planned through, and that the route says which definition it used.
 *
 * <p>The two tiles sit in different parts of the world with no dataset transports in the search, so
 * the only way from one to the other is the registered edge.</p>
 */
class GlobalPathfinderRegisteredTransportTest {

    private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);
    private static final WorldPoint VARROCK = new WorldPoint(3213, 3424, 0);

    private static SplitFlagMap collisionData;

    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final PlayerStateReader playerStateReader = mock(PlayerStateReader.class);
    private final TransportRegistry registry = new TransportRegistry();

    private final GlobalPathfinder pathfinder = new GlobalPathfinder() {
        @Override
        PathfinderConfig createRequestConfig(GlobalPathfinderConfig config) {
            PathfinderConfig request = mock(PathfinderConfig.class);
            when(request.getCalculationCutoffMillis()).thenReturn(10000L);
            when(request.getTransportsPacked(false)).thenReturn(new PrimitiveIntHashMap<>(8));
            when(request.getTransportsPacked(true)).thenReturn(new PrimitiveIntHashMap<>(8));
            when(request.getUsableTeleports(anyBoolean())).thenReturn(new Transport[0]);
            when(request.getMap()).thenReturn(new CollisionMap(collisionData));
            return request;
        }
    };

    @BeforeAll
    static void loadCollisionMap() {
        collisionData = SplitFlagMap.fromResources();
    }

    @BeforeEach
    void setup() throws Exception {
        inject("ctx", ctx);
        inject("client", client);
        inject("transportBridge", new ShortestPathBridge(registry, playerStateReader));
        // Start from an empty registry so the transports bundled with the API cannot change a route.
        for (TransportDefinition bundled : registry.snapshot().getDefinitions()) {
            registry.unregister(bundled.getId());
        }
        when(client.getIntStack()).thenReturn(new int[]{1});
        when(ctx.runOnClientThread(any(Callable.class)))
                .thenAnswer(call -> ((Callable<?>) call.getArgument(0)).call());
        when(playerStateReader.read(anyCollection()))
                .thenReturn(TransportRequirements.PlayerState.builder().build());
    }

    @Test
    void aRegisteredTransportIsPlannedThroughAndNamedOnTheRoute() {
        TransportDefinition carriage = TransportDefinition
                .npc("test:carriage", LUMBRIDGE, VARROCK, "Coachman", "Travel")
                .duration(3)
                .build();
        registry.register(carriage);

        GlobalPathfinder.PathResult result = pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits());

        assertTrue(result.isComplete());
        assertEquals(List.of(LUMBRIDGE, VARROCK), result.getPath());
        assertEquals(1, result.getTransports().size());

        GlobalPathfinder.TransportUsage usage = result.getTransports().get(0);
        assertSame(carriage, usage.getDefinition());
        assertEquals(LUMBRIDGE, usage.getOrigin());
        assertEquals(VARROCK, usage.getDestination());
        assertEquals("Travel Coachman", usage.getObjectInfo());
        assertEquals(3, usage.getDuration());
        assertNotNull(usage.getTransport());
    }

    @Test
    void aTransportRegisteredByAnyoneIsSeenByTheNextSearchAndGoneAfterItsHandleCloses() {
        TransportRegistry.Registration registration = registry.register(
                TransportDefinition.npc("other-plugin:carriage", LUMBRIDGE, VARROCK, "Coachman", "Travel").build());
        assertEquals(1, pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits()).getTransports().size());

        registration.close();

        GlobalPathfinder.PathResult walked = pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits());
        assertTrue(walked.getTransports().isEmpty());
        assertTrue(walked.getPath().size() > 2, "without the carriage the route is walked tile by tile");
    }

    @Test
    void aRegisteredTransportThePlayerCannotUseIsLeftOutOfTheRoute() {
        registry.register(TransportDefinition.npc("test:carriage", LUMBRIDGE, VARROCK, "Coachman", "Travel")
                .requirements(DefinitionRequirements.builder().skill(Skill.AGILITY, 99).build())
                .build());

        GlobalPathfinder.PathResult result = pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits());

        assertTrue(result.getTransports().isEmpty());
        assertTrue(result.getPath().size() > 2, "the search still ran and walked instead");
    }

    private static GlobalPathfinderConfig limits() {
        return GlobalPathfinderConfig.builder().maxSearchNodes(200_000).build();
    }

    private void inject(String name, Object value) throws Exception {
        Field field = GlobalPathfinder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(pathfinder, value);
    }
}
