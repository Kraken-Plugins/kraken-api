package plugins.api.tests.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import lombok.extern.slf4j.Slf4j;
import plugins.api.tests.BaseApiTest;

@Slf4j
@Singleton
public class WidgetTargetWidgetTest extends BaseApiTest {

    @Inject
    private Context context;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        InventoryEntity chisel = context.inventory().withName("Chisel").first();
        if(chisel == null) {
            log.error("Inventory test failed, could not find a chisel");
            return false;
        }

        InventoryEntity gem = context.inventory().withId(1623).first();
        if (gem == null) {
            log.error("Widget test failed, could not find an uncut sapphire");
            return false;
        }

        chisel.useOn(gem.raw());
        return true;
    }

    @Override
    protected String getTestName() {
        return "Widget Target";
    }
}
