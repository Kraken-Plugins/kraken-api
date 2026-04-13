package plugins.api.tests.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import lombok.extern.slf4j.Slf4j;
import plugins.api.tests.BaseApiTest;

@Slf4j
@Singleton
public class WidgetSubActionTest extends BaseApiTest {

    @Inject
    private Context context;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        InventoryEntity ringOfDueling = context.inventory().nameContains("Ring of Dueling").first();
        if(ringOfDueling == null) {
            log.error("Inventory test failed, could not find a Ring of dueling");
            return false;
        }

        context.getInteractionManager().interact(ringOfDueling.raw().getWidget(), "Rub", "Fortis Colosseum");
        return true;
    }

    @Override
    protected String getTestName() {
        return "Widget Sub";
    }
}
