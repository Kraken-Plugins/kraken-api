package plugins.api.tests.query;

import com.kraken.api.Context;
import plugins.api.tests.BaseApiTest;
import plugins.api.requirements.TestRequirements;
import plugins.api.world.Facility;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class GameObjectTest extends BaseApiTest {

    @Override
    public TestRequirements requirements() {
        // Read only: queries bank booths and touches nothing.
        return TestRequirements.builder()
                .facility(Facility.BANK_BOOTH)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean testsPassed = true;

        try {
            // Every check below is satisfied by any bank, so this runs wherever the suite is banking.
            // A trailing "chop the nearest Oak" call used to live at the end of this method; it
            // asserted nothing and, when no Oak was in the scene, threw into the catch below and
            // failed the whole test. Tree interaction is covered by the interaction tests instead.

            // 1. Basic Existence: Verify Bank booths exist in the query
            boolean boothExists = !ctx.gameObjects().withName("Bank booth").first().isNull();
            if (!boothExists) {
                log.error("Failed to find any 'Bank booth'");
                testsPassed = false;
            }

            // 2. Action Filtering: Verify "Bank" action works
            boolean hasBankAction = !ctx.gameObjects()
                    .withName("Bank booth")
                    .withAction("Bank")
                    .first()
                    .isNull();

            if (!hasBankAction) {
                log.error("Failed to find Bank booth with 'Bank' action");
                testsPassed = false;
            }

            // 3. Reachability: The nearest bank booth should be reachable
            // (Assuming player is standing in the bank)
            boolean isReachable = !ctx.gameObjects()
                    .withName("Bank booth")
                    .reachable()
                    .nearest()
                    .isNull();

            if (!isReachable) {
                log.error("Nearest Bank booth was not marked as reachable");
                testsPassed = false;
            }

            // 4. Distance: Verify objects within specific range
            // Finds objects within 3 tiles (should be at least the floor or nearby walls)
            boolean hasCloseObjects = ctx.gameObjects().within(3).count() > 0;
            if (!hasCloseObjects) {
                log.error("Failed to find any objects within 3 tiles");
                testsPassed = false;
            }

            // 5. Specific Location: Check a specific coordinate (Optional, but good for precision)
            // We get the location of the nearest booth, and query .at() that location to ensure it returns the booth.
            var nearestBooth = ctx.gameObjects().withName("Bank booth").nearest();
            if (!nearestBooth.isNull()) {
                WorldPoint boothLoc = nearestBooth.raw().getWorldLocation();
                boolean retrievedByLoc = !ctx.gameObjects().at(boothLoc).withName("Bank booth").first().isNull();
                if (!retrievedByLoc) {
                    log.error("Failed to retrieve Bank booth via .at() coordinate query");
                    testsPassed = false;
                }
            }

        } catch (Exception e) {
            log.error("Failed to run game object test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Game Object";
    }
}