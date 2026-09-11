package com.kraken.api.core;

import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientThreadGatewayTest {
    private final Client client = mock(Client.class);
    private final ClientThread clientThread = mock(ClientThread.class);
    private final BlockingQueue<Runnable> callbacks = new LinkedBlockingQueue<>();

    private ClientThreadGateway gateway(long timeoutMillis) {
        doAnswer(invocation -> { callbacks.add(invocation.getArgument(0)); return null; })
                .when(clientThread).invoke(any(Runnable.class));
        return new ClientThreadGateway(client, clientThread, timeoutMillis);
    }

    @Test
    void timeoutRevokesQueuedActionAndRetryRunsExactlyOnce() throws Exception {
        ClientThreadGateway gateway = gateway(20);
        AtomicInteger effects = new AtomicInteger();
        ClientThreadException failure = assertThrows(ClientThreadException.class, () -> gateway.call(effects::incrementAndGet));
        assertFalse(failure.isOutcomeUnknown());
        callbacks.take().run();
        assertEquals(0, effects.get());
        when(client.isClientThread()).thenReturn(true);
        assertEquals(1, gateway.call(effects::incrementAndGet));
    }

    @Test
    void expiredQueueEntryCannotStartEvenBeforeCallerCancelsIt() throws Exception {
        gateway(1000);
        AtomicLong clock = new AtomicLong();
        ClientThreadGateway gateway = new ClientThreadGateway(client, clientThread, 10, clock::get);
        AtomicInteger effects = new AtomicInteger();
        gateway.execute(effects::incrementAndGet);
        clock.set(10);
        callbacks.take().run();
        assertEquals(0, effects.get());
    }

    @Test
    void interruptionRevokesPendingActionAndRestoresInterruptFlag() throws Exception {
        ClientThreadGateway gateway = gateway(3000);
        AtomicInteger effects = new AtomicInteger();
        AtomicReference<ClientThreadException> failure = new AtomicReference<>();
        AtomicReference<Boolean> interrupted = new AtomicReference<>();
        Thread caller = new Thread(() -> {
            try { gateway.call(effects::incrementAndGet); }
            catch (ClientThreadException e) {
                failure.set(e);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        caller.start();
        Runnable callback = callbacks.poll(1, TimeUnit.SECONDS);
        assertNotNull(callback);
        caller.interrupt();
        caller.join(1000);
        assertFalse(caller.isAlive());
        assertNotNull(failure.get());
        assertFalse(failure.get().isOutcomeUnknown());
        assertEquals(Boolean.TRUE, interrupted.get());
        callback.run();
        assertEquals(0, effects.get());
    }

    @Test
    void timeoutDuringExecutionReportsUnknownAndDoesNotInterruptGameThread() throws Exception {
        ClientThreadGateway gateway = gateway(500);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<ClientThreadException> failure = new AtomicReference<>();
        AtomicInteger effects = new AtomicInteger();
        Thread caller = new Thread(() -> {
            try {
                gateway.call(() -> { started.countDown(); release.await(); return effects.incrementAndGet(); });
            } catch (ClientThreadException e) { failure.set(e); }
        });
        caller.start();
        Runnable callback = callbacks.poll(1, TimeUnit.SECONDS);
        assertNotNull(callback);
        Thread gameThread = new Thread(callback);
        gameThread.start();
        try {
            assertTrue(started.await(1, TimeUnit.SECONDS));
            caller.join(1000);
            assertNotNull(failure.get());
            assertTrue(failure.get().isOutcomeUnknown());
            assertEquals(0, effects.get());
            assertFalse(gameThread.isInterrupted());
        } finally {
            release.countDown();
            gameThread.join(1000);
            caller.join(1000);
        }
        assertEquals(1, effects.get());
        callback.run();
        assertEquals(1, effects.get());
    }

    @Test
    void shutdownCancelsPendingCallsAndAsyncActionsAndRejectsNewWork() throws Exception {
        ClientThreadGateway gateway = gateway(3000);
        AtomicInteger effects = new AtomicInteger();
        CompletableFuture<ClientThreadException> failure = CompletableFuture.supplyAsync(() ->
                assertThrows(ClientThreadException.class, () -> gateway.call(effects::incrementAndGet)));
        Runnable call = callbacks.poll(1, TimeUnit.SECONDS);
        assertNotNull(call);
        gateway.execute(effects::incrementAndGet);
        gateway.close();
        assertFalse(failure.get(1, TimeUnit.SECONDS).isOutcomeUnknown());
        call.run();
        callbacks.take().run();
        when(client.isClientThread()).thenReturn(true);
        assertThrows(ClientThreadException.class, () -> gateway.call(effects::incrementAndGet));
        assertThrows(ClientThreadException.class, () -> gateway.execute(effects::incrementAndGet));
        assertEquals(0, effects.get());
        gateway.close();
    }

    @Test
    void rejectedSubmissionCannotLeaveAnExecutableCallback() throws Exception {
        ClientThreadGateway gateway = gateway(1000);
        doAnswer(invocation -> {
            callbacks.add(invocation.getArgument(0));
            throw new IllegalStateException("queue rejected after accepting callback");
        }).when(clientThread).invoke(any(Runnable.class));
        AtomicInteger effects = new AtomicInteger();
        assertFalse(assertThrows(ClientThreadException.class,
                () -> gateway.call(effects::incrementAndGet)).isOutcomeUnknown());
        callbacks.take().run();
        assertEquals(0, effects.get());
    }

    @Test
    void inlineResultsNullAndFailuresRetainTheirMeaning() {
        ClientThreadGateway gateway = gateway(1000);
        when(client.isClientThread()).thenReturn(true);
        assertNull(gateway.call(() -> null));
        assertEquals(42, gateway.call(() -> 42));
        IllegalStateException cause = new IllegalStateException("test");
        assertSame(cause, assertThrows(ClientThreadException.class, () -> gateway.call(() -> { throw cause; })).getCause());
    }
}
