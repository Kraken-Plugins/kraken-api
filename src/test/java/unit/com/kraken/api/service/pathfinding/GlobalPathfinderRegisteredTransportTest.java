package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.walker.transport.PlayerStateReader;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.registry.BundledTransports;
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
import shortestpath.ShortestPathConfig;
import shortestpath.TeleportationItem;
import shortestpath.pathfinder.CollisionMap;
import shortestpath.pathfinder.SplitFlagMap;
import shortestpath.transport.Transport;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs the real search over the shipped collision map to show that a transport added to the
 * registry is planned through, and that the route says which definition it used.
 *
 * <p>The two tiles sit in different parts of the world with no dataset transports in the search, so
 * the only way from one to the other without walking is the registered edge.</p>
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
        RequestPathfinderConfig createRequestConfig(GlobalPathfinderConfig config) {
            // The request starts with no dataset teleports and offers whatever it is handed, which
            // is what the real configuration does with an empty dataset.
            AtomicReference<Transport[]> teleports = new AtomicReference<>(new Transport[0]);
            RequestPathfinderConfig request = mock(RequestPathfinderConfig.class);
            when(request.getCalculationCutoffMillis()).thenReturn(10000L);
            when(request.getTransportsPacked(false)).thenReturn(new PrimitiveIntHashMap<>(8));
            when(request.getTransportsPacked(true)).thenReturn(new PrimitiveIntHashMap<>(8));
            doAnswer(call -> {
                teleports.set(call.getArgument(0));
                return null;
            }).when(request).addUsableTeleports(any());
            when(request.getUsableTeleports(anyBoolean())).thenAnswer(call -> teleports.get());
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
        inject("transportBridge", new ShortestPathBridge(registry, playerStateReader, mock(MagicService.class)));
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

    @Test
    void aTransportFromTheBundledFileIsPlannedThroughByARealSearch() {
        TransportDefinition bundled = BundledTransports.load().stream()
                .filter(definition -> !definition.isUsableAnywhere())
                .filter(definition -> definition.getRequirements().isEmpty())
                .filter(definition -> definition.getOrigin().getPlane() != definition.getDestination().getPlane())
                .findFirst()
                .orElseThrow();
        registry.register(bundled);

        GlobalPathfinder.PathResult result =
                pathfinder.findPathResult(bundled.getOrigin(), bundled.getDestination(), limits());

        assertTrue(result.isComplete(), bundled.toString());
        assertEquals(1, result.getTransports().size(), bundled.toString());
        assertSame(bundled, result.getTransports().get(0).getDefinition());
    }

    @Test
    void aRegisteredTeleportIsPlannedFromWhereverThePlayerStandsAndNamedOnTheRoute() {
        TransportDefinition tablet = TransportDefinition.itemTeleport("test:varrock-tab", VARROCK, 8007)
                .duration(4)
                .build();
        registry.register(tablet);
        when(playerStateReader.read(anyCollection()))
                .thenReturn(TransportRequirements.PlayerState.builder().item(8007, 1).build());

        GlobalPathfinder.PathResult result = pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits());

        assertTrue(result.isComplete());
        assertEquals(List.of(LUMBRIDGE, VARROCK), result.getPath());
        assertEquals(1, result.getTransports().size());

        GlobalPathfinder.TransportUsage usage = result.getTransports().get(0);
        assertSame(tablet, usage.getDefinition());
        assertTrue(usage.getType().isTeleport(), "the walker must treat it as a teleport, with no origin to walk to");
        assertEquals(LUMBRIDGE, usage.getOrigin());
        assertEquals(VARROCK, usage.getDestination());
        assertEquals(4, usage.getDuration());
    }

    @Test
    void aRegisteredTeleportReachesTheSearchThroughTheRealRequestConfiguration() throws Exception {
        // The other tests hand the search a stand in configuration. This one uses the real class,
        // with the planner's dataset loaded but none of it enabled, so the only teleport the search
        // can be offered is the registered one.
        ShortestPathConfig planningOptions = mock(ShortestPathConfig.class);
        when(planningOptions.calculationCutoff()).thenReturn(20);
        when(planningOptions.useTeleportationItems()).thenReturn(TeleportationItem.NONE);
        GlobalPathfinder realConfiguration = new GlobalPathfinder() {
            @Override
            RequestPathfinderConfig createRequestConfig(GlobalPathfinderConfig config) {
                return new RequestPathfinderConfig(client, planningOptions);
            }
        };
        inject(realConfiguration, "ctx", ctx);
        inject(realConfiguration, "client", client);
        inject(realConfiguration, "transportBridge",
                new ShortestPathBridge(registry, playerStateReader, mock(MagicService.class)));

        TransportDefinition tablet = TransportDefinition.itemTeleport("test:varrock-tab", VARROCK, 8007).build();
        registry.register(tablet);
        when(playerStateReader.read(anyCollection()))
                .thenReturn(TransportRequirements.PlayerState.builder().item(8007, 1).build());

        GlobalPathfinder.PathResult result = realConfiguration.findPathResult(LUMBRIDGE, VARROCK, limits());

        assertTrue(result.isComplete());
        assertEquals(List.of(LUMBRIDGE, VARROCK), result.getPath());
        assertEquals(1, result.getTransports().size());
        assertSame(tablet, result.getTransports().get(0).getDefinition());
    }

    @Test
    void aRegisteredTeleportIsNotUsedForAShortWalkItLeadsAwayFrom() {
        WorldPoint nearLumbridge = new WorldPoint(3222, 3230, 0);
        registry.register(TransportDefinition.itemTeleport("test:varrock-tab", VARROCK, 8007).build());
        when(playerStateReader.read(anyCollection()))
                .thenReturn(TransportRequirements.PlayerState.builder().item(8007, 1).build());

        GlobalPathfinder.PathResult result = pathfinder.findPathResult(nearLumbridge, LUMBRIDGE, limits());

        assertTrue(result.isComplete());
        assertTrue(result.getTransports().isEmpty(), "a short walk is not replaced by a teleport that leads away");
    }

    @Test
    void aRegisteredTeleportWhoseItemIsNotHeldIsLeftOutOfTheRoute() {
        registry.register(TransportDefinition.itemTeleport("test:varrock-tab", VARROCK, 8007).build());

        GlobalPathfinder.PathResult result = pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits());

        assertTrue(result.getTransports().isEmpty());
        assertTrue(result.getPath().size() > 2, "without the tablet the route is walked tile by tile");
    }

    private static GlobalPathfinderConfig limits() {
        return GlobalPathfinderConfig.builder().maxSearchNodes(200_000).build();
    }

    private void inject(String name, Object value) throws Exception {
        inject(pathfinder, name, value);
    }

    private static void inject(GlobalPathfinder target, String name, Object value) throws Exception {
        Field field = GlobalPathfinder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
