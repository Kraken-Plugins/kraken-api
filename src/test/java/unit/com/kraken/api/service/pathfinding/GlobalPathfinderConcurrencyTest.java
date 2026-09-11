package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.PrimitiveIntList;
import shortestpath.WorldPointUtil;
import shortestpath.pathfinder.*;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GlobalPathfinderConcurrencyTest {
    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final AtomicInteger captures = new AtomicInteger();
    private Runnable duringExpansion = () -> {};
    private final GlobalPathfinder pathfinder = new GlobalPathfinder() {
        @Override
        PathfinderConfig createRequestConfig(GlobalPathfinderConfig config) {
            assertFalse(client.isClientThread());
            PathfinderConfig request = mock(PathfinderConfig.class);
            when(request.getCalculationCutoffMillis()).thenReturn(10000L);
            when(request.getTransportsPacked(anyBoolean())).thenReturn(new PrimitiveIntHashMap<>(1));
            CollisionMap map = mock(CollisionMap.class);
            when(map.getRegionPlaneCounts(anyInt())).thenReturn((byte) 4);
            when(request.getMap()).thenReturn(map);
            when(map.getNeighbors(anyInt(), any(), any(), anyInt(), anyBoolean(), any())).thenAnswer(call -> {
                duringExpansion.run();
                NodeGraph graph = call.getArgument(5);
                int previous = call.getArgument(0);
                WorldPoint position = WorldPointUtil.unpackWorldPoint(graph.packedPosition(previous));
                PrimitiveIntList neighbors = new PrimitiveIntList();
                neighbors.add(graph.createTile(WorldPointUtil.packWorldPoint(position.dx(1)), previous, false));
                return neighbors;
            });
            return request;
        }
    };
    private final WorldPoint source = new WorldPoint(3200, 3200, 0);

    @org.junit.jupiter.api.BeforeAll
    static void initializeGlobalMapExtents() {
        SplitFlagMap.fromResources();
    }

    @BeforeEach
    void setup() throws Exception {
        inject("ctx", ctx);
        inject("client", client);
        when(client.getIntStack()).thenReturn(new int[]{1});
        when(ctx.runOnClientThread(any(Callable.class))).thenAnswer(call -> {
            assertFalse(Thread.holdsLock(pathfinder), "capture must not hold the pathfinder monitor");
            captures.incrementAndGet();
            return ((Callable<?>) call.getArgument(0)).call();
        });
    }

    @Test
    void clientCallRejectsWhileWorkerIsWaitingForCapture() throws Exception {
        CountDownLatch queued = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(ctx.runOnClientThread(any(Callable.class))).thenAnswer(call -> {
            assertFalse(Thread.holdsLock(pathfinder));
            queued.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return ((Callable<?>) call.getArgument(0)).call();
        });
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<?> search = worker.submit(() -> pathfinder.findPathResult(source, source));
            assertTrue(queued.await(5, TimeUnit.SECONDS));
            when(client.isClientThread()).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> pathfinder.findPathResult(source, source));
            verify(client, never()).getLocalPlayer();
            when(client.isClientThread()).thenReturn(false);
            release.countDown();
            search.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            worker.shutdownNow();
        }
    }

    @Test
    void nodeLimitReturnsPartialRouteDespiteContinualHeuristicProgress() {
        GlobalPathfinder.PathResult result = pathfinder.findPathResult(source, source.dx(100),
                GlobalPathfinderConfig.builder().maxSearchNodes(4).build());
        assertFalse(result.isComplete());
        assertEquals(4, result.getPath().size());
        assertEquals(source.dx(3), result.getPath().get(3));
    }

    @Test
    void completeSearchAndConcurrentRequestsKeepTheirOwnInputs() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<GlobalPathfinder.PathResult> first = workers.submit(() -> pathfinder.findPathResult(source, source.dx(3)));
            Future<GlobalPathfinder.PathResult> second = workers.submit(() -> pathfinder.findPathResult(source.dx(10), source.dx(15)));
            assertEquals(4, first.get(5, TimeUnit.SECONDS).getPath().size());
            assertTrue(second.get(5, TimeUnit.SECONDS).isComplete());
            assertEquals(source.dx(10), second.get().getSource());
            assertEquals(2, captures.get());
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void interruptedRequestDoesNotCaptureClientState() {
        Thread.currentThread().interrupt();
        try {
            assertFalse(pathfinder.findPathResult(source, source.dx(1)).isComplete());
            assertEquals(0, captures.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void stoppingScriptCancelsAnExpandingSearch() throws Exception {
        CountDownLatch expanding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        duringExpansion = () -> {
            expanding.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        };
        java.util.concurrent.atomic.AtomicReference<GlobalPathfinder.PathResult> result =
                new java.util.concurrent.atomic.AtomicReference<>();
        com.kraken.api.core.script.Script script = new com.kraken.api.core.script.Script() {
            @Override
            public int loop() {
                result.set(pathfinder.findPathResult(source, source.dx(100)));
                return 0;
            }
        };
        Field eventBus = com.kraken.api.core.script.Script.class.getDeclaredField("eventBus");
        eventBus.setAccessible(true);
        eventBus.set(script, mock(net.runelite.client.eventbus.EventBus.class));
        try {
            script.start();
            script.onGameTick(new net.runelite.api.events.GameTick());
            assertTrue(expanding.await(5, TimeUnit.SECONDS));
            CompletionStage<Void> stopped = script.stopAsync();
            release.countDown();
            stopped.toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertNotNull(result.get());
            assertFalse(result.get().isComplete());
            assertEquals(1, result.get().getPath().size());
        } finally {
            release.countDown();
            script.stop();
            assertTrue(script.awaitStopped(5000));
        }
    }

    @Test
    void invalidHardLimitsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> pathfinder.findPathResult(source, source,
                GlobalPathfinderConfig.builder().maxSearchNodes(0).build()));
        assertThrows(IllegalArgumentException.class, () -> pathfinder.findPathResult(source, source,
                GlobalPathfinderConfig.builder().maxSearchMillis(0).build()));
    }

    private void inject(String name, Object value) throws Exception {
        Field field = GlobalPathfinder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(pathfinder, value);
    }
}
