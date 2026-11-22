package plugins.api.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.walker.WalkResult;
import com.kraken.api.service.walker.Walker;
import com.kraken.api.service.walker.WalkerConfig;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.TargetTileProvider;
import plugins.api.WalkerDestination;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TargetTile;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.RouteInspector;

/**
 * Walks to a chosen tile, operating whatever transports the route needs.
 *
 * <p>The destination is a named place from the plugin config, a tile shift-clicked in game when
 * that config is Manual, or a nearby fallback so an unattended suite run still has somewhere to go.
 * Picking Karamja after a shift-click uses Karamja; the leftover Set tile is ignored until the
 * dropdown is Manual again. Named places cover the walks worth repeating after a rebuild: the Grand
 * Exchange, a castle bank upstairs, a gate, a boat.</p>
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
    private RouteInspector routes;

    @Inject
    private TargetTileProvider targetTileProvider;

    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder()
                .targetTile(TargetTile.relativeToPlayer(12, 12))
                .sideEffect(SideEffect.MOVES_PLAYER)
                .sideEffect(SideEffect.TELEPORTS)
                .sideEffect(SideEffect.CONSUMES_ITEMS)
                .timeoutMs(WALK_TIMEOUT_MS)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        WalkerDestination configured = config.walkerDestination();
        if (configured != null && configured.getTile() != null) {
            log.info("Configured destination is {}.", configured.getDisplayName());
        } else {
            log.info("Shift + Right Click 'Walk here' -> 'Set' on a tile to choose where to walk.");
        }

        WorldPoint target = resolveDestination();
        if (!assertNotNull(target, "a destination tile was chosen")) {
            return false;
        }

        WorldPoint start = ctx.players().local().location();
        if (!assertNotNull(start, "the player's location could be read")) {
            return false;
        }

        log.info("Walking from {} to {} ({} tiles apart, plane {} -> {})",
                start, target, start.distanceTo2D(target), start.getPlane(), target.getPlane());

        WalkerConfig walkerConfig = WalkerConfig.builder()
                .timeoutMillis(WALK_TIMEOUT_MS)
                .build();
        routes.planAndLog(start, target, walkerConfig);

        WalkResult result = walker.walkTo(target, walkerConfig);

        log.info("Walk finished: {}", result);

        WorldPoint end = ctx.players().local().location();
        log.info("Ended at {}, {} tiles from the destination (2D)", end,
                end != null ? end.distanceTo2D(target) : -1);

        return assertThat(result.isSuccess(), "the walk reached its destination: " + result.getReason());
    }

    /**
     * Picks the destination: the configured named place, then a tile the user set in game, then a
     * wait for a click, then the declared nearby tile.
     *
     * <p>A named config dest wins over a leftover shift-click, otherwise switching the dropdown back
     * to Karamja would keep walking to the last Set tile. {@link TargetTileProvider#get()} would also
     * silently use the suite-published declared tile instead of a shift-click, which is the opposite
     * of what someone setting a tile in Manual mode expects.</p>
     *
     * @return the destination, or null when none was chosen within the timeout
     */
    private WorldPoint resolveDestination() throws InterruptedException {
        WalkerDestination configured = config.walkerDestination();
        if (configured == null) {
            configured = WalkerDestination.MANUAL;
        }

        WorldPoint fromConfig = configured.resolve(targetTileProvider.getManualTile());
        if (fromConfig != null) {
            if (configured.getTile() != null) {
                log.info("Using the configured destination: {} {}",
                        configured.getDisplayName(), fromConfig);
            } else {
                log.info("Using the tile you picked: {}", fromConfig);
            }
            return fromConfig;
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
