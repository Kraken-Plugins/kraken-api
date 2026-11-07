package com.kraken.api.input.mouse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.input.InputDispatch;
import lombok.Getter;
import net.runelite.api.Point;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;

import java.awt.event.MouseEvent;

/**
 * Records the last mouse press the game saw, the player's own or one Kraken's {@link VirtualMouse} dispatched. Every
 * press becomes a click packet with the time since the previous click, so the click scheduler counts these presses
 * when it keeps a human gap before Kraken's next click.
 *
 * <p>Registers itself as a mouse listener when constructed, after any listener already registered, and never
 * consumes an event. The listener runs on the AWT event thread and only stores values.
 */
@Singleton
public class MousePresses implements MouseListener {

    /**
     * When the last press happened, from {@link System#currentTimeMillis()} (the clock the game stamps presses
     * with), or 0 before the first press.
     */
    @Getter
    private volatile long lastPressMillis;

    /**
     * Where on the canvas the last press happened, or null before the first press.
     */
    @Getter
    private volatile Point lastPressPoint;

    /**
     * True when the last press came from Kraken's {@link VirtualMouse} rather than the player.
     */
    @Getter
    private volatile boolean lastPressSynthetic;

    /**
     * Creates the tracker and registers it with RuneLite's mouse manager.
     *
     * @param mouseManager RuneLite's mouse manager that delivers mouse events to listeners.
     */
    @Inject
    public MousePresses(MouseManager mouseManager) {
        mouseManager.registerMouseListener(this);
    }

    /**
     * Creates a tracker that is not registered with a mouse manager, for tests that call {@link #record} directly.
     */
    public MousePresses() {
    }

    /**
     * Stores a press.
     *
     * @param millis    when the press happened, from {@link System#currentTimeMillis()}.
     * @param point     where on the canvas it happened.
     * @param synthetic true when Kraken dispatched it.
     */
    public void record(long millis, Point point, boolean synthetic) {
        lastPressPoint = point;
        lastPressSynthetic = synthetic;
        lastPressMillis = millis;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent e) {
        record(System.currentTimeMillis(), new Point(e.getX(), e.getY()), InputDispatch.isSynthetic());
        return e;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent e) {
        return e;
    }
}
