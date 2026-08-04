package plugins.api.precondition;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.suite.CancellationToken;
import plugins.api.world.NamedLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves the player between the handful of places the test suite runs.
 *
 * <p>This is deliberately <b>not</b> a general "walk anywhere" API. Executing transports — doors,
 * stairs, boats, teleports — is where cross-map walking gets genuinely hard, and the suite does not
 * need any of it: after the test condensing there are only two stops, the hub and the Grand Exchange,
 * with roughly ninety tiles of open Varrock between them and no doors on the route. So this plans a
 * route, walks whatever part of it is in the loaded scene, re-plans, and repeats. If it stops making
 * progress it gives up and says so, and the test that wanted to travel is skipped with a reason
 * rather than the run hanging.</p>
 *
 * <p>Everything here blocks and must be called off the client thread.</p>
 */
@Slf4j
@Singleton
public class SuiteWalker {

    /** Tiles trimmed from each scene edge; the outer ring is often unwalkable or unrendered. */
    private static final int SCENE_MARGIN = 8;

    /** Scene is 104x104; the usable window after trimming both edges. */
    private static final int SCENE_SIZE = 104;

    /** How close counts as arrived. Cannot go below 2: traversePath itself stops within 2 tiles. */
    private static final int ARRIVAL_TOLERANCE = 3;

    /** Give up after this many plan-and-walk rounds. */
    private static final int MAX_ROUNDS = 25;

    /** Net tiles that must be gained in a round for it to count as progress. */
    private static final int MIN_PROGRESS_TILES = 3;

    /** Consecutive rounds without progress before declaring the player stuck. */
    private static final int MAX_STALLED_ROUNDS = 3;

    /** Overall budget for a single walk. */
    private static final long WALK_TIMEOUT_MS = 180_000;

    @Inject
    private Context ctx;

    @Inject
    private MovementService movementService;

    @Inject
    private GlobalPathfinder globalPathfinder;

    @Inject
    private Waiter waiter;

    /**
     * Walks to a named location, stopping anywhere inside it.
     *
     * @param location where to go; {@link NamedLocation#ANYWHERE} is a no-op
     * @param token polled throughout so a cancelled run stops promptly
     * @return true when the player is at the location
     */
    public boolean walkTo(NamedLocation location, CancellationToken token) {
        if (location == null || location == NamedLocation.ANYWHERE || location.getAnchor() == null) {
            return true;
        }

        WorldPoint start = playerLocation();
        if (start != null && location.contains(start)) {
            return true;
        }

        log.info("Walking to {}", location.getDisplayName());
        return walkTo(location.getAnchor(), Math.max(location.getDefaultRadius(), ARRIVAL_TOLERANCE), token);
    }

    /**
     * Walks to a destination tile.
     *
     * @param destination the tile to reach
     * @param tolerance how close counts as arrived, in tiles
     * @param token polled throughout so a cancelled run stops promptly
     * @return true when the player is within {@code tolerance} of the destination
     */
    public boolean walkTo(WorldPoint destination, int tolerance, CancellationToken token) {
        if (destination == null) {
            return false;
        }

        // Every wait below no-ops on the client thread, which would turn this into a spin that fails
        // in milliseconds for no visible reason.
        if (ctx.getClient().isClientThread()) {
            log.error("SuiteWalker blocks and must not be called on the client thread");
            return false;
        }

        if (isInInstance()) {
            log.error("SuiteWalker cannot route out of an instance: instanced coordinates do not "
                    + "correspond to the static collision map");
            return false;
        }

        int effectiveTolerance = Math.max(tolerance, ARRIVAL_TOLERANCE);
        long deadline = System.currentTimeMillis() + WALK_TIMEOUT_MS;
        int stalledRounds = 0;
        int previousDistance = Integer.MAX_VALUE;

        for (int round = 0; round < MAX_ROUNDS; round++) {
            token.throwIfCancelled("walking to " + destination);

            if (System.currentTimeMillis() > deadline) {
                log.error("Walk to {} exceeded its {}ms budget", destination, WALK_TIMEOUT_MS);
                return false;
            }

            WorldPoint here = playerLocation();
            if (here == null) {
                log.error("Walk to {} aborted, could not read the player's location", destination);
                return false;
            }

            if (here.distanceTo(destination) <= effectiveTolerance) {
                return true;
            }

            List<WorldPoint> route = globalPathfinder.findSparsePath(here, destination);
            if (route == null || route.isEmpty()) {
                log.error("No route from {} to {}", here, destination);
                return false;
            }

            List<WorldPoint> leg = sceneLeg(route, here);
            if (leg.isEmpty()) {
                log.error("Route to {} leaves the loaded scene immediately, which usually means it "
                        + "needs a transport this walker does not execute", destination);
                return false;
            }

            movementService.traversePath(ctx.getClient(), movementService.applyVariableStride(leg));

            WorldPoint after = playerLocation();
            if (after == null) {
                return false;
            }

            int distance = after.distanceTo(destination);
            if (previousDistance - distance < MIN_PROGRESS_TILES) {
                // traversePath already retries individual waypoints, so one bad round is not fatal;
                // several in a row means something is genuinely in the way.
                if (++stalledRounds >= MAX_STALLED_ROUNDS) {
                    log.error("Stuck {} tiles from {} after {} rounds without progress",
                            distance, destination, stalledRounds);
                    return false;
                }
                waiter.sleep(600, token);
            } else {
                stalledRounds = 0;
            }

            previousDistance = distance;
        }

        log.error("Walk to {} gave up after {} rounds", destination, MAX_ROUNDS);
        return false;
    }

