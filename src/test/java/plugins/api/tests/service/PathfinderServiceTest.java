package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import com.kraken.api.service.util.RandomService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.ApiTestPlugin;
import plugins.api.tests.BaseApiTest;

import java.util.List;

@Slf4j
@Singleton
public class PathfinderServiceTest extends BaseApiTest {

    private static final WorldPoint PLAYER_START = new WorldPoint(3253, 3421, 0);
    private static final WorldPoint VARROCK_SQUARE = new WorldPoint(3208, 3422, 0);
    private static final WorldPoint OUT_OF_SCENE_LUMBRIDGE = new WorldPoint(3253, 3251, 0); // A tile far outside the currently loaded region

    @Inject
    private LocalPathfinder pathfinder;

    @Inject
    private ApiTestPlugin plugin;

    @Override
    public boolean runTest(Context ctx) throws Exception {
        WorldPoint playerLocation = ctx.players().local().raw().getWorldLocation();
        if (playerLocation == null) {
            log.error("Unable to run global pathfinding test: local player world location is unavailable.");
            return false;
        }

        WorldPoint pluginTarget = plugin.getTargetTile();
        WorldPoint preferredTarget;

        if (pluginTarget != null) {
            preferredTarget = new WorldPoint(pluginTarget.getX(), pluginTarget.getY(), playerLocation.getPlane());
            log.info("Using target tile: {}", preferredTarget);
        } else if (playerLocation.distanceTo2D(PLAYER_START) <= 200) {
            log.info("Using out of scene lumb: {}", playerLocation);
            preferredTarget = OUT_OF_SCENE_LUMBRIDGE;
        } else {
            log.info("Using varrock square");
            preferredTarget = VARROCK_SQUARE;
        }

        log.info("Using player location: {}", playerLocation);
        WorldPoint normalizedTarget = new WorldPoint(preferredTarget.getX(), preferredTarget.getY(), playerLocation.getPlane());
        List<WorldPoint> denseGlobalPath = pathfinder.findPathWithBackoff(playerLocation, normalizedTarget);
        if (denseGlobalPath.isEmpty()) {
            log.info("No dense global path found.");
            WorldPoint localFallbackTarget = new WorldPoint(playerLocation.getX() + 100, playerLocation.getY() - 100, playerLocation.getPlane());
            denseGlobalPath = pathfinder.findPathWithBackoff(playerLocation, localFallbackTarget);
        }

        if (!assertTrue(!denseGlobalPath.isEmpty(), "Global path should be discovered from current player location to the selected destination.")) {
            return false;
        }

        SleepService.sleep(RandomService.between(5000, 7000));
        plugin.getCurrentPath().clear();
        return true;
    }

    @Override
    protected String getTestName() {
        return "Pathfinder";
    }
}
