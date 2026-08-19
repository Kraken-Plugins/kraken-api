package plugins.api.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.walker.WalkResult;
import com.kraken.api.service.walker.Walker;
import com.kraken.api.service.walker.WalkerConfig;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.TargetTileProvider;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TargetTile;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;

import java.util.List;

/**
 * Walks to a chosen tile, operating whatever transports the route needs.
 *
 * <p>The destination is picked in game by shift right clicking "Walk here" then "Set", which is what
 * makes this test able to exercise transports the harness could never reach on its own — point it at
 * somewhere behind a door, across a boat trip, or through a fairy ring and the same test covers it.
 * A tile picked that way always wins; the relative tile declared in {@link #requirements()} is only a
 * fallback so an unattended suite run still has somewhere to go.</p>
 *
 * <p>The route is planned and logged before the walk starts. That log is the point of the test as much
 * as the pass or fail is: it names every transport the planner intends to use, so a failure can be
 * traced to the transport that did not work rather than to "the walk did not finish".</p>
 */
@Slf4j
public class WalkerTest extends BaseApiTest {

    /** How long to wait for a tile to be chosen in game, in tenths of a second. */
    private static final int TARGET_SELECTION_TIMEOUT = 300;

    /** Budget for the walk itself. Generous, because a transport route can be long. */
    private static final long WALK_TIMEOUT_MS = 240_000;

    @Inject
    private Context ctx;

    @Inject
    private Walker walker;

    @Inject
    private GlobalPathfinder globalPathfinder;

    @Inject
    private TargetTileProvider targetTileProvider;

    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder()
                .targetTile(TargetTile.relativeToPlayer(12, 12))
                .sideEffect(SideEffect.MOVES_PLAYER)
                .sideEffect(SideEffect.TELEPORTS)
                .sideEffect(SideEffect.CONSUMES_ITEMS)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        log.info("Shift + Right Click 'Walk here' -> 'Set' on a tile to choose where to walk.");

        WorldPoint target = waitForTargetSelection();
        if (!assertNotNull(target, "a destination tile was chosen")) {
            return false;
        }

        WorldPoint start = ctx.players().local().location();
        if (!assertNotNull(start, "the player's location could be read")) {
            return false;
        }

        log.info("Walking from {} to {} ({} tiles apart)", start, target, start.distanceTo(target));
        describeRoute(start, target);

        WalkResult result = walker.walkTo(target, WalkerConfig.builder()
                .timeoutMillis(WALK_TIMEOUT_MS)
                .build());

        log.info("Walk finished: {}", result);

        WorldPoint end = ctx.players().local().location();
        log.info("Ended at {}, {} tiles from the destination", end,
                end != null ? end.distanceTo(target) : -1);

        return assertThat(result.isSuccess(), "the walk reached its destination: " + result.getReason());
    }

    /**
     * Logs the transports the planner intends to use before anything is clicked.
     *
     * <p>Without this a failure only says the walk did not finish. With it the log names the transport
     * that was about to be operated, its type, and what the dataset says to click, which is what makes
     * an in-client failure diagnosable.</p>
     *
     * @param start where the walk begins
     * @param target where it is headed
     */
    private void describeRoute(WorldPoint start, WorldPoint target) {
        GlobalPathfinder.PathResult route = globalPathfinder.findPathResult(start, target);
        if (route == null || route.getPath().isEmpty()) {
            log.warn("The planner found no route, so the walk is expected to fail");
            return;
        }

        List<GlobalPathfinder.TransportUsage> transports = route.getTransports();
        log.info("Planned route: {} tiles, complete={}, {} transports",
                route.getPath().size(), route.isComplete(), transports.size());

        for (GlobalPathfinder.TransportUsage usage : transports) {
            log.info("  at step {}: {} {} -> {} | object='{}' display='{}'",
                    usage.getPathIndex(),
                    usage.getType(),
                    usage.getOrigin(),
                    usage.getDestination(),
                    usage.getObjectInfo(),
                    usage.getDisplayInfo());
        }
    }

    /**
     * Waits for a destination, preferring one the user picked in game.
     *
     * <p>A tile picked by hand wins over the one this test declares in {@link #requirements()}. The
     * declared tile exists only so an unattended suite run has somewhere to go; without this
     * preference the runner publishes it before the test starts and silently overrides the selection,
     * which is the opposite of what someone shift right clicking a tile expects.</p>
     *
     * @return the destination, or null when none was chosen within the timeout
     */
    private WorldPoint waitForTargetSelection() throws InterruptedException {
        WorldPoint manual = targetTileProvider.getManualTile();
        if (manual != null) {
            log.info("Using the tile you picked: {}", manual);
            return manual;
        }

        int elapsed = 0;
        while (targetTileProvider.getManualTile() == null
                && targetTileProvider.get() == null
                && elapsed < TARGET_SELECTION_TIMEOUT) {
            Thread.sleep(100);
            elapsed++;
        }

        WorldPoint picked = targetTileProvider.getManualTile();
        if (picked != null) {
            log.info("Using the tile you picked: {}", picked);
            return picked;
        }

        WorldPoint declared = targetTileProvider.get();
        if (declared != null) {
            log.info("No tile was picked, falling back to the declared one: {}", declared);
        }
        return declared;
    }

    @Override
    public String getTestName() {
        return "Walker";
    }
}
