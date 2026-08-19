package com.kraken.api.service.walker;

import com.kraken.api.Context;
import com.kraken.api.query.tileobject.TileObjectEntity;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.util.SleepService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Constants;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Locale;

/**
 * Opens whatever is blocking the next part of a walk.
 *
 * <p>The transport dataset lists only a couple of hundred doors for the whole game — the ones that
 * gate a route. Every other closed door is invisible to the planner: the shipped collision map often
 * marks the tile walkable, so a complete route is planned straight through a shut gate and nothing
 * reports it. The walk then simply stops getting anywhere.</p>
 *
 * <p>Reachability here comes from {@link TileService}, which floods the <em>live</em> scene rather
 * than the shipped map, so the client already knows the gate is shut. Checking that before setting off
 * turns a twenty second walk into a wall into an immediate click on the gate.</p>
 *
 * <p>An obstacle must sit on the blocked tile itself or the previous tile on the path. If one is
 * there and the player is within {@link #CLICK_RANGE_TILES}, it is clicked even when they are not
 * yet next to it — the client walks them to the door. A far unreachable waypoint with nothing to
 * open is walked toward. Being strict about which tiles are searched is what makes an over-eager
 * reachability reading harmless: a compressed path whose next waypoint is across a river is not a
 * door fifty tiles away.</p>
 */
@Slf4j
@Singleton
public class ObstacleRecovery {

    /**
     * How close the player must be before a blocked tile with nothing to open is treated as a stall
     * rather than walked toward. Matches {@code traversePath}'s "already there" radius.
     */
    public static final int INTERACT_RANGE_TILES = 2;

    /**
     * How far away an openable door may be clicked. The client walks the player to it. Farther than
     * this is still walked toward, so a compressed jump across a river is not treated as a clickable
     * door on the far bank.
     */
    public static final int CLICK_RANGE_TILES = 10;

    /** Furthest a single approach click will aim when the next waypoint is still far. */
    private static final int APPROACH_STEP_TILES = 16;

    /** Scenery that can be opened out of the way rather than walked around. */
    private static final String[] OBSTACLE_NAMES = {"door", "gate", "curtain"};

    /** Menu actions that open an obstacle, in the order they are preferred. */
    private static final String[] OBSTACLE_ACTIONS = {
            "Open", "Pay-toll(10gp)", "Pass", "Pass-through", "Go-through", "Push", "Push-through",
            "Squeeze-through", "Enter"
    };

    /** How long to wait for the obstacle to open. */
    private static final long OPEN_TIMEOUT_MS = 5_000;

    @Inject
    private Context ctx;

    @Inject
    private TileService tileService;

    /**
     * Why the last {@link #clearAt} failed, or null when it did not fail (including the skip while
     * the player is still moving, or when the blocked tile is too far away to open).
     *
     * <p>The walker copies this into {@link WalkResult} so a caller who never reads the log still
     * knows which door blocked the walk.</p>
     */
    @Getter
    private String lastFailureReason;

    /**
     * Returns the index of the first waypoint the player cannot currently walk to.
     *
     * <p>Used to cut a leg short at whatever is in the way, so the movement primitive is only ever
     * handed a stretch it can actually complete.</p>
     *
     * <p>Must be given the <em>dense</em> path. Strided waypoints are several tiles apart and are
     * randomly nudged off the path by up to a tile, so a door is rarely standing on one and the
     * blocked tile it reports would be nowhere near the thing actually in the way.</p>
     *
     * <p>The whole list is checked. A short window would miss a gate near the destination and the
     * walker would then click through it, waiting out a movement timeout instead of opening the
     * gate. The list is already clipped to the loaded scene, and reachability is tick-cached, so
     * scanning it is cheap next to walking into a wall.</p>
     *
     * @param waypoints the dense path about to be walked, ordered from the player outwards; may be null
     * @return the index of the first unreachable waypoint, or -1 when the whole leg is walkable
     */
    public int firstUnreachableIndex(List<WorldPoint> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return -1;
        }

