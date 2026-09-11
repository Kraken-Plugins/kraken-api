package unit.com.kraken.api.query;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.core.EntityView;
import com.kraken.api.core.ClientThreadException;
import com.kraken.api.query.npc.NpcEntity;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueryEvaluationThreadTest {
    private static final class Npcs extends AbstractQuery<NpcEntity, Npcs, NPC> {
        private final List<NpcEntity> entities;

        private Npcs(Context ctx, List<NpcEntity> entities) {
            super(ctx);
            this.entities = entities;
        }

        @Override
        protected Supplier<Stream<NpcEntity>> source() {
            return entities::parallelStream;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void callbacksRunOnClientThreadButReturnedViewsRemainLive() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Thread clientThread = executor.submit(Thread::currentThread).get(5, TimeUnit.SECONDS);
            Thread callerThread = Thread.currentThread();
            Context ctx = mock(Context.class);
            when(ctx.runOnClientThread(any(Callable.class))).thenAnswer(invocation -> {
                Callable<Object> callable = invocation.getArgument(0);
                return Thread.currentThread() == clientThread
                        ? callable.call() : executor.submit(callable).get(5, TimeUnit.SECONDS);
            });
            NPC npc = mock(NPC.class);
            AtomicReference<WorldPoint> position = new AtomicReference<>(new WorldPoint(3200, 3200, 0));
            when(npc.getWorldLocation()).thenAnswer(invocation -> {
                assertSame(clientThread, Thread.currentThread());
                return position.get();
            });
            when(npc.isDead()).thenAnswer(invocation -> {
                assertSame(clientThread, Thread.currentThread());
                return false;
            });
            // Override the getter to detect whether map() itself marshals ID extraction.
            NpcEntity view = new NpcEntity(ctx, npc) {
                @Override
                public int getId() {
                    assertSame(clientThread, Thread.currentThread());
                    return 7;
                }
            };
            List<NpcEntity> source = new ArrayList<>(List.of(view));
            Npcs query = new Npcs(ctx, source);
            query.filter(entity -> {
                assertSame(clientThread, Thread.currentThread());
                return true;
            }).distinct(entity -> {
                assertSame(clientThread, Thread.currentThread());
                return entity.getId();
            });

            source.add(view);
            query.sorted((first, second) -> {
                assertSame(clientThread, Thread.currentThread());
                return 0;
            });
            // A separate query guarantees the comparator is invoked before distinct removes duplicates.
            assertEquals(2, new Npcs(ctx, source).sorted((first, second) -> {
                assertSame(clientThread, Thread.currentThread());
                return 0;
            }).list().size());
            assertSame(view, query.firstMatching(entity -> !entity.raw().isDead()).orElseThrow());
            assertTrue(query.firstMatching(entity -> false).isEmpty());
            assertSame(view, query.first().orElseThrow(), "terminal predicates must not persist");
            assertSame(view, query.map().get(7));
            List<WorldPoint> snapshot = query.snapshot(NpcEntity::getWorldLocation);
            List<NpcEntity> membership = query.list();
            assertInstanceOf(EntityView.class, membership.get(0));
            position.set(new WorldPoint(3210, 3200, 0));
            source.clear();
            assertEquals(new WorldPoint(3200, 3200, 0), snapshot.get(0));
            assertThrows(UnsupportedOperationException.class, () -> snapshot.add(position.get()));
            assertEquals(1, membership.size());
            assertTrue(query.list().isEmpty());
            assertEquals(position.get(), executor.submit(membership.get(0)::getWorldLocation)
                    .get(5, TimeUnit.SECONDS));
            query.from(membership).stream().forEach(entity -> assertSame(callerThread, Thread.currentThread()));
            query.first().map(entity -> {
                assertSame(callerThread, Thread.currentThread());
                return entity;
            });
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void parallelSourcesCannotMovePredicatesOffClientThread() {
        Context ctx = QueryTestSupport.contextWithPlayerAt(null);
        Thread clientThread = Thread.currentThread();
        List<NpcEntity> entities = new ArrayList<>();
        for (int index = 0; index < 1000; index++) {
            entities.add(new NpcEntity(ctx, mock(NPC.class)));
        }
        assertEquals(1000, new Npcs(ctx, entities).filter(entity -> {
            assertSame(clientThread, Thread.currentThread());
            return true;
        }).count());
    }
    @Test
    @SuppressWarnings("unchecked")
    void clientThreadFailureReturnsEmptyTerminalValuesWithoutRunningCallbacks() {
        Context ctx = mock(Context.class);
        when(ctx.runOnClientThread(any(Callable.class))).thenThrow(new ClientThreadException("unavailable"));
        Npcs query = new Npcs(ctx, List.of(new NpcEntity(ctx, mock(NPC.class))));
        assertTrue(query.firstMatching(entity -> {
            fail("predicate must not run after a failed handoff");
            return true;
        }).isEmpty());
        assertTrue(query.snapshot(entity -> {
            fail("projection must not run after a failed handoff");
            return entity;
        }).isEmpty());
        assertTrue(query.map().isEmpty());
    }

}
