package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;

/**
 * Crosses a transport that has nothing to operate: two tiles that the game joins by walking, in a
 * place where the bundled collision map says they are not joined.
 *
 * <p>The planner needs such a transport to route across the gap at all. The live scene is what
 * says whether the step can really be taken, so the player is walked onto the far tile when it is
 * reachable there, and the crossing fails with a reason when it is not.</p>
 */
@Slf4j
public class WalkTransportHandler implements TransportHandler {

    @Override
    public boolean execute(TransportContext context) {
        WorldPoint destination = context.getDestination();
        if (destination == null) {
            return context.fail("the step from " + context.getOrigin() + " carries no tile to walk to");
        }

        Context ctx = context.getCtx();
        if (!ctx.getService(TileService.class).isTileReachable(destination)) {
            return context.fail(destination + " cannot be walked to from " + context.playerLocation());
        }

        log.info("Walker: walking across to {}", destination);
        MovementService movementService = ctx.getService(MovementService.class);
        if (!movementService.traversePath(ctx.getClient(), Collections.singletonList(destination), 0)) {
            return context.fail("could not walk across to " + destination);
        }

        return true;
    }
}
