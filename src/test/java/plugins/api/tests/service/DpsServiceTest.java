package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.bank.BankEntity;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.util.dps.DpsResult;
import com.kraken.api.service.util.dps.DpsService;
import com.kraken.api.service.util.dps.calc.DpsCalculator;
import com.kraken.api.service.util.dps.model.AttackStyle;
import com.kraken.api.service.util.dps.model.CombatStance;
import com.kraken.api.service.util.dps.model.CombatStyleType;
import com.kraken.api.service.util.dps.model.EquipmentItem;
import com.kraken.api.service.util.dps.model.GearCategory;
import com.kraken.api.service.util.dps.model.GearSlot;
import com.kraken.api.service.util.dps.model.Loadout;
import com.kraken.api.service.util.dps.model.MonsterData;
import com.kraken.api.service.util.dps.model.PlayerSkills;
import com.kraken.api.service.util.dps.model.WeaponCategory;
import com.kraken.api.service.util.dps.search.GearSearchConfig;
import com.kraken.api.service.util.dps.search.GearSearchResult;
import lombok.extern.slf4j.Slf4j;
import plugins.api.tests.BaseApiTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Exercises {@link DpsService} entirely with free to play gear against a live free to play NPC.
 *
 * <p>The test runs in five stages:</p>
 * <ol>
 *     <li>bundled data lookups (items, monsters, gear classification),</li>
 *     <li>DPS of hand built loadouts so different gear can be compared without wearing it,</li>
 *     <li>banking the setup kit,</li>
 *     <li>equipping that kit piece by piece and asserting the live DPS moves the way each piece implies,</li>
 *     <li>the smart gear search, including applying the change set it returns and verifying the
 *         gear it asked for reproduces the DPS it promised.</li>
 * </ol>
 *
 * <p>See {@code docs/TESTS.md} for the bank, level and location requirements.</p>
 */
@Slf4j
@Singleton
public class DpsServiceTest extends BaseApiTest {

    // f2p kit withdrawn from the bank. Everything here is wearable on a free to play world.
    private static final int BRONZE_SCIMITAR = 1321;
    private static final int RUNE_SCIMITAR = 1333;
    private static final int AMULET_OF_POWER = 1731;
    private static final int RUNE_FULL_HELM = 1163;
    private static final int RUNE_PLATEBODY = 1127;
    private static final int RUNE_PLATELEGS = 1079;
    private static final int SHORTBOW = 841;
    private static final int IRON_ARROW = 884;

    // data only lookups, these never need to be in the bank
    private static final int STAFF_OF_AIR = 1381;
    private static final int COINS = 995;
    private static final int UNKNOWN_NPC_ID = -1;

    private static final String GOBLIN = "Goblin";

    /** Items withdrawn before the in game portion of the test, in withdrawal order. */
    private static final List<SetupItem> SETUP_ITEMS = Arrays.asList(
            new SetupItem(BRONZE_SCIMITAR, "Bronze scimitar", 1),
            new SetupItem(RUNE_SCIMITAR, "Rune scimitar", 1),
            new SetupItem(AMULET_OF_POWER, "Amulet of power", 1),
            new SetupItem(RUNE_FULL_HELM, "Rune full helm", 1),
            new SetupItem(RUNE_PLATEBODY, "Rune platebody", 1),
            new SetupItem(RUNE_PLATELEGS, "Rune platelegs", 1),
            new SetupItem(SHORTBOW, "Shortbow", 1),
            new SetupItem(IRON_ARROW, "Iron arrow", 10));

    /** Rune armour, equipped as one step. None of it carries an offensive melee bonus. */
    private static final List<SetupItem> RUNE_ARMOUR = Arrays.asList(
            new SetupItem(RUNE_FULL_HELM, "Rune full helm", 1),
            new SetupItem(RUNE_PLATEBODY, "Rune platebody", 1),
            new SetupItem(RUNE_PLATELEGS, "Rune platelegs", 1));

    /** NPC names searched for a live target, in preference order. All are free to play. */
    private static final List<String> TARGET_NPCS = Arrays.asList("Guard", "Man", "Goblin");

    private static final AttackStyle SLASH = new AttackStyle("Slash", CombatStyleType.SLASH, CombatStance.AGGRESSIVE);
    private static final AttackStyle RAPID = new AttackStyle("Rapid", CombatStyleType.RANGED, CombatStance.RAPID);