    /**
     * Takes the leading part of a route that lies within the currently loaded scene.
     *
     * <p>The scene is only 104 tiles square, so a cross-map route mostly refers to tiles the client
     * cannot click yet. Walking the part that is loaded, then re-planning once the scene has shifted,
     * is what lets a short-range movement primitive cover a long route.</p>
     *
     * @param route the planned waypoints, ordered from the player outwards
     * @param here the player's current tile, used to derive the scene window
     * @return the contiguous prefix of {@code route} inside the scene, possibly empty
     */
    private List<WorldPoint> sceneLeg(List<WorldPoint> route, WorldPoint here) {
        int[] base = sceneBase();
        if (base == null) {
            return new ArrayList<>();
        }

        return sceneLeg(route, base[0], base[1], here.getPlane(), SCENE_MARGIN);
    }

    /**
     * Clips a route to the part of it that lies within a scene window.
     *
     * <p>Pure arithmetic, kept public and static so it can be unit tested without a running client —
     * it is the only part of the walk loop that is testable that way, everything around it blocks on
     * the game.</p>
     *
     * <p>Truncation stops at the <em>first</em> point outside the window, even if later points come
     * back inside. A route that leaves and returns must not be stitched together, because the tiles
     * in between are not clickable and walking the later section would jump the player's click across
     * the gap.</p>
     *
     * @param route the planned waypoints, ordered from the player outwards; may be null
     * @param baseX world x of the scene's south west corner
     * @param baseY world y of the scene's south west corner
     * @param plane the plane the player is on
     * @param margin tiles to trim from each scene edge
     * @return the contiguous prefix of {@code route} inside the trimmed scene window, possibly empty
     */
    public static List<WorldPoint> sceneLeg(List<WorldPoint> route, int baseX, int baseY, int plane, int margin) {
        List<WorldPoint> leg = new ArrayList<>();
        if (route == null) {
            return leg;
        }

        int minX = baseX + margin;
        int minY = baseY + margin;
        int maxX = baseX + SCENE_SIZE - 1 - margin;
        int maxY = baseY + SCENE_SIZE - 1 - margin;

        for (WorldPoint point : route) {
            // A plane change means stairs or a ladder, which this walker does not operate.
            if (point.getPlane() != plane) {
                break;
            }

            if (point.getX() < minX || point.getX() > maxX
                    || point.getY() < minY || point.getY() > maxY) {
                break;
            }

            leg.add(point);
        }

        return leg;
    }

    /**
     * Reads the player's current tile.
     *
     * @return the tile, or null when it could not be read
     */
    private WorldPoint playerLocation() {
        return ctx.players().local().location();
    }

    /**
     * Reads the south west corner of the loaded scene.
     *
     * @return a two element array of world x and y, or null when it could not be read
     */
    private int[] sceneBase() {
        return ctx.runOnClientThread(() -> {
            var worldView = ctx.getClient().getTopLevelWorldView();
            return new int[]{worldView.getBaseX(), worldView.getBaseY()};
        });
    }

    /**
     * Reports whether the player is inside an instanced area.
     *
     * @return true when the top level world view is an instance
     */
    private boolean isInInstance() {
        Boolean instanced = ctx.runOnClientThread(() -> ctx.getClient().getTopLevelWorldView().isInstance());
        return Boolean.TRUE.equals(instanced);
    }
}
