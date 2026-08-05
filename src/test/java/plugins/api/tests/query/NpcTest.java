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
                .location(NamedLocation.VARROCK_EAST_GUARDS)
                .nearbyNpc(NpcRequirement.named("Guard"))
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        try {
            // Note there is a guard by the gate that is not attackable (annoying I know)
            if (ctx.npcs().filter(n -> n.getId() != 1147).withName("Guard").isPresent()) {
                if (ctx.npcs().filter(n -> n.getId() != 1147).withName("Guard").attackable().isEmpty()) {
                    log.error("Found 'Guard' but attackable() filter excluded them.");
                    return false;
                }
            } else {
                log.warn("Skipping Guard attackable test (No Guards nearby)");
            }

            if (ctx.npcs().alive().isEmpty()) {
                log.error("Failed to find any 'alive' NPCs");
                return false;
            }

            WorldPoint playerLoc = ctx.players().local().location();
            WorldPoint min = new WorldPoint(playerLoc.getX() - 15, playerLoc.getY() - 15, playerLoc.getPlane());
            WorldPoint max = new WorldPoint(playerLoc.getX() + 15, playerLoc.getY() + 15, playerLoc.getPlane());

            if (ctx.npcs().withinArea(min, max).isEmpty()) {
                log.error("withinArea() returned no NPCs despite loose bounds (15 tiles)");
                return false;
            }

            if (ctx.npcs().reachable().isEmpty()) {
                log.error("No NPCs marked as reachable found");
                return false;
            }

            if(ctx.npcs().filter(n -> n.getId() != 1147).withName("Guard").nearest().isPresent()) {
                ctx.npcs().filter(n -> n.getId() != 1147).withName("Guard").nearest().attack();
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to run NPC test", e);
            return false;
        }

        return false;
    }

    @Override
    public String getTestName() {
        return "NPC";
    }
}