package unit.com.kraken.api.core;

import com.kraken.api.Context;
import com.kraken.api.core.ClientThreadException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContextCommandOutcomeTest {
    @Test
    void contextShutdownRevokesItsQueuedRunnable() throws Exception {
        net.runelite.api.Client client = mock(net.runelite.api.Client.class);
        when(client.isClientThread()).thenReturn(true);
        net.runelite.client.callback.ClientThread clientThread = mock(net.runelite.client.callback.ClientThread.class);
        Context context = new Context(client, clientThread, mock(com.kraken.api.input.mouse.VirtualMouse.class),
                mock(net.runelite.client.eventbus.EventBus.class), mock(net.runelite.client.game.ItemManager.class),
                mock(com.kraken.api.service.bank.BankService.class), mock(com.kraken.api.service.shop.ShopService.class),
                mock(com.kraken.api.core.interaction.InteractionManager.class),
                () -> mock(com.kraken.api.service.camera.CameraService.class));
        when(client.isClientThread()).thenReturn(false);
        java.util.concurrent.atomic.AtomicReference<Runnable> callback = new java.util.concurrent.atomic.AtomicReference<>();
        doAnswer(invocation -> { callback.set(invocation.getArgument(0)); return null; })
                .when(clientThread).invoke(any(Runnable.class));
        java.util.concurrent.atomic.AtomicInteger effects = new java.util.concurrent.atomic.AtomicInteger();
        context.runOnClientThread((Runnable) effects::incrementAndGet);
        context.shutdown();
        assertNotNull(callback.get());
        callback.get().run();
        assertEquals(0, effects.get());
        assertThrows(ClientThreadException.class, () -> context.runOnClientThread(() -> 1));
    }

    @Test
    void fallbackAndOptionalDoNotHideUnknownOutcomes() {
        Context context = mock(Context.class, CALLS_REAL_METHODS);
        ClientThreadException unknown = new ClientThreadException("already started", null, true);
        doThrow(unknown).when(context).runOnClientThread(any(Callable.class));
        assertSame(unknown, assertThrows(ClientThreadException.class, () -> context.runOnClientThread(() -> true, false)));
        assertSame(unknown, assertThrows(ClientThreadException.class, () -> context.runOnClientThreadOptional(() -> true)));
    }

    @Test
    void ordinaryRejectionStillUsesFallbackAndEmptyOptional() {
        Context context = mock(Context.class, CALLS_REAL_METHODS);
        doThrow(new ClientThreadException("cancelled before execution")).when(context).runOnClientThread(any(Callable.class));
        assertFalse(context.runOnClientThread(() -> true, false));
        assertTrue(context.runOnClientThreadOptional(() -> true).isEmpty());
    }
}
