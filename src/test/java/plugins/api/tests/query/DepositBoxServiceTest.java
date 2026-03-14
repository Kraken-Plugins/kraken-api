package plugins.api.tests.query;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.bank.DepositBoxService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import plugins.api.tests.BaseApiTest;

@Slf4j
@Singleton
public class DepositBoxServiceTest extends BaseApiTest {

    @Inject
    private DepositBoxService depositBoxService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        if (depositBoxService.isOpen()) {
            log.error("Expected deposit box to be closed for DepositBoxServiceTest. Start test with the deposit box closed.");
            return false;
        }

        log.info("Opening the deposit box.");
        ctx.gameObjects().nameContains("Bank deposit box").nearest().interact("Deposit");
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
    protected String getTestName() {
        return "Deposit Box Service";
    }
}
