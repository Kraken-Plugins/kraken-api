package plugins.api.tests.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import lombok.extern.slf4j.Slf4j;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

@Slf4j
@Singleton
public class WidgetTargetWidgetTest extends BaseApiTest {

    @Inject
    private Context context;

    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder().facility(Facility.BANK_BOOTH)
                .location(NamedLocation.VARROCK_EAST_BANK)
                .inventoryItem(ItemRequirement.of("Chisel", 1))
                .inventoryItem(ItemRequirement.of("Uncut Sapphire", 5))
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        InventoryEntity chisel = context.inventory().withName("Chisel").first().orElse(null);
        if(chisel == null) {
            log.error("Inventory test failed, could not find a chisel");
            return false;
        }

        InventoryEntity gem = context.inventory().withId(1623).first().orElse(null);
        if (gem == null) {
            log.error("Widget test failed, could not find an uncut sapphire");
            return false;
        }

        chisel.useOn(gem.raw());
        return true;
    }

    @Override
    public String getTestName() {
        return "Widget Target";
    }
}
