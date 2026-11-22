package plugins.api.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.pathfinding.PathfinderLiveConfig;
import com.kraken.api.service.walker.WalkerConfig;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans the routes the harness walks and reports what a planned route uses.
 *
 * <p>A route is planned with the settings of the walk it belongs to, adjusted for the world the
 * player is on, so the route that is logged or checked is the one the walker follows. Planning
 * blocks and must be called off the client thread. The checks are pure and work on any planned
 * route. A route's transports are read off its tiles, so they always line up with it; what is
 * worth checking is which transports a route takes.</p>
 */
@Slf4j
@Singleton
public class RouteInspector {

    @Inject
    private Context ctx;

    @Inject
    private GlobalPathfinder globalPathfinder;

    /**
     * Plans a route the way a walk with these settings plans it.
     *
     * @param start where the route begins
     * @param target where it is headed
     * @param walkerConfig the settings of the walk the route is for
     * @return the planned route, which is incomplete or empty when the target cannot be reached
     */
    public GlobalPathfinder.PathResult plan(WorldPoint start, WorldPoint target, WalkerConfig walkerConfig) {
        return globalPathfinder.findPathResult(start, target,
                PathfinderLiveConfig.resolve(walkerConfig.getPathfinderConfig(), ctx));
    }

    /**
     * Plans a route and logs every transport it intends to use, so a walk that fails can be traced
     * to the transport that did not work.
     *
     * @param start where the route begins
     * @param target where it is headed
     * @param walkerConfig the settings of the walk the route is for
     * @return the planned route
     */
    public GlobalPathfinder.PathResult planAndLog(WorldPoint start, WorldPoint target, WalkerConfig walkerConfig) {
        GlobalPathfinder.PathResult route = plan(start, target, walkerConfig);
        for (String line : describe(route)) {
            log.info("{}", line);
        }
        return route;
    }

    /**
     * Describes a planned route: its length, and one line per transport with its kind, both tiles,
     * its definition and what it clicks.
     *
     * @param route the planned route, may be null
     * @return the lines to log
     */
    public static List<String> describe(GlobalPathfinder.PathResult route) {
        List<String> lines = new ArrayList<>();
        if (route == null || route.getPath().isEmpty()) {
            lines.add("The planner found no route, so the walk is expected to fail");
            return lines;
        }

        List<WorldPoint> path = route.getPath();
        lines.add("Planned route: " + path.size() + " tiles, complete=" + route.isComplete()
                + ", " + route.getTransports().size() + " transports, first=" + path.get(0)
                + " last=" + path.get(path.size() - 1));

        for (GlobalPathfinder.TransportUsage usage : route.getTransports()) {
            TransportDefinition definition = usage.getDefinition();
            lines.add("  at step " + usage.getPathIndex() + ": " + usage.getType()
                    + " " + usage.getOrigin() + " -> " + usage.getDestination()
                    + " | id=" + (definition != null ? definition.getId() : null)
                    + " object='" + usage.getObjectInfo() + "'"
                    + " display='" + usage.getDisplayInfo() + "'");
        }
        return lines;
    }

    /**
     * Picks the transports of a route that move the player to another plane, in route order.
     *
     * @param route the planned route, may be null
     * @return the plane changing transports, empty when the route stays on one plane
     */
    public static List<GlobalPathfinder.TransportUsage> planeChanges(GlobalPathfinder.PathResult route) {
        List<GlobalPathfinder.TransportUsage> changes = new ArrayList<>();
        if (route == null) {
            return changes;
        }

        for (GlobalPathfinder.TransportUsage usage : route.getTransports()) {
            if (usage.getOrigin() != null && usage.getDestination() != null
                    && usage.getOrigin().getPlane() != usage.getDestination().getPlane()) {
                changes.add(usage);
            }
        }
        return changes;
    }

    /**
     * Reports whether a route changes plane by transports entered in the scene, one after the
     * other, from one plane to another. A teleport that lands on the other plane does not count,
     * since nothing was climbed. A route that changes no plane leads from a plane to itself.
     *
     * @param route the planned route, may be null
     * @param fromPlane the plane the climbing starts on
     * @param toPlane the plane it has to end on
     * @return true when stairs, ladders or the like lead from the one plane to the other
     */
    public static boolean climbs(GlobalPathfinder.PathResult route, int fromPlane, int toPlane) {
        int plane = fromPlane;
        for (GlobalPathfinder.TransportUsage usage : planeChanges(route)) {
            if (usage.isTeleport() || usage.getOrigin().getPlane() != plane) {
                return false;
            }
            plane = usage.getDestination().getPlane();
        }
        return plane == toPlane;
    }

    /**
     * Reports whether a route goes through the transport with a given definition id.
     *
     * @param route the planned route, may be null
     * @param definitionId the id of the definition to look for
     * @return true when one of the route's transports has that definition
     */
    public static boolean usesDefinition(GlobalPathfinder.PathResult route, String definitionId) {
        if (route == null || definitionId == null) {
            return false;
        }

        for (GlobalPathfinder.TransportUsage usage : route.getTransports()) {
            if (usage.getDefinition() != null && definitionId.equals(usage.getDefinition().getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reports whether a route takes a transport from one tile to another.
     *
     * @param route the planned route, may be null
     * @param origin the tile the transport is entered from
     * @param destination the tile it leads to
     * @return true when one of the route's transports joins exactly those two tiles
     */
    public static boolean usesEdge(GlobalPathfinder.PathResult route, WorldPoint origin, WorldPoint destination) {
        if (route == null || origin == null || destination == null) {
            return false;
        }

        for (GlobalPathfinder.TransportUsage usage : route.getTransports()) {
            if (origin.equals(usage.getOrigin()) && destination.equals(usage.getDestination())) {
                return true;
            }
        }
        return false;
    }
}
