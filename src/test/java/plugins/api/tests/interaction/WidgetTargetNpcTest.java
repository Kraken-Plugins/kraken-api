package plugins.api.tests.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.magic.spellbook.Standard;
import lombok.extern.slf4j.Slf4j;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.NpcRequirement;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

@Slf4j
@Singleton
public class WidgetTargetNpcTest extends BaseApiTest {

    @Inject
    private Context context;

    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder().facility(Facility.COMBAT_NPCS_F2P)
                .location(NamedLocation.VARROCK_EAST_GUARDS)
                .nearbyNpc(NpcRequirement.named("Guard"))
                .inventoryItem(ItemRequirement.of("Fire Rune", 50))
                .inventoryItem(ItemRequirement.of("Mind Rune", 50))
                .inventoryItem(ItemRequirement.of("Air Rune", 50))
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        // There is a "Guard" guarding the gate into south east varrock which is not attackable so exclude him
        NpcEntity guard = context.npcs().filter(n -> n.getId() != 1147).nameContains("Guard").nearest().orElse(null);
        if(guard == null) {
            log.error("Spell Service tests failed, could not find a guard");
            return false;
        }

        return context.getService(MagicService.class).castOn(Standard.FIRE_STRIKE, guard.raw());
    }

    @Override
    public String getTestName() {
        return "Widget NPC";
    }
}
