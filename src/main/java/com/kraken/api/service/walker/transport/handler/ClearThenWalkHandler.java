package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.ObjectInfo;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;

/**
 * Crosses an obstacle that has to be cleared before it can be walked through, such as a web that is
 * slashed.
 *
 * <p>Operating such an obstacle neither moves the player nor reliably works: a slash can fail,
 * leaving the web intact and waiting for another click, and even a successful slash only opens the
 * way while the player stays put. So the obstacle is operated until the far side becomes reachable,
 * retrying failed attempts, and the player is then walked through before the crossing is reported.</p>
 *
 * <p>An obstacle that is already clear is walked through without clicking. A slashed web offers no
 * {@code Slash} action, and reporting it crossed without moving would leave the next round planning
 * the same edge from the same tile.</p>
 */
@Slf4j
public class ClearThenWalkHandler implements TransportHandler {

    /** How many times to operate the obstacle before giving up; an attempt can fail and leave it standing. */
    public static final int MAX_ATTEMPTS = 5;

    /** How long to wait for the player to stop moving before each attempt. */
    private static final long IDLE_BEFORE_CLICK_MS = 8_000;

    /** How long one attempt gets to open the way: walking to the obstacle plus its animation. */
    private static final long CLEAR_TIMEOUT_MS = 6_000;

    @Override
    public boolean execute(TransportContext context) {
        WorldPoint destination = context.getDestination();
        if (destination == null) {
            return context.fail("the obstacle at " + context.getOrigin() + " carries no destination to walk through to");
        }

        Context ctx = context.getCtx();
        TileService tileService = ctx.getService(TileService.class);
        String obstacle = describe(context.getObjectInfo());

        for (int attempt = 1; !tileService.isTileReachable(destination); attempt++) {
            if (attempt > MAX_ATTEMPTS) {
                return context.fail("operated " + obstacle + " " + MAX_ATTEMPTS + " times but the way never opened");
            }

            SleepService.sleepUntil(() -> ctx.players().local().isIdle(), IDLE_BEFORE_CLICK_MS);
            log.info("Walker: clearing {} toward {} (attempt {}/{})", obstacle, destination, attempt, MAX_ATTEMPTS);
            if (!TransportEntityResolver.interact(context)) {
                return context.fail("could not operate " + obstacle);
            }

            SleepService.sleepUntil(() -> tileService.isTileReachable(destination), CLEAR_TIMEOUT_MS);
        }

        log.info("Walker: {} is clear; walking through to {}", obstacle, destination);
        MovementService movementService = ctx.getService(MovementService.class);
        if (!movementService.traversePath(ctx.getClient(), Collections.singletonList(destination), 0)) {
            return context.fail("cleared " + obstacle + " but could not walk through to " + destination);
        }

        return true;
    }

    /**
     * Names the obstacle for logs and failure reasons.
     *
     * @param info what the transport clicks, may be null
     * @return the menu option and target, or a placeholder when the transport names nothing
     */
    static String describe(ObjectInfo info) {
        if (info == null) {
            return "the obstacle";
        }
        String text = info.hasId() ? info.getRemainder() + " " + info.getId() : info.getRemainder();
        return text.trim().isEmpty() ? "the obstacle" : text.trim();
    }
}
