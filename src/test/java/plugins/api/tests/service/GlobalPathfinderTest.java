package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.ApiTestConfig;
import plugins.api.ApiTestPlugin;
import plugins.api.tests.BaseApiTest;

@Slf4j
@Singleton
public class GlobalPathfinderTest extends BaseApiTest {

    @Inject
    private GlobalPathfinder globalPathfinder;

    @Inject
    private ApiTestPlugin plugin;

    @Inject
    private ApiTestConfig apiTestConfig;

    @Override
    protected boolean runTest(Context ctx) {
        WorldPoint playerLocation = ctx.players().local().location();
        if (playerLocation == null) {
            log.error("Global pathfinder test failed: local player world location is unavailable.");
            globalPathfinder.clearLastResult();
            return false;
        }

        WorldPoint target = resolveTarget();
        if (target == null) {
            log.error("Global pathfinder test failed: no valid target configured. Set Global Pathfinder Target or select a tile.");
            globalPathfinder.clearLastResult();
            return false;
        }

        GlobalPathfinder.PathResult result = globalPathfinder.findPathResult(
                playerLocation,
                target,
                GlobalPathfinderConfig.builder().build()
        );

        if (!result.isComplete() || result.getPath().isEmpty()) {
            log.error("Global pathfinder test failed: no complete path found from {} to {}.", playerLocation, target);
            return false;
        }

        boolean valid = true;
        valid &= assertEquals(playerLocation, result.getPath().get(0), "Global path should start at the player's location.");
        valid &= assertEquals(target, result.getPath().get(result.getPath().size() - 1), "Global path should end at the configured target.");
        valid &= assertTrue(!result.getSparsePath().isEmpty(), "Sparse global path should not be empty.");
        valid &= assertEquals(target, result.getSparsePath().get(result.getSparsePath().size() - 1), "Sparse global path should end at the configured target.");

        if (result.getPath().size() > 15) {
            valid &= assertTrue(
                    result.getSparsePath().size() <= result.getPath().size(),
                    "Sparse global path should not contain more waypoints than the dense path."
            );
        }

        if (valid) {
            log.info(
                    "Global pathfinder test completed with {} dense tiles, {} sparse waypoints, and {} transports.",
                    result.getPath().size(),
                    result.getSparsePath().size(),
                    result.getTransports().size()
            );
        }

        return valid;
    }

    @Override
    protected void onFinish() {
        if (!apiTestConfig.showGlobalPathfinderOverlay()) {
            globalPathfinder.clearLastResult();
        }
    }

    private WorldPoint resolveTarget() {
        String raw = apiTestConfig.globalPathfinderTarget();
        if (raw != null && !raw.trim().isEmpty()) {
            return parseTarget(raw.trim());
        }

        WorldPoint selected = plugin.getTargetTile();
        if (selected == null) {
            return null;
        }

        return new WorldPoint(selected.getX(), selected.getY(), selected.getPlane());
    }

    private WorldPoint parseTarget(String raw) {
        String[] coords = raw.split(",");
        if (coords.length != 3) {
            log.error("Invalid global pathfinder target format: {}. Expected x,y,z.", raw);
            return null;
        }

        try {
            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());
            int z = Integer.parseInt(coords[2].trim());
            return new WorldPoint(x, y, z);
        } catch (NumberFormatException ex) {
            log.error("Invalid global pathfinder target values: {}", raw, ex);
            return null;
        }
    }

    @Override
    protected String getTestName() {
        return "Global Pathfinder Test";
    }
}
