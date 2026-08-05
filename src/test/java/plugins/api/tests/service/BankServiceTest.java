package plugins.api.tests.service;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.util.SleepService;
import plugins.api.requirements.BankState;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BankServiceTest extends BaseApiTest {

    @Inject
    private BankService bankService;

    @Override
    public TestRequirements requirements() {
        // Asserts it can open the bank itself, so it must start closed. This is the direct opposite of
        // SpellServiceTest, which needs the bank already open — running them in sequence used to fail
        // whichever went second.
        return TestRequirements.builder()
                .facility(Facility.BANK_BOOTH)
                .bankState(BankState.CLOSED)
                .sideEffect(SideEffect.EMPTIES_INVENTORY)
                .sideEffect(SideEffect.STRIPS_EQUIPMENT)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean open = bankService.isOpen();

        if(open) {
            log.error("Expected bank to be closed for BankServiceTest. Start test with the bank closed.");
            return false;
        }

        log.info("Opening the bank.");
        ctx.gameObjects().nameContains("Bank booth").nearest().interact("Bank");
        SleepService.sleepWhile(() -> bankService.isClosed(), 5000);

        log.info("Depositing all items");
        if(!bankService.depositAll()) {
            log.error("Failed to deposit all inventory items.");
            return false;
        }
        SleepService.sleepFor(3);
        log.info("Depositing all equipment");
        if(!bankService.depositAllEquipment()) {
            log.error("Failed to deposit all worn equipment");
            return false;
        }

        SleepService.sleepFor(3);
        log.info("Depositing all containers");
        if(!bankService.depositContainers()) {
            log.error("Failed to deposit containers (loot bag)");
            return false;
        }

        SleepService.sleepFor(5);
        log.info("Setting withdraw mode to: NOTED");
        if(!bankService.setWithdrawMode(true)) {
            log.error("Failed to set withdraw mode to: NOTED");
            return false;
        }

        SleepService.sleepFor(5);
        log.info("Setting withdraw mode to: ITEM");
        if(!bankService.setWithdrawMode(false)) {
            log.error("Failed to set withdraw mode to: ITEM");
            return false;
        }

        if(!bankService.close()) {
            log.error("Failed to close bank.");
            return false;
        }

        return true;
    }

    @Override
    public String getTestName() {
        return "Bank Service";
    }
}
