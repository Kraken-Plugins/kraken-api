package com.kraken.api.service.ui.login;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kraken.api.Context;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.ReflectionHooks;
import com.kraken.api.query.player.LocalPlayerEntity;
import com.kraken.api.query.player.PlayerQuery;
import com.kraken.api.service.util.ReflectionService;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies how a requested idle timeout maps onto the client's idle timeout field and the watchdog, against a
 * client whose field, idle counters and clamping setter behave like the injected client's.
 */
class IdleTimeoutServiceTest {

    private static final String FIELD = "uv";
    private static final int RUNELITE_DEFAULT_TICKS = 15000;
    private static final int RUNELITE_MINIMUM_TICKS = 15000;
    private static final int RUNELITE_MAXIMUM_TICKS = 90000;
    private static final GameTick TICK = new GameTick();

    private final AtomicInteger idleTimeoutField = new AtomicInteger(RUNELITE_DEFAULT_TICKS);
    private final AtomicInteger mouseIdleTicks = new AtomicInteger();
    private final AtomicInteger keyboardIdleTicks = new AtomicInteger();

    private ReflectionHooks originalHooks;
    private Client client;
    private EventBus eventBus;
    private LocalPlayerEntity localPlayer;
    private IdleTimeoutService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        originalHooks = HooksLoader.getReflectionHooks();
        installIdleTimeoutField(FIELD);

        client = mock(Client.class);
        when(client.getIdleTimeout()).thenAnswer(invocation -> idleTimeoutField.get());
        when(client.getMouseIdleTicks()).thenAnswer(invocation -> mouseIdleTicks.get());
        when(client.getKeyboardIdleTicks()).thenAnswer(invocation -> keyboardIdleTicks.get());
        doAnswer(invocation -> {
            int requested = invocation.getArgument(0);
            idleTimeoutField.set(Math.max(RUNELITE_MINIMUM_TICKS, Math.min(RUNELITE_MAXIMUM_TICKS, requested)));
            return null;
        }).when(client).setIdleTimeout(anyInt());

        ReflectionService reflectionService = mock(ReflectionService.class);
        when(reflectionService.setFieldValue(anyString(), eq(FIELD), isNull(), any())).thenAnswer(invocation -> {
            idleTimeoutField.set(invocation.<Integer>getArgument(3));
            return true;
        });

        localPlayer = mock(LocalPlayerEntity.class);
        PlayerQuery players = mock(PlayerQuery.class);
        when(players.local()).thenReturn(localPlayer);

        Context context = mock(Context.class);
        when(context.players()).thenReturn(players);
        when(context.runOnClientThread(any(Callable.class), any()))
                .thenAnswer(invocation -> invocation.<Callable<?>>getArgument(0).call());

