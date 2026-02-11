package example.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import com.kraken.api.service.util.RandomService;
import com.kraken.api.service.util.SleepService;
import example.ExamplePlugin;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

@Slf4j
@Singleton
public class PathfinderServiceTest extends BaseApiTest {

    private static final WorldPoint PLAYER_START = new WorldPoint(3253, 3421, 0);
    private static final WorldPoint VARROCK_SQUARE = new WorldPoint(3208, 3422, 0);
    private static final WorldPoint VARROCK_WALL_INVALID = new WorldPoint(3270, 3441, 0); // An unreachable tile should find closest point.
    private static final WorldPoint OUT_OF_SCENE_LUMBRIDGE = new WorldPoint(3253, 3251, 0); // A tile far outside the currently loaded region

    @Inject
    private LocalPathfinder pathfinder;

    @Inject
    private ExamplePlugin plugin;

    @Inject
    private MovementService movementService;

    @Inject
    private SleepService sleepService;


    @Override
    public boolean runTest(Context ctx) throws Exception {
        WorldPoint[] path = {
                new WorldPoint(3253, 3421, 0),
                new WorldPoint(3253, 3426, 0),
                new WorldPoint(3255, 3428, 0),
                new WorldPoint(3258, 3430, 0),
                new WorldPoint(3259, 3432, 0),
                new WorldPoint(3263, 3433, 0),
                new WorldPoint(3262, 3438, 0),
                new WorldPoint(3259, 3440, 0),
                new WorldPoint(3255, 3439, 0),
                new WorldPoint(3249, 3440, 0),
                new WorldPoint(3247, 3443, 0),
                new WorldPoint(3245, 3440, 0),
                new WorldPoint(3242, 3438, 0),
                new WorldPoint(3242, 3433, 0),
                new WorldPoint(3239, 3431, 0),
                new WorldPoint(3237, 3428, 0),
                new WorldPoint(3240, 3426, 0),
                new WorldPoint(3240, 3421, 0),
                new WorldPoint(3240, 3418, 0),
                new WorldPoint(3243, 3417, 0),
                new WorldPoint(3245, 3420, 0),
                new WorldPoint(3245, 3425, 0),
                new WorldPoint(3248, 3427, 0),
                new WorldPoint(3249, 3429, 0),
                new WorldPoint(3252, 3428, 0)
        };

        for(int i = 0; i < 5; i++) {
            List<WorldPoint> randomizedPath = pathfinder.randomizeSparsePath(ctx.players().local().raw().getWorldLocation(), List.of(path), 2, 3, true);
            plugin.getCurrentPath().clear();
            plugin.getCurrentPath().addAll(randomizedPath);
            Thread.sleep(RandomService.between(4000, 5000));
        }

        plugin.getCurrentPath().clear();

        return true;
    }

    @Override
    protected String getTestName() {
        return "Pathfinder";
    }
}
