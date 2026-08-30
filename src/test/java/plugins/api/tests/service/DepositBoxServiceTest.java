package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.bank.DepositBoxService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import plugins.api.requirements.DepositBoxState;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

@Slf4j
@Singleton
public class DepositBoxServiceTest extends BaseApiTest {

    @Inject
    private DepositBoxService depositBoxService;

    @Override
    public TestRequirements requirements() {
        // The canonical staging case: a deposit box has no bank attached, so items have to be collected
        // at the hub before travelling here. Without a staging location this test cannot be automated at
        // all, because there is nowhere at the destination to withdraw from.
        return TestRequirements.builder()
                .facility(Facility.DEPOSIT_BOX)
                .stagingLocation(NamedLocation.VARROCK_EAST_BANK)
                .depositBoxState(DepositBoxState.CLOSED)
                .sideEffect(SideEffect.EMPTIES_INVENTORY)
                .sideEffect(SideEffect.STRIPS_EQUIPMENT)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        if (depositBoxService.isOpen()) {
            log.error("Expected deposit box to be closed for DepositBoxServiceTest. Start test with the deposit box closed.");
            return false;
        }

        log.info("Opening the deposit box.");
        ctx.gameObjects().nameContains("Bank deposit box").sortByDistance().interact("Deposit");
        SleepService.sleepWhile(depositBoxService::isClosed, 5000);

        if (depositBoxService.isClosed()) {
            log.error("Failed to open deposit box.");
            return false;
        }

        log.info("Depositing all items");
        if (!depositBoxService.depositAll()) {
            log.error("Failed to deposit all inventory items.");
            return false;
        }

        SleepService.sleepFor(3);
        log.info("Depositing all equipment");
        if (!depositBoxService.depositWornItems()) {
            log.error("Failed to deposit all worn equipment");
            return false;
        }

        SleepService.sleepFor(3);
        log.info("Closing the deposit box.");
        if (!depositBoxService.close()) {
            log.error("Failed to close deposit box.");
            return false;
        }

        return true;
    }

    @Override
    public String getTestName() {
        return "Deposit Box Service";
    }
}
