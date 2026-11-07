package com.kraken.api.core.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.input.mouse.MousePresses;
import net.runelite.api.Point;

/**
 * The last click the game sent, from any source: a click packet Kraken sent ({@link MousePackets}) or a mouse press
 * the game read from the canvas, the player's own or {@code VirtualMouse}'s ({@link MousePresses}). Each of them
 * reaches the server as a click packet, so the time between two clicks is counted from whichever came last.
 */
@Singleton
public class ClickHistory {

    private final MousePackets mousePackets;
    private final MousePresses mousePresses;

    /**
     * @param mousePackets the click packet sender, for Kraken's own clicks.
     * @param mousePresses the press tracker, for clicks made on the canvas.
     */
    @Inject
    public ClickHistory(MousePackets mousePackets, MousePresses mousePresses) {
        this.mousePackets = mousePackets;
        this.mousePresses = mousePresses;
    }

    /**
     * @return when the last click from any source happened, from {@link System#currentTimeMillis()}, or 0 before
     * the first click.
     */
    public long getLastClickMillis() {
        return Math.max(mousePackets.getLastClickMillis(), mousePresses.getLastPressMillis());
    }

    /**
     * @return where on the canvas the last click from any source landed, or null before the first click.
     */
    public Point getLastClickPoint() {
        return isLastClickPress() ? mousePresses.getLastPressPoint() : mousePackets.getLastClickPoint();
    }

    /**
     * @return true when the last click was a press on the canvas rather than a click packet Kraken sent.
     */
    public boolean isLastClickPress() {
        return mousePresses.getLastPressMillis() > mousePackets.getLastClickMillis();
    }
}