        WorldPoint here = ctx.players().local().location();
        if (here == null) {
            return -1;
        }

        // Checking the far end first keeps the common case to a single reachability test: if the
        // player can already get to the end of the leg, nothing on it needs opening.
        WorldPoint furthest = lastWalkableCandidate(waypoints, here);
        if (furthest == null || tileService.isTileReachable(furthest)) {
            return -1;
        }

        for (int i = 0; i < waypoints.size(); i++) {
            WorldPoint point = waypoints.get(i);
            if (point == null || point.getPlane() != here.getPlane() || point.equals(here)) {
                continue;
            }

            if (!tileService.isTileReachable(point)) {
                return i;
            }
        }

        return -1;
    }

    /** The last tile on the leg worth testing, skipping nulls and other planes. */
    private WorldPoint lastWalkableCandidate(List<WorldPoint> waypoints, WorldPoint here) {
        for (int i = waypoints.size() - 1; i >= 0; i--) {
            WorldPoint point = waypoints.get(i);
            if (point != null && point.getPlane() == here.getPlane() && !point.equals(here)) {
                return point;
            }
        }

        return null;
    }

    /**
     * A reachable tile closer to an unreachable waypoint than the player is now.
     *
     * <p>Starts {@link #APPROACH_STEP_TILES} out and walks back until the live flood can reach the
     * candidate, so a compressed path that jumps a river still produces a click on this bank rather
     * than on the far one.</p>
     *
     * @param here where the player is standing, may be null
     * @param aim the unreachable waypoint, may be null
     * @return a closer reachable tile, or null when none is closer than here
     */
    public WorldPoint reachableToward(WorldPoint here, WorldPoint aim) {
        if (here == null || aim == null) {
            return null;
        }

        int dist = here.distanceTo2D(aim);
        if (dist <= INTERACT_RANGE_TILES) {
            return null;
        }

        int maxStep = Math.min(APPROACH_STEP_TILES, dist - 1);
        for (int step = maxStep; step >= 1; step--) {
            WorldPoint candidate = WalkPlan.closerTile(here, aim, step);
            if (candidate != null && !candidate.equals(here) && tileService.isTileReachable(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Whether the player is standing close enough to try opening whatever is blocking this index.
     *
     * @param here where the player is standing, may be null
     * @param path the dense path being walked, may be null
     * @param blockedIndex the index of the first unreachable waypoint
     * @return true when the blocked tile or the previous path tile is within interact range
     */
    public static boolean isWithinInteractRange(WorldPoint here, List<WorldPoint> path, int blockedIndex) {
        if (here == null || path == null || blockedIndex < 0 || blockedIndex >= path.size()) {
            return false;
        }

        WorldPoint blocked = path.get(blockedIndex);
        WorldPoint nearSide = blockedIndex > 0 ? path.get(blockedIndex - 1) : here;
        return isWithinInteractRange(here, blocked, nearSide);
    }

    /**
     * Whether an openable door or gate stands on the blockage and is close enough to click.
     *
     * <p>Used to decide between clicking Open from a few tiles away and walking toward a far
     * unreachable waypoint that has nothing to open.</p>
     *
     * @param path the dense path being walked, may be null
     * @param blockedIndex the index of the first unreachable waypoint
     * @return true when {@link #clearAt} should be tried rather than walking toward the tile
     */
    public boolean canOpen(List<WorldPoint> path, int blockedIndex) {
        if (path == null || blockedIndex < 0 || blockedIndex >= path.size()) {
            return false;
        }

        WorldPoint here = ctx.players().local() != null ? ctx.players().local().location() : null;
        WorldPoint blocked = path.get(blockedIndex);
        WorldPoint nearSide = blockedIndex > 0 ? path.get(blockedIndex - 1) : here;
        if (!isWithinClickRange(here, blocked, nearSide)) {
            return false;
        }

        TileObjectEntity obstacle = findObstacle(blocked, nearSide);
        return obstacle != null && chooseAction(actionsOf(obstacle)) != null;
    }

    /**
     * Whether the player is close enough to click an openable obstacle rather than walk toward it.
     *
     * @param here where the player is standing, may be null
     * @param blocked the first unreachable waypoint, may be null
     * @param nearSide the previous path tile, may be null
     * @return true when either tile is within {@link #CLICK_RANGE_TILES}
     */
    public static boolean isWithinClickRange(WorldPoint here, WorldPoint blocked, WorldPoint nearSide) {
        return isWithinRange(here, blocked, nearSide, CLICK_RANGE_TILES);
    }

    /**
     * Whether the player is standing next to a blocked tile or the near side of the doorway.
     *
     * @param here where the player is standing, may be null
     * @param blocked the first unreachable waypoint, may be null
     * @param nearSide the previous path tile, may be null
     * @return true when either tile is within {@link #INTERACT_RANGE_TILES}
     */
    public static boolean isWithinInteractRange(WorldPoint here, WorldPoint blocked, WorldPoint nearSide) {
        return isWithinRange(here, blocked, nearSide, INTERACT_RANGE_TILES);
    }

    private static boolean isWithinRange(WorldPoint here, WorldPoint blocked, WorldPoint nearSide, int range) {
        if (closeEnough(here, blocked, range)) {
            return true;
        }

        // Standing on the previous path tile only counts when that tile is not the player themselves.
        // A compressed path of [here, far bank] would otherwise look like "next to the doorway"
        // because the near side is the tile they are already standing on.
        return nearSide != null && !nearSide.equals(here) && closeEnough(here, nearSide, range);
    }

    private static boolean closeEnough(WorldPoint here, WorldPoint tile, int range) {
        return here != null
                && tile != null
                && here.getPlane() == tile.getPlane()
                && here.distanceTo(tile) <= range;
    }

    /**
     * Tries to open whatever stands between the path and the tile it cannot reach.
     *
     * <p>Does nothing while the player is still moving. A tile that cannot be reached from here often
     * becomes reachable a few steps along, and acting on that stale reading would have the walker
     * clicking a door it was already walking through.</p>
     *
     * <p>Does nothing when the blocked tile is still far away and nothing openable stands on it. A
     * compressed path can jump a river, and "cannot reach the far bank" is not a closed door; the
     * walker should step toward it instead. An openable door within {@link #CLICK_RANGE_TILES} is
     * clicked from here — the client walks the player to it.</p>
     *
     * @param path the dense path being walked
     * @param blockedIndex the index of the first tile on it that cannot be reached
     * @return true when something was opened, which means the walk is worth continuing
     */
    public boolean clearAt(List<WorldPoint> path, int blockedIndex) {
        lastFailureReason = null;

        if (path == null || blockedIndex < 0 || blockedIndex >= path.size()) {
            return false;
        }

        if (ctx.players().local().isMoving()) {
            log.debug("Walker: skipped obstacle check while moving");
            return false;
        }

        WorldPoint blocked = path.get(blockedIndex);
        if (blocked == null) {
            return false;
        }

        WorldPoint here = ctx.players().local().location();
        // A door stands on one of the two tiles it separates. The near side is the previous tile on
        // the path, not wherever the player happens to be standing, which may still be a long way off.
        WorldPoint nearSide = blockedIndex > 0 ? path.get(blockedIndex - 1) : here;

        TileObjectEntity obstacle = findObstacle(blocked, nearSide);
        String action = obstacle != null ? chooseAction(actionsOf(obstacle)) : null;
        if (action != null) {
            if (!isWithinClickRange(here, blocked, nearSide)) {
                log.debug("Walker: skipped opening {}; player is too far to click it", blocked);
                return false;
            }
        } else if (!isWithinInteractRange(here, blocked, nearSide)) {
            log.debug("Walker: skipped opening {}; player is not next to it", blocked);
            return false;
        } else if (obstacle == null) {
            return fail("cannot reach " + blocked + " from " + nearSide
                    + "; nothing matching door/gate/curtain stands there");
        } else {
            return fail("cannot reach " + blocked + " from " + nearSide + "; "
                    + describe(obstacle) + " offers no way through");
        }

        log.info("Walker: route blocked at {}; '{}' {}", blocked, action, describe(obstacle));
        if (!obstacle.interact(action)) {
            return fail("could not click '" + action + "' on " + describe(obstacle) + " at " + blocked);
        }

        long timeout = openTimeoutMs(here, blocked);
        if (!SleepService.sleepUntil(() -> tileService.isTileReachable(blocked), timeout)) {
            return fail("clicked '" + action + "' on " + describe(obstacle) + " at " + blocked
                    + " but it did not open");
        }

        log.info("Walker: opened {}; {} is now reachable", describe(obstacle), blocked);
        return true;
    }

    /**
     * How long to wait for an open to finish, including walking to the door.
     *
     * @param here where the player is standing, may be null
     * @param blocked the unreachable tile, may be null
     * @return milliseconds to wait
     */
    public static long openTimeoutMs(WorldPoint here, WorldPoint blocked) {
        int dist = 0;
        if (here != null && blocked != null && here.getPlane() == blocked.getPlane()) {
            dist = here.distanceTo(blocked);
        }
        return OPEN_TIMEOUT_MS + (long) dist * Constants.GAME_TICK_LENGTH;
    }

    /**
     * Finds the first unreachable waypoint of a leg and tries to clear it.
     *
     * @param path the leg that could not be walked; may be null
     * @return true when something was opened
     */
    public boolean clear(List<WorldPoint> path) {
        return clearAt(path, firstUnreachableIndex(path));
    }

    /**
     * Looks for an obstacle standing exactly on the blocked tile or on the tile before it.
     *
     * <p>A door occupies one of the two tiles it separates, so those are the only two worth checking.
     * Searching wider would start finding doors that are near the route but not on it.</p>
     */
    private TileObjectEntity findObstacle(WorldPoint blocked, WorldPoint nearSide) {
        TileObjectEntity onBlocked = obstacleAt(blocked);
        if (onBlocked != null) {
            return onBlocked;
        }

        return nearSide != null ? obstacleAt(nearSide) : null;
    }

    private TileObjectEntity obstacleAt(WorldPoint tile) {
        TileObjectEntity object = ctx.tileObjects()
                .at(tile)
                .filter(candidate -> isObstacle(candidate.getName()))
                .first();

        return object != null && object.isPresent() ? object : null;
    }

    private String[] actionsOf(TileObjectEntity obstacle) {
        ObjectComposition composition = obstacle.getObjectComposition();
        return composition != null ? composition.getActions() : null;
    }

    private boolean fail(String reason) {
        lastFailureReason = reason;
        log.info("Walker: {}", reason);
        return false;
    }

    private static String describe(TileObjectEntity obstacle) {
        return obstacle.getName() + " " + obstacle.getId();
    }

    /**
     * Reports whether a scenery name is something that can be opened out of the way.
     *
     * @param name the object's name, may be null
     * @return true when it looks like a door, gate or curtain
     */
    public static boolean isObstacle(String name) {
        if (name == null) {
            return false;
        }

        String lower = name.toLowerCase(Locale.ROOT);
        for (String candidate : OBSTACLE_NAMES) {
            if (lower.contains(candidate)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Picks the action that opens an obstacle.
     *
     * <p>Preference order matters: a gate offering both "Open" and "Climb-over" should be opened,
     * since climbing may cost agility experience or fail. Anything not on the list is ignored rather
     * than guessed at, so a locked door with only "Pick-lock" is reported as no way through.</p>
     *
     * @param actions the object's menu actions, may be null
     * @return the action to use, or null when none of them opens it
     */
    public static String chooseAction(String[] actions) {
        if (actions == null) {
            return null;
        }

        for (String preferred : OBSTACLE_ACTIONS) {
            for (String action : actions) {
                if (action != null && action.equalsIgnoreCase(preferred)) {
                    return action;
                }
            }
        }

        return null;
    }
}
