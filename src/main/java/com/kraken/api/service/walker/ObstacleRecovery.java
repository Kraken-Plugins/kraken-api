package com.kraken.api.service.walker;

import com.kraken.api.Context;
import com.kraken.api.query.tileobject.TileObjectEntity;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
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
 * <p>An obstacle must sit on the blocked tile itself or the tile the player is standing on. Being
 * strict about that is what makes an over-eager reachability reading harmless: if nothing openable is
 * exactly there, nothing happens and the walk proceeds as before.</p>
 */
@Slf4j
@Singleton
public class ObstacleRecovery {

    /** Scenery that can be opened out of the way rather than walked around. */
    private static final String[] OBSTACLE_NAMES = {"door", "gate", "curtain"};

    /** Menu actions that open an obstacle, in the order they are preferred. */
    private static final String[] OBSTACLE_ACTIONS = {
            "Open", "Pass", "Pass-through", "Go-through", "Push", "Push-through", "Squeeze-through", "Enter"
    };

    /** How long to wait for the obstacle to open. */
    private static final long OPEN_TIMEOUT_MS = 5_000;

    /**
     * How far along the path to look for something in the way.
     *
     * <p>Each reachability test costs a hop to the client thread, and anything much further off is
     * either outside the loaded scene or will be re-evaluated on a later round anyway.</p>
     */
    private static final int LOOKAHEAD_TILES = 32;

    @Inject
    private Context ctx;

    @Inject
    private TileService tileService;

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

        int limit = Math.min(waypoints.size(), LOOKAHEAD_TILES);

        // Checking the far end first keeps the common case to a single reachability test: if the
        // player can already get to the end of the window, nothing inside it needs opening.
        WorldPoint furthest = lastWalkableCandidate(waypoints, limit, here);
        if (furthest == null || tileService.isTileReachable(furthest)) {
            return -1;
        }

        for (int i = 0; i < limit; i++) {
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

    /** The last tile in the window worth testing, skipping nulls and other planes. */
    private WorldPoint lastWalkableCandidate(List<WorldPoint> waypoints, int limit, WorldPoint here) {
        for (int i = limit - 1; i >= 0; i--) {
            WorldPoint point = waypoints.get(i);
            if (point != null && point.getPlane() == here.getPlane() && !point.equals(here)) {
                return point;
            }
        }

        return null;
    }

    /**
     * Tries to open whatever stands between the path and the tile it cannot reach.
     *
     * <p>Does nothing while the player is still moving. A tile that cannot be reached from here often
     * becomes reachable a few steps along, and acting on that stale reading would have the walker
     * clicking a door it was already walking through.</p>
     *
     * @param path the dense path being walked
     * @param blockedIndex the index of the first tile on it that cannot be reached
     * @return true when something was opened, which means the walk is worth continuing
     */
    public boolean clearAt(List<WorldPoint> path, int blockedIndex) {
        if (path == null || blockedIndex < 0 || blockedIndex >= path.size()) {
            return false;
        }

        if (ctx.players().local().isMoving()) {
            return false;
        }

        WorldPoint blocked = path.get(blockedIndex);
        if (blocked == null) {
            return false;
        }

        // A door stands on one of the two tiles it separates. The near side is the previous tile on
        // the path, not wherever the player happens to be standing, which may still be a long way off.
        WorldPoint nearSide = blockedIndex > 0
                ? path.get(blockedIndex - 1)
                : ctx.players().local().location();

        TileObjectEntity obstacle = findObstacle(blocked, nearSide);
        if (obstacle == null) {
            log.info("Cannot reach {} and nothing openable stands there or on {}", blocked, nearSide);
            return false;
        }

        String action = chooseAction(actionsOf(obstacle));
        if (action == null) {
            log.info("{} at {} offers no way through", obstacle.getName(), blocked);
            return false;
        }

        log.info("Route is blocked at {}; '{}' the {}", blocked, action, obstacle.getName());
        if (!obstacle.interact(action)) {
            return false;
        }

        return SleepService.sleepUntil(() -> tileService.isTileReachable(blocked), OPEN_TIMEOUT_MS);
    }

    /**
     * Finds the first unreachable waypoint of a leg and tries to clear it.
     *
     * @param waypoints the leg that could not be walked; may be null
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
