package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Crosses a transport that is operated by a single click: doors, gates, stairs, ladders, agility
 * shortcuts, levers and portals.
 *
 * <p>This is the base shape. Transports that need a follow-up step build on it rather than repeating
 * the click.</p>
 */
@Slf4j
public class ObjectTransportHandler implements TransportHandler {

    /** How long to wait for a click to move the player before giving up on it. */
    protected static final long MOVE_TIMEOUT_MS = 8_000;

    @Override
    public boolean execute(TransportContext context) {
        WorldPoint before = context.playerLocation();

        if (!TransportEntityResolver.interact(context)) {
            return false;
        }

        awaitCrossing(context, before);
        return true;
    }

    /**
     * Waits until the player has moved somewhere other than where they started.
     *
     * <p>Deliberately not a check for arriving at the destination: a staircase lands the player on
     * another plane, an agility shortcut can drop them short, and the walker re-plans from wherever
     * they end up. All this needs to know is that the click did something.</p>
     *
     * @param context the transport being crossed
     * @param before where the player stood before the click
     * @return true when the player moved
     */
    protected boolean awaitCrossing(TransportContext context, WorldPoint before) {
        if (before == null) {
            SleepService.sleepFor(2);
            return true;
        }

        return SleepService.sleepUntil(() -> hasMoved(context, before), MOVE_TIMEOUT_MS);
    }

    private boolean hasMoved(TransportContext context, WorldPoint before) {
        WorldPoint now = context.playerLocation();
        return now != null && !now.equals(before);
    }
}
