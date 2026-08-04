package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.service.ui.processing.ProcessingService;
import com.kraken.api.service.util.SleepService;
import plugins.api.tests.BaseApiTest;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.SkillRequirement;
import plugins.api.requirements.TestRequirements;
import plugins.api.world.Facility;
import net.runelite.api.Skill;
import lombok.extern.slf4j.Slf4j;

/**
 * Exercises {@link ProcessingService} against the "make-X" (skill multi) interface.
 *
 * <p>This used to cook fish on the Barbarian Village fire, which pinned it to a location roughly 150
 * tiles from the rest of the suite and required raw salmon, raw trout and 25 Cooking.
 * {@link ProcessingService} is skill agnostic — it reads whatever the skill multi interface is
 * offering and clicks an option, with the action supplied as a plain string — so cutting a gem
 * exercises exactly the same code path while standing at the bank. It also reuses the chisel and
 * uncut sapphire that the widget interaction tests already require, so it adds no new bank stock.</p>
 *
 * <p>The old version asserted almost nothing: it interacted and returned true. This one asserts the
 * interface actually opened, that the quantity varc round trips, and that a cut gem really appeared
 * in the inventory.</p>
 *
 * <p><b>Requires:</b> a chisel and at least one uncut sapphire in the inventory, 20 Crafting, and a
 * free inventory slot. Consumes one uncut sapphire.</p>
 */
@Slf4j
@Singleton
public class ProcessingServiceTest extends BaseApiTest {

    private static final int UNCUT_SAPPHIRE = 1623;
    private static final int SAPPHIRE = 1607;

    private static final String CHISEL = "Chisel";

    /**
     * The skill multi interface labels its options with the skill's verb. Gem cutting uses "Craft",
     * but the option has been spelled "Make" in some interface revisions, so both are attempted and
     * the one that worked is logged.
     */
    private static final String[] CRAFT_ACTIONS = {"Craft", "Make"};

    private static final int INTERFACE_TIMEOUT_MS = 5000;
    private static final int CRAFT_TIMEOUT_MS = 6000;

    @Inject
    private ProcessingService processingService;

    @Override
    public TestRequirements requirements() {
        // Needs a bank only so the runner can hand it a chisel and a gem; the crafting itself happens
        // wherever the player is standing.
        return TestRequirements.builder()
                .facility(Facility.BANK_BOOTH)
                .inventoryItem(ItemRequirement.of(CHISEL))
                .inventoryItem(ItemRequirement.of(UNCUT_SAPPHIRE, 1))
                .skill(SkillRequirement.of(Skill.CRAFTING, 20))
                .sideEffect(SideEffect.CONSUMES_ITEMS)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        InventoryEntity chisel = ctx.inventory().withName(CHISEL).first();
        if (!assertTrue(chisel != null && chisel.isPresent(),
                "Processing service test: no chisel in the inventory")) {
            return false;
        }

        InventoryEntity uncut = ctx.inventory().withId(UNCUT_SAPPHIRE).first();
        if (!assertTrue(uncut != null && uncut.isPresent(),
                "Processing service test: no uncut sapphire in the inventory")) {
            return false;
        }

        final long cutBefore = ctx.inventory().withId(SAPPHIRE).count();

        // Using the chisel on the gem is what raises the skill multi interface.
        if (!assertTrue(chisel.useOn(uncut.raw()),
                "Processing service test: using the chisel on the uncut sapphire was rejected")) {
            return false;
        }

        if (!assertTrue(SleepService.sleepUntilTrue(processingService::isOpen, INTERFACE_TIMEOUT_MS),
                "Processing service test: the make-X interface never opened. Either the item on item "
                        + "interaction failed or the skill multi interface id has changed")) {
            return false;
        }

        boolean testsPassed = checkQuantityRoundTrip();
        testsPassed &= craftOneSapphire(ctx, cutBefore);
        return testsPassed;
    }

    /**
     * Verifies the make-X quantity varc can be written and read back.
     *
     * @return true when the quantity set is the quantity reported
     */
    private boolean checkQuantityRoundTrip() {
        processingService.setAmount(1);

        int amount = processingService.getAmount();
        return assertEquals(1, amount,
                "Processing service test: set the make-X quantity to 1 but read back " + amount
                        + ". The skill multi quantity varc mapping may have changed");
    }

    /**
     * Confirms selecting the cut gem in the interface actually produces one.
     *
     * @param ctx the injected API context
     * @param cutBefore how many cut sapphires were carried before crafting
     * @return true when the inventory gains a cut sapphire
     */
    private boolean craftOneSapphire(Context ctx, long cutBefore) {
        String usedAction = null;

        for (String action : CRAFT_ACTIONS) {
            if (!processingService.isOpen()) {
                break;
            }

            if (processingService.process(action, SAPPHIRE)) {
                usedAction = action;
                if (SleepService.sleepUntilTrue(
                        () -> ctx.inventory().withId(SAPPHIRE).count() > cutBefore, CRAFT_TIMEOUT_MS)) {
                    log.info("Processing service test: crafted a sapphire using the '{}' action", action);
                    return true;
                }
            }
        }

        if (usedAction == null) {
            return assertThat(false, "Processing service test: the make-X interface is open but does not "
                    + "offer a cut sapphire (item " + SAPPHIRE + ") under any of the expected actions");
        }

        return assertThat(false, "Processing service test: selected the cut sapphire with '" + usedAction
                + "' but no sapphire appeared in the inventory. Check the Crafting level is at least 20");
    }

    @Override
    protected String getTestName() {
        return "Processing Service";
    }
}
