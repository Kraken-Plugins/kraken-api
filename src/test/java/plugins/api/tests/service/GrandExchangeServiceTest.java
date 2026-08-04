package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.bank.BankEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.grandexchange.GrandExchangeService;
import com.kraken.api.service.grandexchange.GrandExchangeSlot;
import com.kraken.api.service.util.SleepService;
import plugins.api.requirements.BankState;
import plugins.api.requirements.CustomRequirement;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.NamedLocation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class GrandExchangeServiceTest extends BaseApiTest {

    @Inject
    private Context ctx;

    @Inject
    private BankService bankService;

    @Inject
    private GrandExchangeService grandExchangeService;

    @Override
    public TestRequirements requirements() {
        // Marked destructive because it places live orders and deliberately sells at a loss. It is
        // excluded from bulk runs by default and must be opted into.
        //
        // The free slot is the archetypal custom check: the runner can see whether one exists but must
        // never establish one, since that would mean cancelling somebody's real offers.
        return TestRequirements.builder()
                .location(NamedLocation.GRAND_EXCHANGE)
                .bankState(BankState.CLOSED)
                .bankStock(ItemRequirement.of("Fire rune", 3))
                .customCheck(CustomRequirement.of("a free Grand Exchange slot",
                        context -> context.getService(GrandExchangeService.class).getFirstFreeSlot() != null))
                .destructive(true)
                .sideEffect(SideEffect.TRADES_ON_GE)
                .sideEffect(SideEffect.CONSUMES_ITEMS)
                .sideEffect(SideEffect.LEAVES_INTERFACE_OPEN)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {

        // Setup
        NpcEntity banker = ctx.npcs().withAction("Bank").nearest();
        if(banker == null) {
            log.error("Cannot open bank to withdraw items to sell");
            return false;
        }

        if(!banker.interact("Bank")) {
            log.error("Failed to interact with bank to withdraw items to sell");
            return false;
        }

        SleepService.sleepUntil(bankService::isOpen, 5000);

        if(!bankService.isOpen()) {
            log.error("Cannot open bank, perhaps the pin is blocking interface?");
            return false;
        }

        BankEntity fireRune = ctx.bank().withName("Fire rune").first();
        if(fireRune == null) {
            log.error("No fire runes in the bank to withdraw");
            return false;
        }

        fireRune.withdraw(3);
        bankService.close();

        SleepService.sleepWhile(bankService::isOpen, 5000);

        // Execute by selling fire runes
        NpcEntity clerk = ctx.npcs().withAction("Exchange").nearest();
        if (clerk == null) {
            log.error("Could not find Grand Exchange Clerk");
            return false;
        }

        if (!clerk.interact("Exchange")) {
            log.error("Failed to interact with Grand Exchange Clerk");
            return false;
        }

        if (!SleepService.sleepUntil(grandExchangeService::isOpen, 5000)) {
            log.error("Grand Exchange interface did not open");
            return false;
        }

        GrandExchangeSlot slot = grandExchangeService.getFirstFreeSlot();
        if(slot == null) {
            log.error("No free slots to run the test.");
            return false;
        }

        GrandExchangeSlot fireRuneSellSlot = grandExchangeService.queueSellOrder(fireRune.getId(), 1);

        if(fireRuneSellSlot == null) {
            log.error("Failed to sell fire runes on the grand exchange");
            return false;
        }

        SleepService.sleepUntil(fireRuneSellSlot::isFulfilled, 20000);

        grandExchangeService.collect(fireRuneSellSlot, false);
        SleepService.sleepFor(2);

        // Buy fire runes for 15 gp each (will insta buy)
        GrandExchangeSlot fireRuneBuySlot = grandExchangeService.queueBuyOrder(fireRune.getId(), 15, 15);
        if(fireRuneBuySlot == null) {
            log.error("failed to buy fire runes slot is null");
            return false;
        }

        SleepService.sleepUntil(fireRuneBuySlot::isFulfilled, 20000);
        return true;
    }

    @Override
    protected String getTestName() {
        return "Grand Exchange Service";
    }
}
