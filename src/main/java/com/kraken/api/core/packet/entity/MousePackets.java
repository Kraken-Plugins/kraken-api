package com.kraken.api.core.packet.entity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.PacketFactory;
import com.kraken.api.util.MathUtils;
import com.kraken.api.util.RandomUtils;
import lombok.SneakyThrows;
import net.runelite.api.Client;

import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;

@Singleton
public class MousePackets {

    @Inject
    private Client client;

    @Inject
    private Provider<PacketClient> packetSenderProvider;

    /**
     * Idle-tick threshold at which a synthetic keypress is injected to reset the client's idle
     * counters, re-randomised each time it is crossed.
     *
     * <p>Deliberately static: the counters being watched ({@code getKeyboardIdleTicks} and
     * {@code getMouseIdleTicks}) are client-wide, so every plugin observes the same idle state and one
     * shared threshold produces one keypress per crossing. The compare-and-set below is what enforces
     * that when several plugins click in the same instant.</p>
     */
    private static final AtomicLong idleResetThreshold = new AtomicLong(RandomUtils.randomDelay());

    private final Object resolutionLock = new Object();
    private volatile Field clientMillisField;
    private volatile Field mouseHandlerLastPressedField;
    private volatile Long clientMillisMultiplierInverse;
    private volatile Long mouseHandlerMultiplierInverse;

    /**
     * Queues a click packet to send to the game server. The click packet should be sent before
     * any game interaction (Widget, Movement, Npc, Object etc...) packets are sent. The click packet
     * encapsulates the x and y coordinates of the canvas for the click that was made.
     * @param x The x canvas coordinate.
     * @param y The y canvas coordinate.
     */
    @SneakyThrows
    public void queueClickPacket(int x, int y) {
        long mouseHandlerMS = System.currentTimeMillis();
        setMouseHandlerLastMillis(mouseHandlerMS);
        long clientMS = getClientLastMillis();
        long deltaMs = mouseHandlerMS - clientMS;
        setClientLastMillis(mouseHandlerMS);

        if (deltaMs < 0) deltaMs = 0L;
        if (deltaMs > 32767) deltaMs = 32767L;

        // Last bit of the mouse time is expected to be 1 if it's a left click and 0 if it's a right. This shifts to make it always 1.
        // Since this is only invoked on actual clicks to do something in the game its always going to be a left click.
        int mouseInfo = ((int) deltaMs << 1);

        packetSenderProvider.get().sendPacket(PacketFactory.getEventMouseClick(), mouseInfo, x, y, 0);

        maybeResetIdleCounters();
    }

    /**
     * Injects a synthetic keypress once the client's idle counters pass the current threshold, then
     * re-randomises that threshold.
     *
     * <p>The compare-and-set makes the check-and-claim atomic, so concurrent callers produce exactly
     * one keypress per crossing rather than one each.</p>
     */
    private void maybeResetIdleCounters() {
        int idleClientTicks = client.getKeyboardIdleTicks();
        if (client.getMouseIdleTicks() < idleClientTicks) {
            idleClientTicks = client.getMouseIdleTicks();
        }

        long threshold = idleResetThreshold.get();
        if (idleClientTicks < threshold) {
            return;
        }

        if (!idleResetThreshold.compareAndSet(threshold, RandomUtils.randomDelay())) {
            return;
        }

        dispatchIdleResetKeyPress();
    }

    /**
     * Dispatches the backspace key sequence that resets the client's keyboard idle counter.
     *
     * <p>Posted to the AWT event queue because {@code Component.dispatchEvent} runs the canvas
     * listeners synchronously on the calling thread, and the event dispatch thread is where the client
     * expects to receive input events.</p>
     */
    private void dispatchIdleResetKeyPress() {
        EventQueue.invokeLater(() -> {
            KeyEvent keyPress = new KeyEvent(client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), BUTTON1_DOWN_MASK, KeyEvent.VK_BACK_SPACE, (char) KeyEvent.VK_BACK_SPACE);
            client.getCanvas().dispatchEvent(keyPress);
            KeyEvent keyRelease = new KeyEvent(client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE, (char) KeyEvent.VK_BACK_SPACE);
            client.getCanvas().dispatchEvent(keyRelease);
            KeyEvent keyTyped = new KeyEvent(client.getCanvas(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, (char) KeyEvent.VK_UNDEFINED);
            client.getCanvas().dispatchEvent(keyTyped);
        });
    }

