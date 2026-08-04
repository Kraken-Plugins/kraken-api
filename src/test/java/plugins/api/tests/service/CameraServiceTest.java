package plugins.api.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.camera.CameraService;
import com.kraken.api.service.util.RandomService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import plugins.api.ApiTestPlugin;
import plugins.api.requirements.TargetTile;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;

@Slf4j
public class CameraServiceTest extends BaseApiTest {

    @Inject
    private Client client;

    @Inject
    private CameraService camera;

    @Inject
    private ApiTestPlugin examplePlugin;

    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder()
                .targetTile(TargetTile.relativeToPlayer(3, 3))
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        log.info("Starting Camera Service Test.");

        WorldPoint target = waitForTargetSelection();
        if (target == null) {
            log.error("Test timed out waiting for target selection.");
            return false;
        }

        LocalPoint targetLp = LocalPoint.fromWorld(client, target);

        if(targetLp == null) {
            log.info("Target tile could not be converted to local point");
            return false;
        }

        log.info("Target selected: {}. Running camera service tests...", target);

        int originalPitch = camera.getPitch();

        camera.setPitch(383); // Max up
        Thread.sleep(RandomService.between(2000, 3500));
        if (camera.getPitch() < 370) {
            log.error("Camera Pitch out of bounds. > 370");
            return false;
        }

        camera.setPitch(128); // Max down
        Thread.sleep(RandomService.between(2000, 3500));
        if (camera.getPitch() > 140) {
            log.error("Failed to set Pitch MIN (Down)");
            return false;
        }

        camera.setPitch(originalPitch);

        int originalZoom = camera.getZoom();

        camera.setZoom(800); // Zoom way out
        Thread.sleep(RandomService.between(2000, 3500));

        if (camera.getZoom() < 400) {
            log.error("Failed to Zoom OUT");
            return false;
        };

        camera.setZoom(100);
        Thread.sleep(RandomService.between(2000, 3500));
        if (camera.getZoom() > 200){
            log.error("Failed to Zoom IN");
            return false;
        }

        // Reset Zoom
        camera.setZoom(originalZoom);

        // Randomly offset camera first so we know we actually moved
        int startAngle = camera.angleToTile(target) + 100;
        camera.setAngle(startAngle, 10);
        camera.turnTo(targetLp);


        // Calculate expected angle
//        int expectedAngle = camera.angleToTile(targetLp);
        // Check if we are within the default 80 degree tolerance of turnTo
//        log.info("Expected angle: {}", expectedAngle);
//        if (!camera.isAngleGood(expectedAngle, 80)) {
//            return fail("Failed to Turn To target. Angle difference too high.");
//        }

        camera.setPitch(128);
        camera.setAngle(camera.getAngle() + 90, 10);
        Thread.sleep(RandomService.between(2000, 3500));

        camera.centerTileOnScreen(targetLp);

        if (!camera.isTileCenteredOnScreen(targetLp)) {
            camera.centerTileOnScreen(targetLp);
            if (!camera.isTileCenteredOnScreen(targetLp)) {
                log.error("Failed to center tile on screen.");
                return false;
            }
        }

        log.info("Camera Service Test Passed Successfully.");
        return true;
    }

    private WorldPoint waitForTargetSelection() throws InterruptedException {
        int timeout = 0;
        while (examplePlugin.getTargetTile() == null && timeout < 300) {
            Thread.sleep(100);
            timeout++;
        }
        return examplePlugin.getTargetTile();
    }

    @Override
    public String getTestName() {
        return "Camera Service";
    }
}
