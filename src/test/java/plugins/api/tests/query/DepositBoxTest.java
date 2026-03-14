package plugins.api.tests.query;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.bank.DepositBoxService;
import com.kraken.api.util.RandomUtils;
import plugins.api.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

            testsPassed &= !ctx.depositBox().isEmpty();
            testsPassed &= ctx.depositBox().count() > 0;

            testsPassed &= ctx.depositBox().list().stream().allMatch(e -> e.getId() != -1);
            testsPassed &= ctx.depositBox().list().stream().allMatch(e -> e.getName() != null && !e.getName().isEmpty());

            long total = ctx.depositBox().count();
            long noted = ctx.depositBox().noted().count();
            long unnoted = ctx.depositBox().unnoted().count();
            testsPassed &= noted + unnoted == total;

            testsPassed &= ctx.depositBox().stackable().count() <= total;
            testsPassed &= ctx.depositBox().quantityGreaterThan(0).count() == total;
            testsPassed &= ctx.depositBox().quantityGreaterThan(Integer.MAX_VALUE - 1).count() == 0;

            var first = ctx.depositBox().first();
            testsPassed &= first != null;
            testsPassed &= ctx.depositBox().withId(first.getId()).first() != null;
            testsPassed &= first.count() > 0;

            // --- Entity deposit action tests ---

            // depositOne - use Coins since they are stackable and won't be fully consumed by one deposit
            var coins = ctx.depositBox().withId(995).first();
            if (coins != null) {
                testsPassed &= coins.depositOne();
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            } else {
                log.warn("No coins found, skipping depositOne test");
            }

            // depositFive
            var swordfish = ctx.depositBox().withName("Swordfish").first();
            if (swordfish != null) {
                testsPassed &= swordfish.count() >= 5;
                testsPassed &= swordfish.depositFive();
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            } else {
                log.warn("No swordfish found, skipping depositFive test");
            }

            // depositTen
            var lobster = ctx.depositBox().withName("Lobster").first();
            if (lobster != null) {
                testsPassed &= lobster.count() >= 10;
                testsPassed &= lobster.depositTen();
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            } else {
                log.warn("No lobster found, skipping depositTen test");
            }

            // depositX - deposit 3 of an item
            var fireRunes = ctx.depositBox().withName("Fire rune").first();
            if (fireRunes != null) {
                testsPassed &= fireRunes.count() >= 3;
                testsPassed &= fireRunes.depositX(3);
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            } else {
                log.warn("No fire runes found, skipping depositX test");
            }

            // depositAll - deposit all of one item and verify it's gone
            var lawRunes = ctx.depositBox().withName("Law rune").first();
            if (lawRunes != null) {
                int lawRuneId = lawRunes.getId();
                testsPassed &= lawRunes.depositAll();
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
                testsPassed &= ctx.depositBox().withId(lawRuneId).first() == null;
            } else {
                log.warn("No law runes found, skipping depositAll test");
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