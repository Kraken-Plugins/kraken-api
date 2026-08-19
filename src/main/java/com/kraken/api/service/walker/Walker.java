package com.kraken.api.service.walker;

import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.TransportExecutor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Walks the player anywhere in the game world, operating whatever transports the route needs.
 *
 * <p>Where {@link MovementService} sends a single click and {@link GlobalPathfinder} produces a route,
 * this composes the two: it plans, walks the part of the route the client has loaded, operates the
 * transport at the end of that part, then re-plans from wherever the player ended up. That loop is
 * what turns a short-range click into cross-map travel.</p>
 *
 * <h3>Threading</h3>
 * <p>Every call blocks and must be made off the client thread. Waiting is a no-op on the client
 * thread, which would turn the loop into a spin that fails in milliseconds for no visible reason, so
 * the walker refuses to run there rather than misbehaving.</p>
 *
 * <h3>Failure</h3>
 * <p>A walk that cannot finish returns a {@link WalkResult} saying why — a missing item for a boat
 * fare, a transport whose destination chooser is not implemented, a stall — rather than hanging.</p>
 *
 * <h3>Doors the planner does not know about</h3>
 * <p>The transport dataset lists only the doors that gate a route. An ordinary closed door is just a
 * wall to the planner, so it routes around one where it can and never mentions it. When a walk stops
 * making progress {@link ObstacleRecovery} looks for a door, gate or curtain in the way and opens it,
 * which covers the doors the graph has never heard of.</p>
 */
@Slf4j
@Singleton
public class Walker {

    @Inject
    private Context ctx;

    @Inject
    private GlobalPathfinder globalPathfinder;

    @Inject
    private MovementService movementService;

    @Inject
    private TransportExecutor transportExecutor;

    @Inject
    private ObstacleRecovery obstacleRecovery;

    /**
     * Walks to a destination using default settings.
     *
     * @param destination the tile to reach
     * @return how the walk ended
     */
    public WalkResult walkTo(WorldPoint destination) {
        return walkTo(destination, WalkerConfig.builder().build());
    }

    /**
     * Walks to a destination, stopping within a given distance of it.
     *
     * @param destination the tile to reach
     * @param tolerance how close counts as arrived, in tiles
     * @return how the walk ended
     */
    public WalkResult walkTo(WorldPoint destination, int tolerance) {
        return walkTo(destination, WalkerConfig.builder().tolerance(tolerance).build());
    }

    /**
     * Walks to a destination with explicit settings.
     *
     * @param destination the tile to reach
     * @param config tolerances, budgets and the planning options to use
     * @return how the walk ended
     */
    public WalkResult walkTo(WorldPoint destination, WalkerConfig config) {
        if (destination == null) {
            return WalkResult.failure(WalkOutcome.NO_ROUTE, "no destination given", null, 0);
        }

        if (ctx.getClient().isClientThread()) {
            log.error("Walker blocks and must not be called on the client thread");
            return WalkResult.failure(WalkOutcome.CALLED_ON_CLIENT_THREAD,
                    "the walker blocks and must be called off the client thread", null, 0);
        }

        if (isInInstance()) {
            return WalkResult.failure(WalkOutcome.IN_INSTANCE,
                    "instanced coordinates do not correspond to the static collision map", playerLocation(), 0);
        }

        return run(destination, config);
    }

    private WalkResult run(WorldPoint destination, WalkerConfig config) {
        long deadline = System.currentTimeMillis() + config.getTimeoutMillis();
        int stalledRounds = 0;
        int previousDistance = Integer.MAX_VALUE;

        for (int round = 0; round < config.getMaxRounds(); round++) {
            if (System.currentTimeMillis() > deadline) {
                return WalkResult.failure(WalkOutcome.TIMED_OUT,
                        "exceeded the " + config.getTimeoutMillis() + "ms budget", playerLocation(), round);
            }

            WorldPoint here = playerLocation();
            if (here == null) {
                return WalkResult.failure(WalkOutcome.UNKNOWN_POSITION,
                        "could not read the player's location", null, round);
            }

            if (here.distanceTo(destination) <= config.getTolerance()) {
                return WalkResult.success(here, round);
            }

            GlobalPathfinder.PathResult route =
                    globalPathfinder.findPathResult(here, destination, config.getPathfinderConfig());
            if (route == null || route.getPath().isEmpty()) {
                return WalkResult.failure(WalkOutcome.NO_ROUTE,
                        "no route from " + here + " to " + destination, here, round);
            }

            GlobalPathfinder.TransportUsage next = WalkPlan.firstTransport(route.getTransports());
            WalkResult failure = advance(route, next, config, round);
            if (failure != null) {
                return failure;
            }

            WorldPoint after = playerLocation();
            if (after == null) {
                return WalkResult.failure(WalkOutcome.UNKNOWN_POSITION,
                        "could not read the player's location", null, round);
            }

            int distance = after.distanceTo(destination);
            if (WalkPlan.madeProgress(previousDistance, distance, config.getMinProgressTiles())) {
                stalledRounds = 0;
            } else if (obstacleRecovery.clear(route.getPath())) {
                // A closed door is not a stall. The dataset lists only the doors that gate a route, so
                // an ordinary one is invisible to the planner and shows up as a walk that stops
                // getting anywhere. Opening it is progress, so the stall budget is left alone.
                stalledRounds = 0;
            } else if (++stalledRounds >= config.getMaxStalledRounds()) {
                return WalkResult.failure(WalkOutcome.STALLED,
                        "stopped making progress " + distance + " tiles from " + destination, after, round);
            } else {
                SleepService.sleep(600);
            }

            previousDistance = distance;
        }

        return WalkResult.failure(WalkOutcome.TIMED_OUT,
                "gave up after " + config.getMaxRounds() + " rounds", playerLocation(), config.getMaxRounds());
    }

