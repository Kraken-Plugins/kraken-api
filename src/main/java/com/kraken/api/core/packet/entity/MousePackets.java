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
        Field clientLastPressedTimeMillis = client.getClass().getDeclaredField(HooksLoader.getReflectionHooks().getClientMillisField());
        clientLastPressedTimeMillis.setAccessible(true);
        long retValue = clientLastPressedTimeMillis.getLong(client) * HooksLoader.getReflectionHooks().getClientMillisMultiplier();
        clientLastPressedTimeMillis.setAccessible(false);
        return retValue;
    }

    @SneakyThrows
    private void setMouseHandlerLastMillis(long time) {
        Class<?> mouseHandler = client.getClass().getClassLoader().loadClass(HooksLoader.getReflectionHooks().getMouseHandlerLastPressedClass());
        Field mouseHandlerLastPressedTime = mouseHandler.getDeclaredField(HooksLoader.getReflectionHooks().getMouseHandlerLastPressedField());
        mouseHandlerLastPressedTime.setAccessible(true);
        mouseHandlerLastPressedTime.setLong(null, time * MathUtils.modInverse(HooksLoader.getReflectionHooks().getMouseHandlerMultiplier()));
        mouseHandlerLastPressedTime.setAccessible(false);
    }

    @SneakyThrows
    private void setClientLastMillis(long time) {
        Field clientLastPressedTimeMillis = client.getClass().getDeclaredField(HooksLoader.getReflectionHooks().getClientMillisField());
        clientLastPressedTimeMillis.setAccessible(true);
        clientLastPressedTimeMillis.setLong(client, time * MathUtils.modInverse(HooksLoader.getReflectionHooks().getClientMillisMultiplier()));
        clientLastPressedTimeMillis.setAccessible(false);
    }
}