        eventBus = mock(EventBus.class);
        service = new IdleTimeoutService(client, eventBus, reflectionService, () -> context);
    }

    @AfterEach
    void restore() throws Exception {
        install(originalHooks);
    }

    @Test
    void rejectsTimeoutsShorterThanOneClientTick() {
        assertThrows(IllegalArgumentException.class, () -> service.setIdleTimeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> service.setIdleTimeout(Duration.ofMillis(Constants.CLIENT_TICK_LENGTH - 1)));
        assertThrows(IllegalArgumentException.class, () -> service.setIdleTimeout(Duration.ofMinutes(-1)));
        assertThrows(NullPointerException.class, () -> service.setIdleTimeout(null));

        assertEquals(RUNELITE_DEFAULT_TICKS, idleTimeoutField.get());
        verifyNoInteractions(eventBus);
    }

    @Test
    void rejectsTimeoutsBeyondTheServerSessionLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setIdleTimeout(IdleTimeoutService.MAXIMUM_IDLE_TIMEOUT.plusMillis(Constants.CLIENT_TICK_LENGTH)));

        assertTrue(service.setIdleTimeout(IdleTimeoutService.MAXIMUM_IDLE_TIMEOUT));
        assertEquals(Optional.of(IdleTimeoutService.MAXIMUM_IDLE_TIMEOUT), service.getIdleTimeout());
    }

    @Test
    void writesTimeoutsTheClientEnforcesWithoutAWatchdog() {
        assertTrue(service.setIdleTimeout(Duration.ofMinutes(45)));

        assertEquals(135000, idleTimeoutField.get());
        assertEquals(Optional.of(Duration.ofMinutes(45)), service.getIdleTimeout());

        idle(Duration.ofHours(1));
        service.onGameTick(TICK);
        verify(localPlayer, never()).logout();
    }

    @Test
    void switchesToTheWatchdogWhereRuneLiteScalingOverflows() {
        int ceiling = IdleTimeoutService.NATIVE_MAXIMUM_TICKS;
        assertDoesNotThrow(() -> Math.multiplyExact(IdleTimeoutService.CLIENT_IDLE_THRESHOLD_TICKS, ceiling));
        assertThrows(ArithmeticException.class, () -> Math.multiplyExact(IdleTimeoutService.CLIENT_IDLE_THRESHOLD_TICKS, ceiling + 1));

        service.setIdleTimeout(ticks(ceiling));
        assertEquals(ceiling, idleTimeoutField.get());

        service.setIdleTimeout(ticks(ceiling + 1));
        assertEquals(IdleTimeoutService.DISABLED_FIELD_VALUE, idleTimeoutField.get());
        assertEquals(Optional.of(ticks(ceiling + 1)), service.getIdleTimeout());
    }

    @Test
    void logsOutAfterALongTimeoutTheClientCannotEnforce() {
        service.setIdleTimeout(Duration.ofHours(2));

        idle(Duration.ofHours(2).minusSeconds(1));
        service.onGameTick(TICK);
        verify(localPlayer, never()).logout();

        idle(Duration.ofHours(2));
        service.onGameTick(TICK);
        verify(localPlayer).logout();
    }

    @Test
    void logsOutAfterAShortTimeoutOnlyOnceBothInputsAreIdle() {
        service.setIdleTimeout(Duration.ofMinutes(1));
        assertEquals(3000, idleTimeoutField.get());

        mouseIdleTicks.set(5000);
        keyboardIdleTicks.set(100);
        service.onGameTick(TICK);
        verify(localPlayer, never()).logout();

        keyboardIdleTicks.set(3000);
        service.onGameTick(TICK);
        verify(localPlayer).logout();
    }

    @Test
    void waitsBeforeRetryingARefusedLogout() {
        service.setIdleTimeout(Duration.ofMinutes(1));
        idle(Duration.ofMinutes(2));

        for (int tick = 0; tick <= IdleTimeoutService.LOGOUT_RETRY_GAME_TICKS; tick++) {
            service.onGameTick(TICK);
        }
        verify(localPlayer, times(1)).logout();

        service.onGameTick(TICK);
        verify(localPlayer, times(2)).logout();
    }

    @Test
    void disableIdleTimeoutNeverLogsOut() {
        assertTrue(service.disableIdleTimeout());

        assertEquals(IdleTimeoutService.DISABLED_FIELD_VALUE, idleTimeoutField.get());
        assertEquals(Optional.empty(), service.getIdleTimeout());

        idle(IdleTimeoutService.MAXIMUM_IDLE_TIMEOUT);
        service.onGameTick(TICK);
        verify(localPlayer, never()).logout();
    }

    @Test
    void reappliesTheTimeoutAfterRuneLiteOverwritesIt() {
        service.setIdleTimeout(Duration.ofHours(2));

        // RuneLite's Logout Timer plugin goes through the clamping setter.
        client.setIdleTimeout(Integer.MAX_VALUE);
        assertEquals(RUNELITE_MAXIMUM_TICKS, idleTimeoutField.get());

        service.onGameTick(TICK);
        assertEquals(IdleTimeoutService.DISABLED_FIELD_VALUE, idleTimeoutField.get());
    }

    @Test
    void registersForGameTicksOnce() {
        service.setIdleTimeout(Duration.ofMinutes(10));
        service.disableIdleTimeout();

        verify(eventBus, times(1)).register(service);
    }

    @Test
    void resetRestoresTheTimeoutFromBeforeTheFirstChange() {
        idleTimeoutField.set(45000);
        service.setIdleTimeout(Duration.ofHours(2));
        service.setIdleTimeout(Duration.ofMinutes(1));

        assertTrue(service.reset());
        assertEquals(45000, idleTimeoutField.get());
        verify(eventBus).unregister(service);

        client.setIdleTimeout(30000);
        service.onGameTick(TICK);
        assertEquals(30000, idleTimeoutField.get());
    }

    @Test
    void refusesWhenHooksPredateTheIdleTimeoutField() throws Exception {
        installIdleTimeoutField(null);

        assertFalse(service.setIdleTimeout(Duration.ofHours(1)));
        assertFalse(service.disableIdleTimeout());

        assertEquals(RUNELITE_DEFAULT_TICKS, idleTimeoutField.get());
        verifyNoInteractions(eventBus);
    }

    private void idle(Duration duration) {
        int idleTicks = (int) (duration.toMillis() / Constants.CLIENT_TICK_LENGTH);
        mouseIdleTicks.set(idleTicks);
        keyboardIdleTicks.set(idleTicks);
    }

    private static Duration ticks(int clientTicks) {
        return Duration.ofMillis((long) clientTicks * Constants.CLIENT_TICK_LENGTH);
    }

    private void installIdleTimeoutField(String fieldName) throws Exception {
        Gson gson = new Gson();
        JsonObject hooks = gson.toJsonTree(originalHooks).getAsJsonObject();
        if (fieldName == null) {
            hooks.remove("idleTimeoutField");
        } else {
            hooks.addProperty("idleTimeoutField", fieldName);
        }
        install(gson.fromJson(hooks, ReflectionHooks.class));
    }

    private static void install(ReflectionHooks hooks) throws Exception {
        Field field = HooksLoader.class.getDeclaredField("reflectionHooks");
        field.setAccessible(true);
        field.set(null, hooks);
    }
}
