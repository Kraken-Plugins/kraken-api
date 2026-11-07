package com.kraken.api.core.interaction.event;

import com.kraken.api.core.interaction.model.MenuOption;
import lombok.Value;

/**
 * Posted on RuneLite's event bus, on the client thread, right after Kraken sends a click packet. Kraken's clicks post
 * no {@code MenuOptionClicked} and no AWT mouse events, so this is how a recorder or test tool sees them.
 *
 * <p>Clicks sent through an interaction carry the menu entry they act on. A bare click packet, such as one sent with
 * {@code MousePackets.queueClickPacket} before a movement packet, has no menu entry.
 */
@Value
public class KrakenClickSent {

    /**
     * When the click packet was stamped, from {@link System#currentTimeMillis()}: the time the packet's delta counts
     * from.
     */
    long millis;

    /**
     * The canvas x coordinate in the click packet.
     */
    int x;

    /**
     * The canvas y coordinate in the click packet.
     */
    int y;

    /**
     * The menu entry the click acts on, or null for a bare click packet.
     */
    MenuOption option;

    /**
     * The menu option text, such as "Activate", or an empty string.
     */
    String action;

    /**
     * The menu target text with tags removed, such as "Protect from Magic", or an empty string.
     */
    String target;
}
