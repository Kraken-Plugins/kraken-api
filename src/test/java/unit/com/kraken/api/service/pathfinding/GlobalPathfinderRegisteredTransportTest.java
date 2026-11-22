package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.walker.transport.PlayerStateReader;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.TestRegistries;
import com.kraken.api.service.walker.transport.registry.TransportDataset;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs the real search over the shipped collision map to show that a transport added to the
 * registry is planned through, and that the route says which definition it used.
 *
 * <p>The two tiles sit in different parts of the world and the registry starts with no dataset, so
 * the only way from one to the other without walking is the transport a test puts there.</p>
 */
class GlobalPathfinderRegisteredTransportTest {

    private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);
    private static final WorldPoint VARROCK = new WorldPoint(3213, 3424, 0);

    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final PlayerStateReader playerStateReader = mock(PlayerStateReader.class);

    private TransportRegistry registry = TestRegistries.empty();
    private GlobalPathfinder pathfinder;

    @BeforeEach
    void setup() throws Exception {
        when(ctx.runOnClientThread(any(Callable.class)))
                .thenAnswer(call -> ((Callable<?>) call.getArgument(0)).call());
        when(playerStateReader.read(any(PlayerStateReader.Needs.class)))
                .thenReturn(TransportRequirements.PlayerState.builder().build());
        pathfinder = pathfinderOver(registry);
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
        assertSame(carriage.getCategory(), usage.getType());
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
    void aTransportOfTheDatasetIsPlannedThroughByARealSearch() throws Exception {
        TransportDefinition stairs = TransportDataset.bundled().stream()
                .filter(definition -> !definition.isUsableAnywhere())
                .filter(definition -> definition.getRequirements().isEmpty())
                .filter(definition -> definition.getOrigin().getPlane() != definition.getDestination().getPlane())
                .findFirst()
                .orElseThrow();
        pathfinder = pathfinderOver(TestRegistries.over(List.of(stairs)));

        GlobalPathfinder.PathResult result =
                pathfinder.findPathResult(stairs.getOrigin(), stairs.getDestination(), limits());

        assertTrue(result.isComplete(), stairs.toString());
        assertEquals(1, result.getTransports().size(), stairs.toString());
        assertSame(stairs, result.getTransports().get(0).getDefinition());
    }

    @Test
    void aRegisteredTransportIsOperatedInPlaceOfTheDatasetTransportBetweenTheSameTiles() throws Exception {
        TransportDefinition datasetCarriage = TransportDefinition
                .npc("kraken:upstream-carriage", LUMBRIDGE, VARROCK, "Coachman", "Travel").build();
        TransportDefinition registered = TransportDefinition
                .npc("test:carriage", LUMBRIDGE, VARROCK, "Driver", "Ride").build();
        registry = TestRegistries.over(List.of(datasetCarriage));
        pathfinder = pathfinderOver(registry);

        assertSame(datasetCarriage, onlyTransport(pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits())));

        registry.register(registered);

        assertSame(registered, onlyTransport(pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits())));
    }

    @Test
    void aDatasetTransportThatIsReplacedOrDisabledIsHiddenFromTheSearch() throws Exception {
        TransportDefinition datasetCarriage = TransportDefinition
                .npc("kraken:upstream-carriage", LUMBRIDGE, VARROCK, "Coachman", "Travel").build();
        registry = TestRegistries.over(List.of(datasetCarriage));
        pathfinder = pathfinderOver(registry);

        TransportRegistry.Registration disabled = registry.disableEdge(LUMBRIDGE, VARROCK);
        assertTrue(pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits()).getTransports().isEmpty());
        disabled.close();
        assertSame(datasetCarriage, onlyTransport(pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits())));

        // A replacement the player cannot use still hides the edge it replaces, which is how a
        // toll gate keeps a player without coins from being sent through the free dataset gate.
        registry.register(TransportDefinition.npc("test:toll-carriage", LUMBRIDGE, VARROCK, "Coachman", "Pay")
                .replaces(true)
                .requirements(DefinitionRequirements.builder()
                        .item(DefinitionRequirements.RequiredItem.of(995, 10))
                        .build())
                .build());

        assertTrue(pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits()).getTransports().isEmpty());
    }

    @Test
    void aRegisteredTeleportIsPlannedFromWhereverThePlayerStandsAndNamedOnTheRoute() {
        TransportDefinition tablet = TransportDefinition.itemTeleport("test:varrock-tab", VARROCK, 8007)
                .duration(4)
                .build();
        registry.register(tablet);
        when(playerStateReader.read(any(PlayerStateReader.Needs.class)))
                .thenReturn(TransportRequirements.PlayerState.builder().item(8007, 1).build());

        GlobalPathfinder.PathResult result = pathfinder.findPathResult(LUMBRIDGE, VARROCK, limits());

        assertTrue(result.isComplete());
        assertEquals(List.of(LUMBRIDGE, VARROCK), result.getPath());
        assertEquals(1, result.getTransports().size());

        GlobalPathfinder.TransportUsage usage = result.getTransports().get(0);
        assertSame(tablet, usage.getDefinition());
        assertTrue(usage.isTeleport(), "the walker must treat it as a teleport, with no origin to walk to");
        assertEquals(LUMBRIDGE, usage.getOrigin());
        assertEquals(VARROCK, usage.getDestination());
        assertEquals(4, usage.getDuration());
    }

    @Test
    void aRegisteredTeleportIsNotUsedForAShortWalkItLeadsAwayFrom() {
        WorldPoint nearLumbridge = new WorldPoint(3222, 3230, 0);
        registry.register(TransportDefinition.itemTeleport("test:varrock-tab", VARROCK, 8007).build());
        when(playerStateReader.read(any(PlayerStateReader.Needs.class)))
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

    private static TransportDefinition onlyTransport(GlobalPathfinder.PathResult result) {
        assertEquals(1, result.getTransports().size());
        return result.getTransports().get(0).getDefinition();
    }

    private static GlobalPathfinderConfig limits() {
        return GlobalPathfinderConfig.builder().maxSearchNodes(200_000).build();
    }

    private GlobalPathfinder pathfinderOver(TransportRegistry transports) throws Exception {
        GlobalPathfinder target = new GlobalPathfinder();
        inject(target, "ctx", ctx);
        inject(target, "client", client);
        inject(target, "transportPlanner",
                new TransportPlanner(transports, playerStateReader, mock(MagicService.class), client));
        return target;
    }

    private static void inject(GlobalPathfinder target, String name, Object value) throws Exception {
        Field field = GlobalPathfinder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
