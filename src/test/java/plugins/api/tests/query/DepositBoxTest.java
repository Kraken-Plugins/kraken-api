package plugins.api.tests.query;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.bank.DepositBoxService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.util.RandomUtils;
import net.runelite.api.EquipmentInventorySlot;
import plugins.api.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DepositBoxTest extends BaseApiTest {

    @Inject
    private DepositBoxService depositBoxService;

    @Override
    protected boolean runTest(Context ctx) {
        boolean testsPassed = true;

        try {
            if (depositBoxService.isClosed()) {
                log.error("Cannot execute deposit box query tests, deposit box is not open");
                return false;
            }

            // --- Query filter tests ---

            boolean notEmpty = !ctx.depositBox().isEmpty();
            testsPassed &= notEmpty;
            log.info("[{}] depositBox is not empty", notEmpty ? "PASS" : "FAIL");

            boolean countAboveZero = ctx.depositBox().count() > 0;
            testsPassed &= countAboveZero;
            log.info("[{}] depositBox count > 0", countAboveZero ? "PASS" : "FAIL");

            boolean allValidIds = ctx.depositBox().list().stream().allMatch(e -> e.getId() != -1);
            testsPassed &= allValidIds;
            log.info("[{}] all entities have valid IDs", allValidIds ? "PASS" : "FAIL");

            boolean allValidNames = ctx.depositBox().list().stream().allMatch(e -> e.getName() != null && !e.getName().isEmpty());
            testsPassed &= allValidNames;
            log.info("[{}] all entities have valid names", allValidNames ? "PASS" : "FAIL");

            long total = ctx.depositBox().count();
            long noted = ctx.depositBox().noted().count();
            long unnoted = ctx.depositBox().unnoted().count();
            boolean notedUnnotedSum = noted + unnoted == total;
            testsPassed &= notedUnnotedSum;
            log.info("[{}] noted({}) + unnoted({}) == total({})", notedUnnotedSum ? "PASS" : "FAIL", noted, unnoted, total);

            boolean stackableSubset = ctx.depositBox().stackable().count() <= total;
            testsPassed &= stackableSubset;
            log.info("[{}] stackable count <= total({})", stackableSubset ? "PASS" : "FAIL", total);

            boolean quantityGtZero = ctx.depositBox().quantityGreaterThan(0).count() == total;
            testsPassed &= quantityGtZero;
            log.info("[{}] quantityGreaterThan(0) == total({})", quantityGtZero ? "PASS" : "FAIL", total);

            boolean quantityGtMax = ctx.depositBox().quantityGreaterThan(Integer.MAX_VALUE - 1).count() == 0;
            testsPassed &= quantityGtMax;
            log.info("[{}] quantityGreaterThan(MAX) == 0", quantityGtMax ? "PASS" : "FAIL");

            var first = ctx.depositBox().first();
            boolean firstNotNull = first != null;
            testsPassed &= firstNotNull;
            log.info("[{}] first entity is not null", firstNotNull ? "PASS" : "FAIL");

            boolean withIdFindsFirst = ctx.depositBox().withId(first.getId()).first() != null;
            testsPassed &= withIdFindsFirst;
            log.info("[{}] withId({}) finds entity", withIdFindsFirst ? "PASS" : "FAIL", first.getId());

            boolean firstCountAboveZero = first.count() > 0;
            testsPassed &= firstCountAboveZero;
            log.info("[{}] first entity count({}) > 0", firstCountAboveZero ? "PASS" : "FAIL", first.count());

            // --- Entity deposit action tests ---

            var coins = ctx.depositBox().withId(995).first();
            if (coins != null) {
                boolean depositOne = coins.depositOne();
                testsPassed &= depositOne;
                log.info("[{}] depositOne on coins", depositOne ? "PASS" : "FAIL");
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            } else {
                log.warn("[SKIP] No coins found, skipping depositOne test");
            }

            var swordfish = ctx.depositBox().noted().withName("Swordfish").first();
            if (swordfish != null) {
                boolean hasEnough = swordfish.count() >= 5;
                testsPassed &= hasEnough;
                log.info("[{}] swordfish noted count({}) >= 5", hasEnough ? "PASS" : "FAIL", swordfish.count());

                boolean depositFive = swordfish.depositFive();
                testsPassed &= depositFive;
                log.info("[{}] depositFive on swordfish", depositFive ? "PASS" : "FAIL");
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            } else {
                log.warn("[SKIP] No swordfish found, skipping depositFive test");
            }

            var lobster = ctx.depositBox().noted().withName("Lobster").first();
            if (lobster != null) {
                boolean hasEnough = lobster.count() >= 10;
                testsPassed &= hasEnough;
                log.info("[{}] lobster noted count({}) >= 10", hasEnough ? "PASS" : "FAIL", lobster.count());

                boolean depositTen = lobster.depositTen();
                testsPassed &= depositTen;
                log.info("[{}] depositTen on lobster", depositTen ? "PASS" : "FAIL");
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            } else {
                log.warn("[SKIP] No lobster found, skipping depositTen test");
            }

            var fireRunes = ctx.depositBox().nameContains("Fire rune").first();
            if (fireRunes != null) {
                boolean hasEnough = fireRunes.count() >= 3;
                testsPassed &= hasEnough;
                log.info("[{}] fire rune count({}) >= 3", hasEnough ? "PASS" : "FAIL", fireRunes.count());

                boolean depositX = fireRunes.depositX(3);
                testsPassed &= depositX;
                log.info("[{}] depositX(3) on fire runes", depositX ? "PASS" : "FAIL");
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            } else {
                log.warn("[SKIP] No fire runes found, skipping depositX test");
            }

            var lawRunes = ctx.depositBox().withName("Law rune").first();
            if (lawRunes != null) {
                int lawRuneId = lawRunes.getId();
                boolean depositAll = lawRunes.depositAll();
                testsPassed &= depositAll;
                log.info("[{}] depositAll on law runes", depositAll ? "PASS" : "FAIL");
                SleepService.sleepFor(3);
                boolean goneAfterDeposit = ctx.depositBox().withId(lawRuneId).first() == null;
                testsPassed &= goneAfterDeposit;
                log.info("[{}] law runes no longer in inventory after depositAll", goneAfterDeposit ? "PASS" : "FAIL");
            } else {
                log.warn("[SKIP] No law runes found, skipping depositAll test");
            }

            var helm = ctx.depositBox().inEquipment().inSlot(EquipmentInventorySlot.HEAD);
            if(helm != null) {
                boolean success = helm.deposit();
                log.info("[{}] deposit() on helm", success ? "PASS" : "FAIL");
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            } else {
                log.warn("[SKIP] No helm found in equipment.");
            }

        } catch (Exception e) {
            log.error("Exception during deposit box query test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "DepositBoxQuery";
    }
}