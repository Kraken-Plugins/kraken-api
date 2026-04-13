package plugins.api.tests.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.magic.spellbook.Standard;
import lombok.extern.slf4j.Slf4j;
import plugins.api.tests.BaseApiTest;

@Slf4j
@Singleton
public class WidgetTargetNpcTest extends BaseApiTest {

    @Inject
    private Context context;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        NpcEntity guard = context.npcs().nameContains("Guard").nearest();
        if(guard == null) {
            log.error("Spell Service tests failed, could not find a guard");
            return false;
        }

        return context.getService(MagicService.class).castOn(Standard.FIRE_STRIKE, guard.raw());
    }

    @Override
    protected String getTestName() {
        return "Widget NPC";
    }
}