    /** Doubles are only ever compared to absorb floating point noise, never real differences. */
    private static final double EPSILON = 1e-9;

    @Inject
    private DpsService dpsService;

    @Inject
    private BankService bankService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean passed = verifyDataLookups();

        NpcEntity target = findTarget(ctx);
        if (!assertNotNull(target, "A live target npc " + TARGET_NPCS + " with known combat stats must be nearby")) {
            return false;
        }

        MonsterData monster = dpsService.monster(target);
        log.info("Target: {} (npc id {}) resolved to '{}' version '{}' with {} hp and {} defence",
                target.getName(), target.getId(), monster.getName(), monster.getVersion(),
                monster.getSkills().getHp(), monster.getSkills().getDef());

        passed &= verifyLoadoutCalculations(monster);

        if (!setupGear(ctx)) {
            return false;
        }

        passed &= verifyEquippedDpsChanges(ctx, target);
        passed &= verifyGearSearch(ctx, target, monster);
        return passed;
    }

    // ------------------------------------------------------------------
    // 1. bundled data lookups
    // ------------------------------------------------------------------

    private boolean verifyDataLookups() {
        boolean passed = true;

        EquipmentItem scimitar = dpsService.item(RUNE_SCIMITAR);
        passed &= assertNotNull(scimitar, "Equipment data for the rune scimitar");
        if (scimitar != null) {
            passed &= assertEquals("Rune scimitar", scimitar.getName(), "Rune scimitar name");
            passed &= assertEquals(GearSlot.WEAPON, scimitar.getGearSlot(), "Rune scimitar gear slot");
            passed &= assertEquals(WeaponCategory.SLASH_SWORD, scimitar.getWeaponCategory(), "Rune scimitar weapon category");
            passed &= assertEquals(4, scimitar.getSpeed(), "Rune scimitar attack speed");
        }

        for (SetupItem item : SETUP_ITEMS) {
            passed &= assertNotNull(dpsService.item(item.id), "Equipment data for " + item.name);
        }

        passed &= assertNull(dpsService.item(COINS), "Coins are not equippable so they have no equipment data");

        // gear classification is what prunes the search space, so every style must be recognised
        passed &= assertEquals(GearCategory.MELEE, dpsService.categorize(RUNE_SCIMITAR), "Rune scimitar gear category");
        passed &= assertEquals(GearCategory.RANGED, dpsService.categorize(SHORTBOW), "Shortbow gear category");
        passed &= assertEquals(GearCategory.RANGED, dpsService.categorize(IRON_ARROW), "Iron arrow gear category");
        passed &= assertEquals(GearCategory.MAGIC, dpsService.categorize(STAFF_OF_AIR), "Staff of air gear category");
        // rune armour is purely defensive, it has no offensive bonus in any style
        passed &= assertEquals(GearCategory.NONE, dpsService.categorize(RUNE_PLATEBODY), "Rune platebody gear category");
        passed &= assertEquals(GearCategory.NONE, dpsService.categorize(COINS), "Unknown items classify as NONE");

        MonsterData goblin = dpsService.monster(GOBLIN);
        passed &= assertNotNull(goblin, "Monster data for a goblin");
        if (goblin != null) {
            passed &= assertEquals(GOBLIN, goblin.getName(), "Goblin name");
            passed &= assertTrue(goblin.getSkills().getHp() > 0, "Goblins should have hitpoints");
            MonsterData byId = dpsService.monster(goblin.getId());
            passed &= assertNotNull(byId, "Monster data for goblin npc id " + goblin.getId());
        }

        // goblins exist at several combat levels, all of them should be reachable by name
        passed &= assertTrue(dpsService.monstersByName(GOBLIN).size() > 1, "Every version of a goblin should be returned by name");
        passed &= assertNull(dpsService.monster(UNKNOWN_NPC_ID), "An unknown npc id has no monster data");
        passed &= assertNull(dpsService.monster("Not a real monster"), "An unknown monster name has no monster data");

        return passed;
    }

    // ------------------------------------------------------------------
    // 2. dps of hand built loadouts
    // ------------------------------------------------------------------

    private boolean verifyLoadoutCalculations(MonsterData monster) {
        boolean passed = true;

        Loadout current = dpsService.currentLoadout();
        PlayerSkills skills = current.getSkills();
        log.info("Calculating with live skill levels: attack {} strength {} defence {} ranged {} magic {}",
                skills.getAttack(), skills.getStrength(), skills.getDefence(), skills.getRanged(), skills.getMagic());

        DpsResult bronze = dpsService.calculate(loadout(skills, SLASH, BRONZE_SCIMITAR), monster);
        DpsResult rune = dpsService.calculate(loadout(skills, SLASH, RUNE_SCIMITAR), monster);
        DpsResult amulet = dpsService.calculate(loadout(skills, SLASH, RUNE_SCIMITAR, AMULET_OF_POWER), monster);
        DpsResult armour = dpsService.calculate(loadout(skills, SLASH, RUNE_SCIMITAR, AMULET_OF_POWER,
                RUNE_FULL_HELM, RUNE_PLATEBODY, RUNE_PLATELEGS), monster);
        DpsResult bow = dpsService.calculate(loadout(skills, RAPID, SHORTBOW, IRON_ARROW), monster);

        log.info("Bronze scimitar        : {}", format(bronze));
        log.info("Rune scimitar          : {}", format(rune));
        log.info("Rune scimitar + amulet : {}", format(amulet));
        log.info("...in full rune armour : {}", format(armour));
        log.info("Shortbow + iron arrows : {}", format(bow));

        // a better weapon is more accurate and hits harder, so it must kill faster
        passed &= assertTrue(rune.getMaxAttackRoll() > bronze.getMaxAttackRoll(),
                "Rune scimitar should roll a higher attack than a bronze scimitar");
        passed &= assertTrue(rune.getMaxHit() >= bronze.getMaxHit(),
                "Rune scimitar should not hit lower than a bronze scimitar");
        passed &= assertTrue(rune.getDps() > bronze.getDps(),
                "Rune scimitar should out dps a bronze scimitar");
        passed &= assertTrue(rune.getExpectedTimeToKillSeconds() < bronze.getExpectedTimeToKillSeconds(),
                "Higher dps should mean a shorter time to kill");

        // amulet of power carries +6 accuracy in every melee style
        passed &= assertTrue(amulet.getMaxAttackRoll() > rune.getMaxAttackRoll(),
                "Amulet of power should raise the attack roll");
        passed &= assertTrue(amulet.getDps() > rune.getDps(),
                "Amulet of power should raise dps");

        // rune armour has no offensive melee bonus, so it must not move melee dps at all
        passed &= assertTrue(Math.abs(armour.getDps() - amulet.getDps()) < EPSILON,
                "Rune armour is purely defensive so melee dps must be unchanged (was "
                        + armour.getDps() + " vs " + amulet.getDps() + ")");

        // result fields themselves
        passed &= assertEquals(4, rune.getAttackSpeedTicks(), "Rune scimitar attack speed in ticks");
        passed &= assertEquals(3, bow.getAttackSpeedTicks(), "Shortbow on rapid attack speed in ticks");
        passed &= assertTrue(rune.getAccuracy() > 0 && rune.getAccuracy() <= 1, "Accuracy should be a probability");
        passed &= assertTrue(rune.getNpcDefenceRoll() > 0, "The npc should roll a defence");
        passed &= assertTrue(rune.getExpectedDamagePerAttack() > 0, "Expected damage per attack should be positive");
        passed &= assertTrue(rune.getExpectedHitsToKill() > 0, "Expected hits to kill should be positive");
        passed &= assertEquals(monster.getId(), rune.getMonsterId(), "Result monster id");
        passed &= assertEquals(monster.getName(), rune.getMonsterName(), "Result monster name");
        passed &= assertEquals(CombatStyleType.SLASH, rune.getStyle().getType(), "Melee result style type");
        passed &= assertEquals(CombatStyleType.RANGED, bow.getStyle().getType(), "Ranged result style type");
        passed &= assertTrue(bow.getDps() > 0, "A shortbow with iron arrows should produce positive dps");

        // the calculator accessor exposes the same numbers as the summarised result
        DpsCalculator calculator = dpsService.calculator(loadout(skills, SLASH, RUNE_SCIMITAR), monster);
        passed &= assertEquals(rune.getMaxHit(), calculator.getMax(), "calculator() and calculate() max hit");
        passed &= assertTrue(Math.abs(calculator.getDps() - rune.getDps()) < EPSILON, "calculator() and calculate() dps");

        // calculating against an npc id resolves the same monster
        DpsResult byNpcId = dpsService.calculate(loadout(skills, SLASH, RUNE_SCIMITAR), monster.getId());
        passed &= assertNotNull(byNpcId, "calculate(loadout, npcId) for npc id " + monster.getId());
        if (byNpcId != null) {
            passed &= assertTrue(Math.abs(byNpcId.getDps() - rune.getDps()) < EPSILON,
                    "calculate(loadout, npcId) should match calculate(loadout, monster)");
        }
        passed &= assertNull(dpsService.calculate(loadout(skills, SLASH, RUNE_SCIMITAR), UNKNOWN_NPC_ID),
                "calculate(loadout, npcId) for an unknown npc id");

        return passed;
    }

    // ------------------------------------------------------------------
    // 3. bank the setup kit
    // ------------------------------------------------------------------

    private boolean setupGear(Context ctx) {
        if (!bankService.isOpen()) {
            GameObjectEntity booth = ctx.gameObjects().nameContains("Bank booth").withAction("Bank").nearest();
            if (booth == null || booth.isNull()) {
                log.error("Cannot set up the dps test, no bank booth nearby.");
                return false;
            }

            booth.interact("Bank");
            SleepService.sleepUntil(bankService::isOpen, 5000);
        }

        if (!bankService.isOpen()) {
            log.error("Cannot set up the dps test, the bank did not open. Is a bank pin blocking the interface?");
            return false;
        }

        log.info("Stripping the player so dps is measured from a known state.");
        bankService.depositAll();
        SleepService.sleepFor(2);
        bankService.depositAllEquipment();
        SleepService.sleepFor(2);
        bankService.setWithdrawMode(false);
        SleepService.sleepFor(1);

        for (SetupItem item : SETUP_ITEMS) {
            BankEntity entity = ctx.bank().withName(item.name).first();
            if (entity == null || entity.isNull()) {
                log.error("Cannot run the dps test, '{}' is missing from the bank. See docs/TESTS.md.", item.name);
                return false;
            }

            if (!entity.withdraw(item.quantity)) {
                log.error("Failed to withdraw {} x {}", item.quantity, item.name);
                return false;
            }
            SleepService.sleepFor(2);
        }

        bankService.close();
        SleepService.sleepWhile(bankService::isOpen, 5000);

        for (SetupItem item : SETUP_ITEMS) {
            if (!ctx.inventory().hasItem(item.id)) {
                log.error("'{}' was not withdrawn into the inventory.", item.name);
                return false;
            }
        }

        return true;
    }

    // ------------------------------------------------------------------
    // 4. equip the kit and watch the live dps move
    // ------------------------------------------------------------------

    private boolean verifyEquippedDpsChanges(Context ctx, NpcEntity target) {
        boolean passed = true;

        Map<GearSlot, EquipmentItem> stripped = dpsService.currentEquipment();
        if (!stripped.isEmpty()) {
            log.warn("Expected nothing to be worn after banking but found {}", describe(stripped));
        }

        DpsResult unarmed = dpsService.calculateCurrent(target);
        if (!assertNotNull(unarmed, "DPS of the current setup vs " + target.getName())) {
            return false;
        }
        log.info("Unarmed                : {}", format(unarmed));

        if (!equip(ctx, BRONZE_SCIMITAR, "Bronze scimitar")) {
            return false;
        }
        DpsResult bronze = dpsService.calculateCurrent(target);
        log.info("Bronze scimitar        : {}", format(bronze));
        // a bronze scimitar is not asserted to beat bare fists: on a defensive stance its slash
        // attack rolls against a higher defence than an unarmed crush attack, and against targets
        // like a guard that is enough for fists to win. Only same stance comparisons are asserted.
        passed &= assertTrue(bronze.getDps() > 0, "Equipping a bronze scimitar should produce positive dps");

        if (!equip(ctx, RUNE_SCIMITAR, "Rune scimitar")) {
            return false;
        }
        DpsResult rune = dpsService.calculateCurrent(target);
        log.info("Rune scimitar          : {}", format(rune));
        passed &= assertTrue(rune.getMaxAttackRoll() > bronze.getMaxAttackRoll(),
                "Swapping to a rune scimitar should raise the attack roll");
        passed &= assertTrue(rune.getDps() > bronze.getDps(), "Swapping to a rune scimitar should raise dps");
        passed &= assertTrue(rune.getDps() > unarmed.getDps(), "A rune scimitar should comfortably beat punching");

        if (!equip(ctx, AMULET_OF_POWER, "Amulet of power")) {
            return false;
        }
        DpsResult amulet = dpsService.calculateCurrent(target);
        log.info("Rune scimitar + amulet : {}", format(amulet));
        passed &= assertTrue(amulet.getMaxAttackRoll() > rune.getMaxAttackRoll(),
                "Wearing an amulet of power should raise the attack roll");
        passed &= assertTrue(amulet.getDps() > rune.getDps(), "Wearing an amulet of power should raise dps");

        for (SetupItem piece : RUNE_ARMOUR) {
            if (!equip(ctx, piece.id, piece.name)) {
                return false;
            }
        }
        DpsResult armour = dpsService.calculateCurrent(target);
        log.info("...in full rune armour : {}", format(armour));
        passed &= assertTrue(Math.abs(armour.getDps() - amulet.getDps()) < EPSILON,
                "Rune armour carries no offensive melee bonus so live melee dps must be unchanged (was "
                        + armour.getDps() + " vs " + amulet.getDps() + ")");

        // the loadout the service reads back must match what is actually worn
        Map<GearSlot, EquipmentItem> worn = dpsService.currentEquipment();
        log.info("Worn gear read back by the service: {}", describe(worn));
        passed &= assertEquals(RUNE_SCIMITAR, itemId(worn.get(GearSlot.WEAPON)), "Weapon slot");
        passed &= assertEquals(AMULET_OF_POWER, itemId(worn.get(GearSlot.NECK)), "Neck slot");
        passed &= assertEquals(RUNE_FULL_HELM, itemId(worn.get(GearSlot.HEAD)), "Head slot");
        passed &= assertEquals(RUNE_PLATEBODY, itemId(worn.get(GearSlot.BODY)), "Body slot");
        passed &= assertEquals(RUNE_PLATELEGS, itemId(worn.get(GearSlot.LEGS)), "Legs slot");

        Loadout live = dpsService.currentLoadout();
        passed &= assertNotNull(live.getStyle(), "The current loadout should resolve the selected attack style");
        passed &= assertEquals(RUNE_SCIMITAR, itemId(live.getWeapon()), "The current loadout weapon");

        // available gear spans worn equipment and everything wearable still in the inventory
        List<Integer> available = new ArrayList<>();
        for (EquipmentItem item : dpsService.availableGear()) {
            available.add(item.getId());
        }
        passed &= assertTrue(available.contains(RUNE_SCIMITAR), "availableGear() should include worn equipment");
        passed &= assertTrue(available.contains(SHORTBOW), "availableGear() should include wearable inventory items");
        passed &= assertTrue(available.contains(IRON_ARROW), "availableGear() should include ammo in the inventory");

        return passed;
    }

    // ------------------------------------------------------------------
    // 5. smart gear search
    // ------------------------------------------------------------------

    private boolean verifyGearSearch(Context ctx, NpcEntity target, MonsterData monster) {
        boolean passed = true;

        DpsResult before = dpsService.calculateCurrent(target);
        log.info("Gear search baseline (currently worn): {}", format(before));

        GearSearchResult result = dpsService.findBestGear(target);
        if (!assertNotNull(result, "Gear search result vs " + target.getName())) {
            return false;
        }

        DpsResult best = result.getBest();
        if (!assertNotNull(best, "Gear search should find at least one usable loadout")) {
            return false;
        }

        log.info("Gear search evaluated {} loadouts, best: {}", result.getEvaluations(), format(best));
        log.info("Best loadout: {}", describe(best.getLoadout().getEquipment()));
        for (Map.Entry<GearCategory, DpsResult> entry : result.getBestPerStyle().entrySet()) {
            log.info("  best {} setup: {} | {}", entry.getKey(),
                    describe(entry.getValue().getLoadout().getEquipment()), format(entry.getValue()));
        }

        passed &= assertTrue(result.getEvaluations() > 0, "The search should evaluate at least one loadout");
        passed &= assertTrue(best.getDps() > 0, "The best loadout should have positive dps");
        passed &= assertTrue(best.getDps() + EPSILON >= before.getDps(),
                "The search should never come back worse than the gear already worn");

        Map<GearCategory, DpsResult> perStyle = result.getBestPerStyle();
        passed &= assertTrue(perStyle.containsKey(GearCategory.MELEE),
                "A melee setup should be found, two scimitars are available");
        passed &= assertTrue(perStyle.containsKey(GearCategory.RANGED),
                "A ranged setup should be found, a shortbow and iron arrows are available");
        passed &= assertTrue(!perStyle.containsKey(GearCategory.MAGIC),
                "No magic weapon is available so no magic setup should be returned");

        DpsResult bestMelee = perStyle.get(GearCategory.MELEE);
        if (bestMelee != null) {
            passed &= assertEquals(RUNE_SCIMITAR, itemId(bestMelee.getLoadout().get(GearSlot.WEAPON)),
                    "The best melee setup should pick the rune scimitar over the bronze one");
        }

        DpsResult bestRanged = perStyle.get(GearCategory.RANGED);
        if (bestRanged != null) {
            passed &= assertEquals(SHORTBOW, itemId(bestRanged.getLoadout().get(GearSlot.WEAPON)),
                    "The best ranged setup should pick the only bow available");
            passed &= assertEquals(IRON_ARROW, itemId(bestRanged.getLoadout().get(GearSlot.AMMO)),
                    "The best ranged setup should load the only compatible ammo available");
        }

        passed &= applyChangeSet(ctx, result, monster, "best overall");

        // restricting the search to one style should come back with a plan that swaps weapons.
        // This also covers the findBestGear(monster, config) overload.
        Loadout current = dpsService.currentLoadout();
        GearSearchConfig rangedOnly = GearSearchConfig.builder()
                .skills(current.getSkills())
                .boosts(current.getBoosts())
                .prayers(current.getPrayers())
                .styles(Collections.singletonList(GearCategory.RANGED))
                .build();

        GearSearchResult ranged = dpsService.findBestGear(monster, rangedOnly);
        if (!assertNotNull(ranged, "Ranged only gear search result")) {
            return false;
        }
        if (!assertNotNull(ranged.getBest(), "Ranged only gear search should find a usable loadout")) {
            return false;
        }

        log.info("Ranged only search: {} | {}", describe(ranged.getBest().getLoadout().getEquipment()),
                format(ranged.getBest()));
        passed &= assertEquals(1, ranged.getBestPerStyle().size(), "A style restricted search should return one style");
        passed &= assertTrue(ranged.getBestPerStyle().containsKey(GearCategory.RANGED),
                "A ranged only search should only return a ranged setup");
        passed &= assertTrue(!ranged.getItemsToEquip().isEmpty(),
                "Switching from melee to ranged should require equipping the bow and ammo");
        passed &= assertEquals(SHORTBOW, itemId(ranged.getBest().getLoadout().get(GearSlot.WEAPON)), "Ranged search weapon");
        passed &= assertEquals(IRON_ARROW, itemId(ranged.getBest().getLoadout().get(GearSlot.AMMO)), "Ranged search ammo");

        passed &= applyChangeSet(ctx, ranged, monster, "ranged only");

        return passed;
    }

    /**
     * Applies the equips and removals a search asked for, then verifies the gear now worn is the
     * loadout the search chose and reproduces the dps it reported.
     * @param ctx The api context
     * @param result The search result to apply
     * @param monster The monster the search ran against
     * @param label Description of the search, used for logging
     * @return True when the change set landed exactly
     */
    private boolean applyChangeSet(Context ctx, GearSearchResult result, MonsterData monster, String label) {
        boolean passed = true;
        DpsResult best = result.getBest();

        log.info("Change set ({}): equip {}, remove {}", label, describe(result.getItemsToEquip()), result.getSlotsToRemove());
        for (Map.Entry<GearSlot, EquipmentItem> entry : result.getItemsToEquip().entrySet()) {
            if (!equip(ctx, entry.getValue().getId(), entry.getValue().getName())) {
                return false;
            }
        }

        for (GearSlot slot : result.getSlotsToRemove()) {
            EquipmentEntity entity = wornIn(ctx, slot);
            if (entity == null) {
                continue;
            }

            entity.remove();
            SleepService.sleepFor(2);
            passed &= assertTrue(wornIn(ctx, slot) == null, "The " + slot + " slot should be empty after removing it");
        }

        Map<GearSlot, EquipmentItem> worn = dpsService.currentEquipment();
        log.info("Worn gear after applying the {} change set: {}", label, describe(worn));
        for (GearSlot slot : GearSlot.values()) {
            passed &= assertEquals(itemId(best.getLoadout().get(slot)), itemId(worn.get(slot)),
                    "The " + slot + " slot after applying the " + label + " change set");
        }

        // wearing what the search asked for must reproduce the dps it promised
        Loadout achieved = best.getLoadout().copy();
        achieved.setEquipment(worn);
        DpsResult after = dpsService.calculate(achieved, monster);
        log.info("Searched {} dps {} vs achieved dps {}", label, best.getDps(), after.getDps());
        passed &= assertTrue(Math.abs(after.getDps() - best.getDps()) < EPSILON,
                "The gear now worn should reproduce the dps the " + label + " search reported");

        return passed;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Finds the nearest free to play npc with known combat stats.
     * @param ctx The api context
     * @return NpcEntity to calculate against, or null when nothing suitable is nearby
     */
    private NpcEntity findTarget(Context ctx) {
        for (String name : TARGET_NPCS) {
            NpcEntity npc = ctx.npcs().withName(name).nearest();
            if (npc != null && !npc.isNull() && dpsService.monster(npc) != null) {
                return npc;
            }
        }
        return null;
    }

    /**
     * Builds a loadout from item ids, letting each item pick its own slot.
     * @param skills The skill levels to calculate with
     * @param style The attack style to calculate with
     * @param itemIds The items to wear
     * @return Loadout ready to hand to the service
     */
    private Loadout loadout(PlayerSkills skills, AttackStyle style, int... itemIds) {
        Map<GearSlot, EquipmentItem> gear = new EnumMap<>(GearSlot.class);
        for (int itemId : itemIds) {
            EquipmentItem item = dpsService.item(itemId);
            if (item == null) {
                log.error("No equipment data for item id {}, the loadout will be incomplete", itemId);
                continue;
            }
            gear.put(item.getGearSlot(), item);
        }

        return Loadout.builder()
                .equipment(gear)
                .skills(skills)
                .boosts(PlayerSkills.none())
                .prayers(new ArrayList<>())
                .style(style)
                .build();
    }

    /**
     * Equips an item from the inventory and waits for it to actually be worn.
     * @param ctx The api context
     * @param itemId The item to equip
     * @param name The item name, used for logging
     * @return True when the item is worn
     */
    private boolean equip(Context ctx, int itemId, String name) {
        if (ctx.equipment().isWearing(itemId)) {
            return true;
        }

        EquipmentEntity item = ctx.equipment().inInventory().withId(itemId).first();
        if (item == null || item.isNull()) {
            log.error("Cannot equip {} ({}), it is not in the inventory", name, itemId);
            return false;
        }

        item.wieldOrWear();
        boolean worn = SleepService.sleepUntil(() -> ctx.equipment().isWearing(itemId), 5000);
        if (!worn) {
            log.error("Failed to equip {} ({})", name, itemId);
        }
        return worn;
    }

    /**
     * Finds the item currently worn in a gear slot.
     * @param ctx The api context
     * @param slot The gear slot to read
     * @return EquipmentEntity worn in that slot, or null when the slot is empty
     */
    private EquipmentEntity wornIn(Context ctx, GearSlot slot) {
        for (EquipmentEntity entity : ctx.equipment().inInterface().list()) {
            if (entity == null || entity.raw() == null) {
                continue;
            }
            if (GearSlot.fromRuneLite(entity.getSlot()) == slot) {
                return entity;
            }
        }
        return null;
    }

    private int itemId(EquipmentItem item) {
        return item == null ? -1 : item.getId();
    }

    private String format(DpsResult result) {
        return String.format("dps %.4f, max hit %d, accuracy %.2f%%, attack roll %d, defence roll %d, %d ticks, ttk %.1fs",
                result.getDps(), result.getMaxHit(), result.getAccuracy() * 100, result.getMaxAttackRoll(),
                result.getNpcDefenceRoll(), result.getAttackSpeedTicks(), result.getExpectedTimeToKillSeconds());
    }

    private String describe(Map<GearSlot, EquipmentItem> equipment) {
        StringBuilder sb = new StringBuilder();
        for (GearSlot slot : GearSlot.values()) {
            EquipmentItem item = equipment.get(slot);
            if (item == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(slot).append('=').append(item.getName());
        }
        return sb.length() == 0 ? "nothing" : sb.toString();
    }

    @Override
    public String getTestName() {
        return "DPS Service";
    }

    /** An item withdrawn from the bank before the in game portion of the test. */
    private static final class SetupItem {
        private final int id;
        private final String name;
        private final int quantity;

        private SetupItem(int id, String name, int quantity) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
        }
    }
}
