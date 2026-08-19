package com.kraken.api.service.walker;

import com.kraken.api.Context;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.magic.spellbook.Spellbook;
import com.kraken.api.service.magic.spellbook.Standard;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.pathfinding.PathfinderLiveConfig;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.TransportArrival;
import com.kraken.api.service.walker.transport.TransportExecutor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Collections;
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
 * fare, a transport whose destination chooser is not implemented, a stall, or an incomplete plan
 * that never reaches the destination. An incomplete route is not walked: the closest land the
 * search found cannot create a crossing that is not in the graph.</p>
 *
 * <h3>Doors the planner does not know about</h3>
 * <p>The transport dataset lists only the doors that gate a route. An ordinary closed door is just a
 * wall to the planner, so it routes around one where it can and never mentions it. Before a leg is
 * walked, {@link ObstacleRecovery} tests live-scene reachability and opens whatever stands on the
 * first unreachable tile when the player is close enough to click it. A compressed path that jumps a
 * river is walked toward, not treated as a missing door. After a successful open, the remainder of
 * the leg is walked in the same call. A stall after movement fails is only the backstop, for a door
 * that shuts mid-leg.</p>
 *
 * <h3>Home teleport</h3>
 * <p>The planner never picks the Lumbridge home teleport: walking is cheaper than its cooldown. When
 * the player is on the standard book, the thirty-minute timer is clear, they are more than fifty
 * tiles from the courtyard, and the path from the courtyard is shorter than the walk from here, the
 * walker casts it first and re-plans from Lumbridge. Farm to the castle bank uses it; farm to the
 * Grand Exchange does not. A nearby underground tile does not: 2D distance is not the remaining walk.
 * CS2 does not know about that timer, so cooldown is {@code AIDE_TELE_TIMER}, not {@code canCast}.</p>
 */
@Slf4j
@Singleton
public class Walker {

    /**
     * How many doors a single {@link #walk} call will open before giving the outer loop another
     * turn. A corridor of gates should not spin here forever.
     */
    private static final int MAX_OBSTACLE_CLEARS = 8;

    /**
     * How long to wait for leftover animation after a shortcut before the next walk click.
     *
     * <p>Idle is already required before operating a transport. Without the same wait here, the dest
     * click after Climb-into is eaten by the tunnel animation and MovementService times out.</p>
     */
    private static final long IDLE_BEFORE_WALK_MS = 8_000;

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

    @Inject
    private TileService tileService;

    @Inject
    private MagicService magicService;

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

            GlobalPathfinderConfig pathfinderConfig =
                    PathfinderLiveConfig.resolve(config.getPathfinderConfig(), ctx);
            GlobalPathfinder.PathResult route =
                    globalPathfinder.findPathResult(here, destination, pathfinderConfig);
            if (route == null || route.getPath().isEmpty()) {
                return WalkResult.failure(WalkOutcome.NO_ROUTE,
                        WalkPlan.noRouteReason(here, destination, null), here, round);
            }

            GlobalPathfinder.TransportUsage next = WalkPlan.firstTransport(route.getTransports());
            if (shouldHomeTeleport(here, destination, route, next, pathfinderConfig)
                    && castHomeTeleport(here)) {
                continue;
            }

            if (!WalkPlan.isFollowable(route.isComplete(), route.getPath(), destination, config.getTolerance())) {
                return WalkResult.failure(WalkOutcome.NO_ROUTE,
                        WalkPlan.noRouteReason(here, destination, route.getPath()), here, round);
            }

            WalkResult failure = advance(route, next, config, destination, round);
            if (failure != null) {
                return failure;
            }

            WorldPoint after = playerLocation();
            if (after == null) {
                return WalkResult.failure(WalkOutcome.UNKNOWN_POSITION,
                        "could not read the player's location", null, round);
            }

