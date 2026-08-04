package plugins.api.tests.query;

import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.requirements.NpcRequirement;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

@Slf4j
public class NpcTest extends BaseApiTest {

    @Override
    public TestRequirements requirements() {
        // Reads the scene and attacks a guard; needs no items and changes nothing that outlives it.
        return TestRequirements.builder()
                .facility(Facility.COMBAT_NPCS_F2P)
                .location(NamedLocation.VARROCK_EAST_BANK)
                .nearbyNpc(NpcRequirement.named("Guard"))
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean testsPassed = true;

        try {
            // 2. Attackable Filter: Verify logic on Guards
            // Guards should be attackable
            boolean guardsFound = ctx.npcs().withName("Guard").isPresent();
            if (guardsFound) {
                boolean guardIsAttackable = ctx.npcs().withName("Guard").attackable().isPresent();
                if (!guardIsAttackable) {
                    log.error("Found 'Guard' but attackable() filter excluded them.");
                    testsPassed = false;
                }
            } else {
                log.warn("Skipping Guard attackable test (No Guards nearby)");
            }

            boolean aliveCheck = !ctx.npcs().alive().first().isNull();
            if (!aliveCheck) {
                log.error("Failed to find any 'alive' NPCs");
                testsPassed = false;
            }

            // Area Query (.withinArea)
            // Define a box around the player and ensure we find NPCs inside it
            WorldPoint playerLoc = ctx.players().local().location();
            WorldPoint min = new WorldPoint(playerLoc.getX() - 15, playerLoc.getY() - 15, playerLoc.getPlane());
            WorldPoint max = new WorldPoint(playerLoc.getX() + 15, playerLoc.getY() + 15, playerLoc.getPlane());

            boolean npcsInArea = !ctx.npcs().withinArea(min, max).first().isNull();
            if (!npcsInArea) {
                log.error("withinArea() returned no NPCs despite loose bounds (15 tiles)");
                testsPassed = false;
            }

            // Reachability
            // Ensure at least some NPCs are reachable (e.g., other players' pets, guards, or men)
            // Note: Bankers behind booths might return false for reachability depending on exact tile logic
            boolean anyReachable = !ctx.npcs().reachable().first().isNull();
            if (!anyReachable) {
                log.error("No NPCs marked as reachable found");
                testsPassed = false;
            }

            // Interaction Chain (Optional Smoke Test)
            // Only run if we found a guard, try to hover or check interaction logic (without clicking)
            if (guardsFound) {
                var guard = ctx.npcs().withName("Guard").nearest();
                if (guard.isNull()) {
                    log.error("Guard found previously but failed to retrieve in Interaction test");
                    testsPassed = false;
                }
                guard.attack();
            }

        } catch (Exception e) {
            log.error("Failed to run NPC test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    public String getTestName() {
        return "NPC";
    }
}