    /**
     * Walks one leg of the route and, when that leg ends at a transport, operates it.
     *
     * @return null to continue, or the result that should end the walk
     */
    private WalkResult advance(GlobalPathfinder.PathResult route,
                               GlobalPathfinder.TransportUsage next,
                               WalkerConfig config,
                               int round) {
        if (next == null) {
            // The dense path, not the sparse one: walk() needs every tile to find what is in the way,
            // and applyVariableStride is what turns it into clicks.
            walk(route.getPath());
            return null;
        }

        walk(WalkPlan.approach(route.getPath(), next.getPathIndex()));


        WorldPoint here = playerLocation();
        if (here == null) {
            return WalkResult.failure(WalkOutcome.UNKNOWN_POSITION,
                    "could not read the player's location", null, round);
        }

        boolean teleport = next.getType() != null && next.getType().isTeleport();
        if (!teleport && here.distanceTo(next.getOrigin()) > config.getTransportFireRadius()) {
            // Not there yet. Re-planning from here is safer than firing at a distance, because the
            // scene may simply not have loaded the rest of the approach yet.
            return null;
        }

        TransportExecutor.Result result = transportExecutor.execute(next);
        if (result.isCrossed()) {
            return null;
        }

        if (result.isRequirementsUnmet()) {
            return WalkResult.failure(WalkOutcome.TRANSPORT_REQUIREMENTS_UNMET, result.getReason(), here, round);
        }

        if (!result.isSupported()) {
            return WalkResult.failure(WalkOutcome.TRANSPORT_UNSUPPORTED, result.getReason(), here, round);
        }

        return WalkResult.failure(WalkOutcome.TRANSPORT_FAILED, result.getReason(), here, round);
    }

    /**
     * Walks whatever part of a route is inside the loaded scene and currently reachable.
     *
     * <p>The leg is cut short at the first waypoint the player cannot reach, so the movement
     * primitive is only handed a stretch it can actually complete, and whatever is in the way is dealt
     * with at that point rather than after a full retry cycle has timed out. Reachability comes from
     * the live scene, which knows about closed doors the planner's map does not.</p>
     *
     * @param route the planned waypoints to walk
     * @return true when the walk should continue, false when it is blocked by something that could not
     *         be cleared
     */
    private boolean walk(List<WorldPoint> route) {
        if (route == null || route.isEmpty()) {
            return true;
        }

        int[] base = sceneBase();
        WorldPoint here = playerLocation();
        if (base == null || here == null) {
            return true;
        }

        List<WorldPoint> leg = SceneWindow.clip(route, base[0], base[1], here.getPlane());
        if (leg.isEmpty()) {
            return true;
        }

        // Reachability is tested on the dense leg. Striding spaces waypoints several tiles apart and
        // nudges them off the path at random, so a door is rarely standing on one and the blocked
        // tile would point nowhere useful.
        int blocked = obstacleRecovery.firstUnreachableIndex(leg);
        if (blocked == 0) {
            // The obstruction is already adjacent, so open it rather than setting off.
            return obstacleRecovery.clearAt(leg, 0);
        }

        List<WorldPoint> reachable = blocked < 0 ? leg : leg.subList(0, blocked);
        List<WorldPoint> waypoints = movementService.applyVariableStride(reachable);
        if (waypoints.isEmpty()) {
            return true;
        }

        if (!movementService.traversePath(ctx.getClient(), waypoints)) {
            // Reachable when planned and still failed, so something changed underfoot. The same
            // recovery covers a door that shut while the player was walking.
            return obstacleRecovery.clear(leg);
        }

        // Walked as far as the obstruction; now standing next to it, open it.
        return blocked < 0 || obstacleRecovery.clearAt(leg, blocked);
    }

    private WorldPoint playerLocation() {
        return ctx.players().local().location();
    }

    private int[] sceneBase() {
        return ctx.runOnClientThread(() -> {
            var worldView = ctx.getClient().getTopLevelWorldView();
            return new int[]{worldView.getBaseX(), worldView.getBaseY()};
        }, null);
    }

    private boolean isInInstance() {
        return Boolean.TRUE.equals(ctx.runOnClientThread(
                () -> ctx.getClient().getTopLevelWorldView().isInstance(), Boolean.FALSE));
    }
}
