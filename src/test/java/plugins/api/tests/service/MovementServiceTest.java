package plugins.api.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.ApiTestPlugin;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TargetTile;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;

@Slf4j
public class MovementServiceTest extends BaseApiTest {

    @Inject
    private Context ctx;

    @Inject
    private MovementService movementService;

    @Inject
    private ApiTestPlugin examplePlugin;

    @Override
    public TestRequirements requirements() {
        // Declaring a target tile is what makes this test runnable unattended. The body polls for a
        // tile the user picked by shift right clicking "Set" and gives up after thirty seconds; with a
        // tile published ahead of time that poll returns immediately, and the manual pathway still
        // works whenever no suite is running.
        return TestRequirements.builder()
                .targetTile(TargetTile.relativeToPlayer(6, 6))
                .sideEffect(SideEffect.MOVES_PLAYER)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        log.info("Test started. Please Shift + Right Click 'Walk here' -> 'Set' on a tile to begin movement.");

        WorldPoint target = waitForTargetSelection();
        if (target == null) {
            log.error("Test timed out waiting for target selection.");
            return false;
        }

        log.info("Target selected: {}. Attempting to move...", target);
        movementService.moveTo(target);
        return waitForArrival(target);
    }

    private WorldPoint waitForTargetSelection() throws InterruptedException {
        int timeout = 0;
        // Wait up to 30 seconds for user input
        while (examplePlugin.getTargetTile() == null && timeout < 300) {
            Thread.sleep(100);
            timeout++;
        }
        return examplePlugin.getTargetTile();
    }

    private boolean waitForArrival(WorldPoint target) throws InterruptedException {
        int timeoutTicks = 0;
        int maxTicks = 25; // Fail if not arrived in ~30 seconds (adjust based on distance)

        while (timeoutTicks < maxTicks) {
            WorldPoint playerLoc = ctx.players().local().location();
            if (playerLoc.distanceTo(target) < 3) {
                return true;
            }

            Thread.sleep(600);
            timeoutTicks++;
        }

        log.error("Test failed: Player did not reach destination within timeout.");
        return false;
    }

    @Override
    public String getTestName() {
        return "Movement Service";
    }
}
