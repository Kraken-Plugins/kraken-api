package plugins.api.tests.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.player.PlayerEntity;
import lombok.extern.slf4j.Slf4j;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

@Slf4j
@Singleton
public class WidgetTargetPlayerTest extends BaseApiTest {

    @Inject
    private Context context;

    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder().facility(Facility.BANK_BOOTH)
                .location(NamedLocation.VARROCK_WEST_BANK)
                .inventoryItem(ItemRequirement.of("Chaotic handegg", 1))
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        InventoryEntity egg = context.inventory().withName("Chaotic handegg").first().orElse(null);
        if(egg == null) {
            log.error("No chaotic handegg in inventory");
            return false;
        }

        PlayerEntity randomPlayer = ctx.players().nearest().stream().findFirst().orElse(null);
        if(randomPlayer == null) {
            log.error("No random player found nearby");
            return false;
        }

        return egg.useOn(randomPlayer.raw());
    }

    @Override
    public String getTestName() {
        return "Widget Player";
    }
}
