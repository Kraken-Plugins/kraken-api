package plugins.api.tests.query;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.util.RandomUtils;
import plugins.api.requirements.BankState;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.InventoryPolicy;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InventoryTest extends BaseApiTest {

    @Inject
    private BankService bankService;

    @Override
    public TestRequirements requirements() {
        // Two constraints here are easy to miss from reading the assertions alone.
        //
        // The slot checks require an empty inventory before the withdrawals: swordfish must land in
        // slot 0 and lobster in slot 5, which only holds if nothing else is carried. EXACT is declared
        // explicitly rather than left to AUTO so that stays true even if the item lists change.
        //
        // The hasItem assertions are negative — they prove the query does not report an item that is
        // absent — so a gold bar or sapphire left over from another test would fail them.
        return TestRequirements.builder()
                .facility(Facility.BANK_BOOTH)
                .bankState(BankState.OPEN)
                .inventoryPolicy(InventoryPolicy.EXACT)
                .bankStock(ItemRequirement.of("Swordfish", 5))
                .bankStock(ItemRequirement.of("Lobster", 5))
                .forbiddenItem(ItemRequirement.of("Gold bar"))
                .forbiddenItem(ItemRequirement.of(1607, 1))
                .sideEffect(SideEffect.DROPS_ITEMS)
                .sideEffect(SideEffect.CONSUMES_ITEMS)
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

            bankService.setWithdrawMode(false);

            if(ctx.bank().withName("Swordfish").isPresent()) {
                ctx.bank().withName("Swordfish").first().ifPresent(e -> e.withdraw(5));
            } else {
                log.error("Failed to find Swordfish in the bank");
            }
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));

            if(ctx.bank().withName("Lobster").isPresent()) {
                ctx.bank().withName("Lobster").first().ifPresent(e -> e.withdraw(5));
            } else {
                log.error("No lobster present in the bank");
            }
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));

            assertTrue(ctx.inventory().inSlot(0).first().map(e -> e.getName().equals("Swordfish")).orElse(false), "Swordfish is in slot 0");
            assertTrue(ctx.inventory().inSlot(5).first().map(e -> e.getName().equals("Lobster")).orElse(false), "Lobster is in slot 5");
            assertNull(ctx.inventory().inSlot(27).first().orElse(null), "Nothing in slot 27");

            testsPassed &= ctx.inventory().food().count() > 0;
            testsPassed &= !ctx.inventory().isEmpty();
            testsPassed &= ctx.inventory().nameContains("Sword").count() > 0;
            testsPassed &= ctx.inventory().filter(entity -> entity.getName().equalsIgnoreCase("Swordfish")).interact("Drop");
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            testsPassed &= ctx.inventory().food().nameContains("Lobster").interact("Eat");
            SleepService.tick();
            ctx.groundItems().filter(entity -> entity.getName().equalsIgnoreCase("Swordfish")).first().ifPresent(item -> item.take());

            testsPassed &= !ctx.inventory().hasItem("Gold bar");
            testsPassed &= !ctx.inventory().hasItem(1607); // sapphire
            testsPassed &= ctx.inventory().hasItem("Swordfish");
            testsPassed &= ctx.inventory().hasItem(379);

        } catch (Exception e) {
            log.error("Exception during inventory query test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    public String getTestName() {
        return "Inventory";
    }
}