    @SneakyThrows
    private long getClientLastMillis() {
        return getClientMillisField().getLong(client) * HooksLoader.getReflectionHooks().getClientMillisMultiplier();
    }

    @SneakyThrows
    private void setMouseHandlerLastMillis(long time) {
        getMouseHandlerLastPressedField().setLong(null, time * mouseHandlerMultiplierInverse());
    }

    @SneakyThrows
    private void setClientLastMillis(long time) {
        getClientMillisField().setLong(client, time * clientMillisMultiplierInverse());
    }

    /**
     * Returns the cached client millis {@link Field}, resolving it on first use. Only the field
     * handle is cached — the timestamp is read from and written to the live client on every call.
     * A failed resolution is not cached and is retried on the next call.
     *
     * @return The reflected field with its accessible flag set.
     */
    @SneakyThrows
    private Field getClientMillisField() {
        Field cached = clientMillisField;
        if (cached != null) {
            return cached;
        }
        synchronized (resolutionLock) {
            if (clientMillisField == null) {
                Field resolved = client.getClass().getDeclaredField(HooksLoader.getReflectionHooks().getClientMillisField());
                resolved.setAccessible(true);
                clientMillisField = resolved;
            }
            return clientMillisField;
        }
    }

    /**
     * Returns the cached mouse handler last-pressed {@link Field}, resolving it on first use.
     * Only the field handle is cached — the timestamp is written through it on every call.
     * A failed resolution is not cached and is retried on the next call.
     *
     * @return The reflected static field with its accessible flag set.
     */
    @SneakyThrows
    private Field getMouseHandlerLastPressedField() {
        Field cached = mouseHandlerLastPressedField;
        if (cached != null) {
            return cached;
        }
        synchronized (resolutionLock) {
            if (mouseHandlerLastPressedField == null) {
                Class<?> mouseHandler = client.getClass().getClassLoader().loadClass(HooksLoader.getReflectionHooks().getMouseHandlerLastPressedClass());
                Field resolved = mouseHandler.getDeclaredField(HooksLoader.getReflectionHooks().getMouseHandlerLastPressedField());
                resolved.setAccessible(true);
                mouseHandlerLastPressedField = resolved;
            }
            return mouseHandlerLastPressedField;
        }
    }

    /**
     * Returns the modular inverse of the hooked client millis multiplier, computed once. The
     * inverse is a pure function of the hook constant, so caching it cannot change the value.
     *
     * @return The modular inverse used to scramble timestamps written to the client field.
     */
    private long clientMillisMultiplierInverse() {
        Long cached = clientMillisMultiplierInverse;
        if (cached == null) {
            cached = MathUtils.modInverse(HooksLoader.getReflectionHooks().getClientMillisMultiplier());
            clientMillisMultiplierInverse = cached;
        }
        return cached;
    }

    /**
     * Returns the modular inverse of the hooked mouse handler multiplier, computed once. The
     * inverse is a pure function of the hook constant, so caching it cannot change the value.
     *
     * @return The modular inverse used to scramble timestamps written to the mouse handler field.
     */
    private long mouseHandlerMultiplierInverse() {
        Long cached = mouseHandlerMultiplierInverse;
        if (cached == null) {
            cached = MathUtils.modInverse(HooksLoader.getReflectionHooks().getMouseHandlerMultiplier());
            mouseHandlerMultiplierInverse = cached;
        }
        return cached;
    }
}
