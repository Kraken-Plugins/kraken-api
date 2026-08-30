package plugins.api.tests.query;


import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.container.bank.BankEntity;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.util.RandomUtils;
import plugins.api.requirements.BankState;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BankTest extends BaseApiTest {

    @Inject
    BankService bankService;

    @Override
    public TestRequirements requirements() {
        // Everything here is bank stock rather than an inventory requirement: the test deposits the
        // whole inventory first and then withdraws for itself, so the runner only has to guarantee the
        // items exist in the bank before the player is stripped.
        return TestRequirements.builder()
                .facility(Facility.BANK_BOOTH)
                .bankState(BankState.OPEN)
                .bankStock(ItemRequirement.of("Rune platebody"))
                .bankStock(ItemRequirement.of("Rune platelegs"))
                .bankStock(ItemRequirement.of("Rune full helm"))
                .bankStock(ItemRequirement.of(373, 4))
                .bankStock(ItemRequirement.of("Lobster", 13))
                .sideEffect(SideEffect.EMPTIES_INVENTORY)
                .sideEffect(SideEffect.STRIPS_EQUIPMENT)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) {
        boolean testsPassed = true;

        try {
            if (!bankService.isOpen()) {
                GameObjectEntity bank = ctx.gameObjects()
                        .withName("Bank booth")
                        .withAction("Bank")
                        .nearest()
                        .orElse(null);

                if (bank == null) {
                    log.error("Failed to find Bank booth with 'Bank' action");
                    return false;
                }

                bank.interact("Bank");
                SleepService.sleepFor(2);
            }

            // Test for substring contains with platelegs, platebody, plateskirt etc...
            long plateCount = ctx.bank().nameContains("plate").stream().count();
            testsPassed &= plateCount > 0;
            testsPassed &= bankService.depositAll();
            SleepService.sleepFor(2);
            testsPassed &= bankService.depositAllEquipment();
            SleepService.sleepFor(2);

            testsPassed &= ctx.bank().withName("Rune Platebody").isPresent();
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            testsPassed &= ctx.bank().withName("Rune Platelegs").first().map(e -> e.withdraw(1, true)).orElse(false);
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            testsPassed &= ctx.bank().withName("Rune full helm").first().map(e -> e.withdraw(1, false)).orElse(false);
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            testsPassed &= ctx.bank().withId(373).first().map(e -> e.withdraw(4, false)).orElse(false); // swordfish

            SleepService.sleepFor(3);
            BankEntity lobster = ctx.bank().withName("Lobster").first().orElse(null);
            lobster.withdraw(6, false);
            SleepService.sleepFor(3);
            lobster.withdraw(7, true);

            testsPassed &= bankService.close();
        } catch (Exception e) {
            log.error("Exception during bank query test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    public String getTestName() {
        return "Bank";
    }
}