            int distance = WalkPlan.progressDistance(after, destination);
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
     * <p>When the next hop is a teleport whose landing is already walkable on this plane, the landing
     * is walked rather than the item being used. Jewellery has no origin tile, so the planner will
     * pick a glory stop six tiles past a nearby destination; skipping that as an "open door" is what
     * stalled a twelve-tile walk on Musa Point.</p>
     *
     * @param route the planned path and transports
     * @param next the first transport on that path, or null when the rest is walking
     * @param config tolerances and fire radius
     * @param destination the walk's destination, used when aborting on a blocked door
     * @param round which plan-and-walk round this is
     * @return null to continue, or the result that should end the walk
     */
    private WalkResult advance(GlobalPathfinder.PathResult route,
                               GlobalPathfinder.TransportUsage next,
                               WalkerConfig config,
                               WorldPoint destination,
                               int round) {
        if (next == null) {
            // The dense path, not the sparse one: walk() needs every tile to find what is in the way,
            // and applyVariableStride is what turns it into clicks. Arrival uses the walker's
            // tolerance so a one-tile miss of the Set tile is not a MovementService retry.
            if (!walk(route.getPath(), config.getTolerance())) {
                return blocked(destination, round);
            }
            return null;
        }

        if (!walk(WalkPlan.approach(route.getPath(), next.getPathIndex()),
                config.getTransportFireRadius())) {
            WalkResult failure = blocked(destination, round);
            if (failure != null) {
                return failure;
            }
            // Nowhere left to walk, but no door failed. Standing on a staircase origin looks like
            // that: the approach is the player's own tile, and the next waypoint is another plane.
            // Falling through still fires the transport when they are in range.
        }

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

        WorldPoint landing = next.getDestination();
        boolean landingReachable = landing != null && tileService.isTileReachable(landing);
        if (teleport && TransportArrival.landingWalkable(here, landing, landingReachable)) {
            log.info("Walker: {} already reachable at {}, walking instead of {}",
                    landing, here, next.getDisplayInfo());
            if (!walk(route.getPath(), config.getTolerance())) {
                return blocked(destination, round);
            }
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
     * <p>When none of the remaining path is in the loaded scene — a compressed route whose next tile
     * is eighty tiles away — the player is walked to the scene edge toward that tile, so the scene
     * shifts. An empty clip is not arrival. When they are already standing on that tile — a staircase
     * origin, or the scene edge itself — there is nothing to walk and this returns true so the
     * transport can be operated. Returning false here is what skipped Climb-up while standing on the
     * stairs.</p>
     *
     * <p>A waypoint that is in the scene but not reachable is opened when a door stands on it and
     * the player is close enough to click, even from a few tiles away. A compressed path can jump a
     * river, and the far bank is not a door; the player is walked toward it instead, so the walk does
     * not abort before they have moved.</p>
     *
     * <p>Opening a door is not the end of the leg. After a successful open, reachability is checked
     * again and whatever is now free is walked, so a courtyard door does not stall the walk on the
     * near side.</p>
     *
     * <p>The player is waited to idle first. After Climb-into the leftover animation would otherwise
     * eat the dest click. Approach walks use the fire radius so they still stop beside the origin;
     * dest walks use the walker's tolerance so a one-tile miss of the Set tile is not a timeout.</p>
     *
     * @param route the planned waypoints to walk
     * @param withinTiles how close to a waypoint counts as arrived, in tiles
     * @return true when the walk should continue, false when it is blocked by something that could not
     *         be cleared
     */
    private boolean walk(List<WorldPoint> route, int withinTiles) {
        if (route == null || route.isEmpty()) {
            return true;
        }

        SleepService.sleepUntil(() -> ctx.players().local().isIdle(), IDLE_BEFORE_WALK_MS);

        int[] base = sceneBase();
        WorldPoint here = playerLocation();
        if (base == null || here == null) {
            return true;
        }

        List<WorldPoint> leg = SceneWindow.clip(route, base[0], base[1], here.getPlane());
        if (nowhereToWalk(leg, here)) {
            WorldPoint edge = SceneWindow.toward(here, firstStepBeyond(route, here), base[0], base[1]);
            if (edge == null) {
                // Already on the only tile we would click: the staircase we are standing on, or the
                // scene edge toward a far waypoint. Not a failed walk.
                return true;
            }
            leg = Collections.singletonList(edge);
        }

        int opens = 0;
        while (true) {
            here = playerLocation();
            if (here == null) {
                return true;
            }

            // Reachability is tested on the dense leg. Striding spaces waypoints several tiles apart
            // and nudges them off the path at random, so a door is rarely standing on one and the
            // blocked tile would point nowhere useful.
            int blocked = obstacleRecovery.firstUnreachableIndex(leg);
            if (blocked >= 0 && obstacleRecovery.canOpen(leg, blocked)) {
                if (opens >= MAX_OBSTACLE_CLEARS || !obstacleRecovery.clearAt(leg, blocked)) {
                    return obstacleRecovery.getLastFailureReason() == null;
                }
                opens++;
                continue;
            }

            if (blocked >= 0 && !ObstacleRecovery.isWithinInteractRange(here, leg, blocked)) {
                return walkToward(leg, here, blocked, base);
            }

            if (blocked == 0) {
                if (opens >= MAX_OBSTACLE_CLEARS || !obstacleRecovery.clearAt(leg, 0)) {
                    return false;
                }
                opens++;
                continue;
            }

            List<WorldPoint> reachable = blocked < 0 ? leg : leg.subList(0, blocked);
            List<WorldPoint> waypoints = movementService.applyVariableStride(reachable);
            if (waypoints.isEmpty()) {
                return true;
            }

            if (!movementService.traversePath(ctx.getClient(), waypoints, withinTiles)) {
                // Reachable when planned and still failed, so something changed underfoot. The same
                // recovery covers a door that shut while the player was walking, including one a few
                // tiles ahead that can still be clicked.
                WorldPoint now = playerLocation();
                int again = obstacleRecovery.firstUnreachableIndex(leg);
                if (opens < MAX_OBSTACLE_CLEARS
                        && again >= 0
                        && (obstacleRecovery.canOpen(leg, again)
                        || ObstacleRecovery.isWithinInteractRange(now, leg, again))
                        && obstacleRecovery.clearAt(leg, again)) {
                    opens++;
                    continue;
                }
                return false;
            }

            if (blocked < 0) {
                return true;
            }

            if (opens >= MAX_OBSTACLE_CLEARS || !obstacleRecovery.clearAt(leg, blocked)) {
                return false;
            }
            opens++;
        }
    }

    /**
     * Walks toward an unreachable waypoint that is still too far away to open.
     *
     * <p>The reachable prefix of the path is used when it actually leaves the player's tile.
     * Otherwise a closer reachable tile toward the blockage is clicked, so a compressed jump across
     * a river still produces movement on this bank. Returning false here does not record a door
     * failure: there is nothing to open yet.</p>
     *
     * @param leg the scene-clipped path
     * @param here where the player is standing
     * @param blocked index of the first unreachable waypoint
     * @param base scene origin, used to aim a scene-edge click when the target is outside the window
     * @return true when a step was taken or there is nothing to do this round
     */
    private boolean walkToward(List<WorldPoint> leg, WorldPoint here, int blocked, int[] base) {
        WorldPoint aim = leg.get(blocked);
        log.info("Walker: cannot reach {} yet; walking toward it", aim);

        List<WorldPoint> reachable = blocked == 0
                ? Collections.emptyList()
                : leg.subList(0, blocked);
        if (!nowhereToWalk(reachable, here)) {
            List<WorldPoint> waypoints = movementService.applyVariableStride(reachable);
            if (waypoints.isEmpty()) {
                return true;
            }
            return movementService.traversePath(ctx.getClient(), waypoints);
        }

        WorldPoint step = obstacleRecovery.reachableToward(here, aim);
        if (step != null) {
            return movementService.traversePath(ctx.getClient(), Collections.singletonList(step));
        }

        WorldPoint edge = SceneWindow.toward(here, aim, base[0], base[1]);
        if (edge != null && !edge.equals(aim)) {
            return movementService.traversePath(ctx.getClient(), Collections.singletonList(edge));
        }

        return true;
    }

    /**
     * Whether the clipped leg has nowhere to click except the tile the player is already on.
     */
    private static boolean nowhereToWalk(List<WorldPoint> leg, WorldPoint here) {
        if (leg == null || leg.isEmpty()) {
            return true;
        }

        for (WorldPoint point : leg) {
            if (point != null && !point.equals(here)) {
                return false;
            }
        }

        return true;
    }

    /**
     * The first route tile that is not the player's current tile, used to aim a scene-edge click.
     */
    private static WorldPoint firstStepBeyond(List<WorldPoint> route, WorldPoint here) {
        if (route == null) {
            return null;
        }

        for (WorldPoint point : route) {
            if (point != null && !point.equals(here)) {
                return point;
            }
        }

        return null;
    }

    private WorldPoint playerLocation() {
        return ctx.players().local().location();
    }

    /**
     * Whether a Lumbridge home teleport would leave a shorter walk than the planned route.
     *
     * <p>Skipped when spells are disabled, the planner already picked a teleport, the player is on
     * another book, the thirty-minute timer is running, the client will not cast the spell, or they
     * are already near the courtyard. A 2D check skips the extra pathfind when Lumbridge is farther
     * from the destination; when both routes exist, the shorter dense path wins.</p>
     *
     * @param here where the player is standing
     * @param destination the walk's destination
     * @param walking the route from here
     * @param next the first transport on that route, may be null
     * @param pathfinderConfig whether teleport spells are allowed, already adjusted for this world
     * @return true when the home teleport should be cast this round
     */
    private boolean shouldHomeTeleport(WorldPoint here,
                                       WorldPoint destination,
                                       GlobalPathfinder.PathResult walking,
                                       GlobalPathfinder.TransportUsage next,
                                       GlobalPathfinderConfig pathfinderConfig) {
        if (pathfinderConfig == null || !pathfinderConfig.isUseTeleportationSpells()) {
            return false;
        }

        if (next != null && next.getType() != null && next.getType().isTeleport()) {
            return false;
        }

        if (!Spellbook.isOnStandardSpellbook()) {
            return skipHomeTeleport("not on the standard spellbook (" + Spellbook.getCurrentSpellbook() + ")");
        }

        if (!HomeTeleportPlan.isFarFromCourtyard(here)) {
            return false;
        }

        if (!HomeTeleportPlan.courtyardIsCloser(here, destination)) {
            return false;
        }

        int timer = ctx.getVarpValue(VarPlayerID.AIDE_TELE_TIMER);
        if (HomeTeleportPlan.isOnCooldown(timer, Instant.now())) {
            return skipHomeTeleport("on cooldown (AIDE_TELE_TIMER=" + timer + ")");
        }

        if (!magicService.canCast(Standard.HOME_TELEPORT)) {
            return skipHomeTeleport("client will not cast it (AIDE_TELE_TIMER=" + timer + ")");
        }

        GlobalPathfinder.PathResult viaCourtyard = globalPathfinder.findPathResult(
                HomeTeleportPlan.COURTYARD, destination, pathfinderConfig);
        if (viaCourtyard == null || viaCourtyard.getPath().isEmpty()) {
            return skipHomeTeleport("no route from the courtyard to " + destination);
        }

        int walkingTiles = pathTiles(walking);
        int courtyardTiles = pathTiles(viaCourtyard);
        if (!HomeTeleportPlan.preferViaCourtyard(
                here, walking.isComplete(), walkingTiles, viaCourtyard.isComplete(), courtyardTiles)) {
            return skipHomeTeleport("walking from here is " + walkingTiles
                    + " tiles, courtyard route is " + courtyardTiles
                    + " (walking complete=" + walking.isComplete()
                    + ", via courtyard complete=" + viaCourtyard.isComplete() + ")");
        }

        return true;
    }

    /**
     * How many tiles a planned route is.
     *
     * <p>Uses the dense path. Sparse waypoints are a click sequence and would make a long walk look
     * shorter than a denser courtyard route.</p>
     *
     * @param route the planned route, may be null
     * @return the number of tiles, or 0 when there is no path
     */
    private static int pathTiles(GlobalPathfinder.PathResult route) {
        if (route == null || route.getPath() == null) {
            return 0;
        }

        return route.getPath().size();
    }

    private boolean skipHomeTeleport(String reason) {
        log.info("Walker: skipping home teleport: {}", reason);
        return false;
    }

    /**
     * Channels the Lumbridge home teleport and waits for it to land.
     *
     * @param before where the player stood before the cast
     * @return true when they moved far enough that the teleport happened
     */
    private boolean castHomeTeleport(WorldPoint before) {
        SleepService.sleepUntil(() -> {
            var local = ctx.players().local();
            return local == null || !local.isMoving();
        }, 5_000);

        log.info("Walker: home teleport; walking from here is longer than from the Lumbridge courtyard");
        if (!magicService.cast(Standard.HOME_TELEPORT)) {
            return false;
        }

        return SleepService.sleepUntil(() -> {
            WorldPoint now = playerLocation();
            return now != null && before != null && now.distanceTo2D(before) > 10;
        }, HomeTeleportPlan.CAST_TIMEOUT_MS);
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

    /**
     * Ends the walk when an obstacle could not be cleared.
     *
     * <p>Returning false from {@link #walk} while the player is still moving is not a failure — the
     * skip is expected — so this only aborts when {@link ObstacleRecovery} recorded a reason.</p>
     *
     * @param destination where the walk was heading
     * @param round which plan-and-walk round this is
     * @return a stalled result, or null when the miss was only the moving skip
     */
    private WalkResult blocked(WorldPoint destination, int round) {
        String reason = obstacleRecovery.getLastFailureReason();
        if (reason == null) {
            return null;
        }

        WorldPoint here = playerLocation();
        log.info("Walker: aborting at {}, heading for {}: {}", here, destination, reason);
        return WalkResult.failure(WalkOutcome.STALLED, reason, here, round);
    }
}
