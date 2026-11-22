package plugins.api.precondition;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.WalkResult;
import com.kraken.api.service.walker.Walker;
import com.kraken.api.service.walker.WalkerConfig;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.ApiTestConfig;
import plugins.api.suite.CancellationToken;
import plugins.api.world.NamedLocation;
import plugins.api.world.RouteInspector;

/**
 * Moves the player between the places the test suite runs, using the walker under test.
 *
 * <p>Every trip goes through {@link Walker}: it is planned over the bundled collision map and
 * transport dataset, and the walker operates whatever the route needs on the way, such as doors,
 * stairs, shortcuts, rides and the home teleport. The walk between two tests is coverage of its
 * own that way. A transport that stops working shows up as the next test skipped, with the
 * walker's outcome and reason as the cause.</p>
 *
 * <p>Travel spends nothing unless the plugin's Travel Uses Consumables setting is on. Item setup
 * happens before the trip to where a test runs, so a route that broke a tablet, cast a rune
 * teleport or paid a fare could use up what the test is about to assert on.</p>
 *
 * <p>Everything here blocks and must be called off the client thread.</p>
 */
@Slf4j
@Singleton
public class SuiteWalker {

    /** How close to a location's anchor the player is walked. */
    private static final int ARRIVAL_TOLERANCE = 3;

    /** Budget for one trip. The longest, Lumbridge to the Grand Exchange, is over 300 tiles on foot. */
    private static final long WALK_TIMEOUT_MS = 360_000;

    @Inject
    private Context ctx;

    @Inject
    private Walker walker;

    @Inject
    private RouteInspector routes;

    @Inject
    private ApiTestConfig config;

    /**
     * Walks to a named location.
     *
     * @param location where to go; {@link NamedLocation#ANYWHERE} needs no travel
     * @param token checked before and after the walk; cancelling it also interrupts the walk
     * @return how the walk ended, which is a success without moving when the player is already there
     * @throws plugins.api.suite.TestCancelledException if the run is cancelled during the walk
     */
    public WalkResult walkTo(NamedLocation location, CancellationToken token) {
        WorldPoint start = ctx.players().local().location();
        if (location == null || location == NamedLocation.ANYWHERE || location.getAnchor() == null
                || location.contains(start)) {
            return WalkResult.success(start, 0);
        }

        String trip = "travelling to " + location.getDisplayName();
        token.throwIfCancelled(trip);

        WalkerConfig walkerConfig = travelConfig(config.travelUsesConsumables());
        WorldPoint destination = location.getAnchor();
        log.info("Travelling from {} to {} at {}", start, location.getDisplayName(), destination);

        WalkResult result;
        try {
            // Planning refuses the client thread by throwing. The walker refuses it with a result,
            // which is the one worth reporting, so the route is only logged from a worker.
            if (start != null && !ctx.getClient().isClientThread()) {
                routes.planAndLog(start, destination, walkerConfig);
            }
            result = walker.walkTo(destination, walkerConfig);
        } catch (RuntimeException e) {
            // Cancelling a run interrupts the worker, and the walker's waits end an interrupted
            // wait with an exception of their own. That is a stopped run, not a failed setup.
            token.throwIfCancelled(trip);
            throw e;
        }
        token.throwIfCancelled(trip);

        if (result.isSuccess()) {
            log.info("Arrived at {} after {} rounds", location.getDisplayName(), result.getRounds());
        } else {
            log.error("Could not travel to {}: {}", location.getDisplayName(), describeFailure(result));
        }
        return result;
    }

    /**
     * Builds the walker settings for a trip between tests.
     *
     * <p>Without consumables the planner is kept away from teleport items and teleport spells, and
     * from every fare, by allowing it to spend nothing of any currency. What is left costs nothing:
     * doors, stairs, shortcuts, free rides and the home teleport. The wilderness is avoided either
     * way.</p>
     *
     * @param allowConsumables whether a trip may use up runes, charges, tablets and coins
     * @return the settings to walk with
     */
    public static WalkerConfig travelConfig(boolean allowConsumables) {
        WalkerConfig defaults = WalkerConfig.builder().build();
        GlobalPathfinderConfig.GlobalPathfinderConfigBuilder planning =
                defaults.getPathfinderConfig().toBuilder().avoidWilderness(true);

        if (!allowConsumables) {
            planning.useTeleportationItems(false)
                    .useTeleportationSpells(false)
                    .currencyThreshold(0);
        }

        return defaults.toBuilder()
                .tolerance(ARRIVAL_TOLERANCE)
                .timeoutMillis(WALK_TIMEOUT_MS)
                .pathfinderConfig(planning.build())
                .build();
    }

    /**
     * Describes a walk that did not arrive, for a skip reason or a log line.
     *
     * @param result the result of the walk
     * @return the walker's outcome and its reason
     */
    public static String describeFailure(WalkResult result) {
        return result.getOutcome() + " (" + result.getReason() + ")";
    }
}
