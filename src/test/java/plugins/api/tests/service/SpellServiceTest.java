package plugins.api.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.container.bank.BankEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.magic.spellbook.Spellbook;
import com.kraken.api.service.magic.spellbook.Standard;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.util.RandomUtils;
import plugins.api.requirements.BankState;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.NpcRequirement;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.SkillRequirement;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

@Slf4j
public class SpellServiceTest extends BaseApiTest {

    @Inject
    private MagicService magicService;

    @Inject
    private BankService bankService;

    @Override
    public TestRequirements requirements() {
        // Part of this test asserts hasRequiredRunes returns false before the law rune is withdrawn, so
        // it must start with an empty inventory as well as an open bank.
        return TestRequirements.builder()
                .facility(Facility.BANK_BOOTH)
                .facility(Facility.COMBAT_NPCS_F2P)
                .bankState(BankState.OPEN)
                .bankStock(ItemRequirement.of("Mind rune", 10))
                .bankStock(ItemRequirement.of("Fire rune", 50))
                .bankStock(ItemRequirement.of("Air rune", 5))
                .bankStock(ItemRequirement.of("Law rune", 1))
                .nearbyNpc(NpcRequirement.named("Guard"))
                .skill(SkillRequirement.of(Skill.MAGIC, 25))
                .spellbook(Spellbook.STANDARD)
                .sideEffect(SideEffect.EMPTIES_INVENTORY)
                .sideEffect(SideEffect.CONSUMES_ITEMS)
                .sideEffect(SideEffect.TELEPORTS)
                // Ends with a Varrock teleport, which lands next to the hub, so it costs almost nothing
                // to recover from provided it runs late in the group.
                .orderHint(50)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) {
        boolean testsPassed = true;

        try {

            // Setup
            if(!bankService.isOpen()) {
                log.error("Cannot execute spell service tests, bank is not open");
                return false;
            }

            bankService.depositAll();
            ctx.bank().withName("Mind rune").first().ifPresent(BankEntity::withdrawTen);
            Thread.sleep(RandomUtils.randomIntBetween(700, 1000));
            ctx.bank().withName("Fire rune").first().ifPresent(e -> e.withdraw(50));
            Thread.sleep(RandomUtils.randomIntBetween(700, 1000));
            ctx.bank().withName("Air rune").first().ifPresent(BankEntity::withdrawFive);
            Thread.sleep(RandomUtils.randomIntBetween(700, 1000));
            boolean hasRunes = magicService.hasRequiredRunes(Standard.VARROCK_TELEPORT);
            if(hasRunes) {
                log.info("Spell Service tests failed, hasRequiredRunes returned true when player should not have VARROCK_TELEPORT runes");
                return false;
            }
            ctx.bank().withName("Law rune").first().ifPresent(BankEntity::withdrawOne);
            bankService.close();
            Thread.sleep(RandomUtils.randomIntBetween(2400, 3200));
            boolean hasRunesTrue = magicService.hasRequiredRunes(Standard.VARROCK_TELEPORT);
            if(!hasRunesTrue) {
                log.info("Spell Service tests failed, hasRequiredRunes returned false when player should have VARROCK_TELEPORT runes");
                return false;
            }


            NpcEntity guard = ctx.npcs().nameContains("Guard").nearest().orElse(null);
            if(guard == null) {
                log.error("Spell Service tests failed, could not find a guard");
                return false;
            }

            log.info("Casting fire strike on guard");
            magicService.castOn(Standard.FIRE_STRIKE, guard.raw());
            SleepService.sleepFor(5);

            log.info("Teleporting away");
            magicService.cast(Standard.VARROCK_TELEPORT);
        } catch (Exception e) {
            log.error("Exception during spell service test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    public String getTestName() {
        return "Spell Service";
    }
}

