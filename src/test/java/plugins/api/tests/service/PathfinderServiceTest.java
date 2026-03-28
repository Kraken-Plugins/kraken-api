package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.ApiTestPlugin;
import plugins.api.tests.BaseApiTest;

import java.util.Collections;
import java.util.List;

@Slf4j
@Singleton
public class PathfinderServiceTest extends BaseApiTest {

    @Inject
    private LocalPathfinder pathfinder;

    @Inject
    private ApiTestPlugin plugin;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        WorldPoint playerLocation = ctx.players().local().location();
        if (playerLocation == null) {
            log.error("Pathfinder test failed: local player world location is unavailable.");
            updateOverlayPath(ctx, Collections.emptyList());
            return false;
        }

        WorldPoint target = resolveTarget();
        if (target == null) {
            log.error("Pathfinder test failed: no valid target configured. Set Pathfinder Test Target or select a tile.");
            updateOverlayPath(ctx, Collections.emptyList());
            return false;
        }

        if (playerLocation.equals(target)) {
            log.info("Pathfinder test target matches the player's current tile.");
        } else {
            log.info("Pathfinder test target: {}", target);
        }

        if (target.getPlane() != playerLocation.getPlane()) {
            log.error("Pathfinder test failed: target plane {} does not match player plane {}.", target.getPlane(), playerLocation.getPlane());
            updateOverlayPath(ctx, Collections.emptyList());
            return false;
        }

        List<WorldPoint> path = pathfinder.findPath(playerLocation, target);
        if (path == null || path.isEmpty()) {
            log.error("Pathfinder test failed: no path found from {} to {}. Ensure the target is inside the loaded scene.", playerLocation, target);
            updateOverlayPath(ctx, Collections.emptyList());
            return false;
        }

        updateOverlayPath(ctx, path);

        boolean valid = true;
        valid &= assertEquals(playerLocation, path.get(0), "Path should start at the player's location.");
        valid &= assertEquals(target, path.get(path.size() - 1), "Path should end at the target location.");

        if (valid) {
            log.info("Pathfinder test completed with {} path tiles.", path.size());
        }

        return valid;
    }

    private WorldPoint resolveTarget() {
        String raw = config.pathfinderTestTarget();
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
            log.error("Invalid pathfinder test target format: {}. Expected x,y,z.", raw);
            return null;
        }

        try {
            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());
            int z = Integer.parseInt(coords[2].trim());
            return new WorldPoint(x, y, z);
        } catch (NumberFormatException ex) {
            log.error("Invalid pathfinder test target values: {}", raw, ex);
            return null;
        }
    }

    private void updateOverlayPath(Context ctx, List<WorldPoint> path) {
        ctx.runOnClientThread(() -> {
            plugin.getPathfinderTestPath().clear();
            if (path != null && !path.isEmpty()) {
                plugin.getPathfinderTestPath().addAll(path);
            }
            return null;
        });
    }

    @Override
    protected String getTestName() {
        return "Pathfinder Test";
    }
}
