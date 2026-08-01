package unit.com.kraken.api.dps;

import com.google.gson.Gson;
import com.kraken.api.service.util.dps.calc.DpsCalculator;
import com.kraken.api.service.util.dps.data.DpsDataStore;
import com.kraken.api.service.util.dps.model.AttackStyle;
import com.kraken.api.service.util.dps.model.CombatStance;
import com.kraken.api.service.util.dps.model.CombatStyleType;
import com.kraken.api.service.util.dps.model.EquipmentItem;
import com.kraken.api.service.util.dps.model.GearSlot;
import com.kraken.api.service.util.dps.model.Loadout;
import com.kraken.api.service.util.dps.model.MonsterData;
import com.kraken.api.service.util.dps.model.PlayerSkills;
import com.kraken.api.service.util.dps.model.PrayerBoost;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies the ported DPS calculation against expected values taken directly from the
 * osrs-dps-calc TypeScript project's test suite (BasicRolls.test.ts, CombatCalc.test.ts
 * and GeneratedTests.test.ts), which themselves encode in-game verified values.
 */
class DpsCalculatorTest {

    private static final int ABYSSAL_DEMON = 415;
    private static final int VORKATH = 8059;
    private static final int REVENANT_KNIGHT = 7939;
    private static final int SCURRIUS = 7222;
    private static final int GIANT_CRYPT_RAT = 1680;
    private static final int VANSTROM_KLAUSE = 9567;

    private static final DpsDataStore DATA = new DpsDataStore(new Gson());

    private static final AttackStyle LUNGE_STAB_AGGRESSIVE = new AttackStyle("Lunge", CombatStyleType.STAB, CombatStance.AGGRESSIVE);
    private static final AttackStyle LUNGE_STAB_CONTROLLED = new AttackStyle("Lunge", CombatStyleType.STAB, CombatStance.CONTROLLED);
    private static final AttackStyle POUND_CRUSH_AGGRESSIVE = new AttackStyle("Pound", CombatStyleType.CRUSH, CombatStance.AGGRESSIVE);
    private static final AttackStyle PUMMEL_CRUSH_AGGRESSIVE = new AttackStyle("Pummel", CombatStyleType.CRUSH, CombatStance.AGGRESSIVE);
    private static final AttackStyle MAGIC_ACCURATE = new AttackStyle("Accurate", CombatStyleType.MAGIC, CombatStance.ACCURATE);
    private static final AttackStyle AUTOCAST = new AttackStyle("Spell", CombatStyleType.MAGIC, CombatStance.AUTOCAST);

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static MonsterData monster(int id) {
        MonsterData m = DATA.monster(id);
        assertNotNull(m, "missing monster id " + id);
        return m;
    }

    private static Loadout.LoadoutBuilder player(AttackStyle style, PlayerSkills skills, List<PrayerBoost> prayers) {
        return Loadout.builder()
                .style(style)
                .skills(skills)
                .boosts(PlayerSkills.none())
                .prayers(prayers)
                // the wiki calculator's test player defaults to being on a slayer task
                .onSlayerTask(true);
    }

    private static Loadout equip(Loadout loadout, Object... slotItemPairs) {
        for (int i = 0; i < slotItemPairs.length; i += 2) {
            GearSlot slot = (GearSlot) slotItemPairs[i];
            int itemId = (Integer) slotItemPairs[i + 1];
            EquipmentItem item = DATA.equipment(itemId);
            assertNotNull(item, "missing equipment id " + itemId);
            loadout.getEquipment().put(slot, item);
        }
        return loadout;
    }

    private static PlayerSkills skills(int atk, int str, int ranged, int magic) {
        return PlayerSkills.builder().attack(atk).strength(str).ranged(ranged).magic(magic).build();
    }

    /** Max melee gear from the generated tests: torva, infernal cape, torture, avernic, ferocious, primordial, b ring (i). */
    private static Loadout maxMelee(AttackStyle style, int neckId, int headId) {
        Loadout loadout = player(style, skills(118, 118, 99, 99), Collections.singletonList(PrayerBoost.PIETY)).build();
        return equip(loadout,
                GearSlot.HEAD, headId,
                GearSlot.CAPE, 21285,
                GearSlot.NECK, neckId,
                GearSlot.BODY, 26384,
                GearSlot.SHIELD, 22322,
                GearSlot.LEGS, 26386,
                GearSlot.HANDS, 22981,
                GearSlot.FEET, 13239,
                GearSlot.RING, 11773);
    }

    /** Elite void melee gear from the generated tests. */
    private static Loadout voidMelee(AttackStyle style, int neckId) {
        Loadout loadout = player(style, skills(118, 118, 99, 99), Collections.singletonList(PrayerBoost.PIETY)).build();
        return equip(loadout,
                GearSlot.HEAD, 11665,
                GearSlot.CAPE, 21285,
                GearSlot.NECK, neckId,
                GearSlot.BODY, 13072,
                GearSlot.SHIELD, 22322,
                GearSlot.LEGS, 13073,
                GearSlot.HANDS, 8842,
                GearSlot.FEET, 13239,
                GearSlot.RING, 11773);
    }

    /** Obsidian armour + zerker gear from the generated tests. */
    private static Loadout obsidian(int neckId) {
        Loadout loadout = player(LUNGE_STAB_AGGRESSIVE, skills(118, 118, 99, 99), Collections.singletonList(PrayerBoost.PIETY)).build();
        return equip(loadout,
                GearSlot.HEAD, 21298,
                GearSlot.CAPE, 21285,
                GearSlot.NECK, neckId,
                GearSlot.WEAPON, 6523,
                GearSlot.BODY, 21301,
                GearSlot.SHIELD, 22322,
                GearSlot.LEGS, 21304,
                GearSlot.HANDS, 22981,
                GearSlot.FEET, 13239,
                GearSlot.RING, 11773);
    }

    /** Max mage gear (ancestral, occult, tormented, magus ring) from the generated tests. */
    private static Loadout maxMage(int headId, int neckId, int weaponId, Integer shieldId) {
        Loadout loadout = player(MAGIC_ACCURATE, skills(99, 99, 99, 112), Collections.emptyList()).build();
        equip(loadout,
                GearSlot.HEAD, headId,
                GearSlot.CAPE, 21780,
                GearSlot.NECK, neckId,
                GearSlot.WEAPON, weaponId,
                GearSlot.BODY, 21021,
                GearSlot.LEGS, 21024,
                GearSlot.HANDS, 19544,
                GearSlot.FEET, 13235,
                GearSlot.RING, 28313);
        if (shieldId != null) {
            equip(loadout, GearSlot.SHIELD, shieldId);
        }
        return loadout;
    }

    private static int maxHit(Loadout loadout, int monsterId) {
        return new DpsCalculator(DATA, loadout, monster(monsterId)).getMax();
    }

    // ------------------------------------------------------------------
    // BasicRolls.test.ts
    // ------------------------------------------------------------------

    @Test
    void meleeBasicRolls() {
        Loadout l1 = player(null, PlayerSkills.builder().attack(1).build(), Collections.emptyList()).build();
        equip(l1, GearSlot.WEAPON, 4151); // Abyssal whip
        assertEquals(1752, new DpsCalculator(DATA, l1, monster(ABYSSAL_DEMON)).getMaxAttackRoll());

        Loadout l2 = player(null, PlayerSkills.builder().strength(1).build(), Collections.emptyList()).build();
        equip(l2, GearSlot.WEAPON, 4151);
        assertEquals(2, new DpsCalculator(DATA, l2, monster(ABYSSAL_DEMON)).getMax());

        Loadout l3 = player(null, PlayerSkills.maxed(), Collections.emptyList()).build();
        equip(l3, GearSlot.WEAPON, 4151);
        DpsCalculator calc = new DpsCalculator(DATA, l3, monster(ABYSSAL_DEMON));
        assertEquals(16060, calc.getMaxAttackRoll());
        assertEquals(24, calc.getMax());
    }

    @Test
    void rangedBasicRolls() {
        Loadout l1 = player(null, PlayerSkills.builder().ranged(1).build(), Collections.emptyList()).build();
        equip(l1, GearSlot.WEAPON, 25865); // Bow of faerdhinen (charged)
        DpsCalculator low = new DpsCalculator(DATA, l1, monster(ABYSSAL_DEMON));
        assertEquals(2304, low.getMaxAttackRoll());
        assertEquals(3, low.getMax());

        Loadout l2 = player(null, PlayerSkills.maxed(), Collections.emptyList()).build();
        equip(l2, GearSlot.WEAPON, 25865);
        DpsCalculator high = new DpsCalculator(DATA, l2, monster(ABYSSAL_DEMON));
        assertEquals(21120, high.getMaxAttackRoll());
        assertEquals(29, high.getMax());
    }

    @Test
    void magicBasicRolls() {
        Loadout l1 = player(null, PlayerSkills.builder().magic(1).build(), Collections.emptyList()).build();
        equip(l1, GearSlot.WEAPON, 11905); // Trident of the seas
        DpsCalculator low = new DpsCalculator(DATA, l1, monster(ABYSSAL_DEMON));
        assertEquals(948, low.getMaxAttackRoll());
        assertEquals(1, low.getMax());

        Loadout l2 = player(null, PlayerSkills.maxed(), Collections.emptyList()).build();
        equip(l2, GearSlot.WEAPON, 11905);
        DpsCalculator high = new DpsCalculator(DATA, l2, monster(ABYSSAL_DEMON));
        assertEquals(8690, high.getMaxAttackRoll());
        assertEquals(28, high.getMax());
    }

    // ------------------------------------------------------------------
    // CombatCalc.test.ts — end-to-end dps
    // ------------------------------------------------------------------

    @Test
    void emptyPlayerAgainstAbyssalDemon() {
        Loadout loadout = player(null, PlayerSkills.maxed(), Collections.emptyList()).build();
        DpsCalculator calc = new DpsCalculator(DATA, loadout, monster(ABYSSAL_DEMON));

        assertEquals(11, calc.getMax());
        assertEquals(7040, calc.getMaxAttackRoll());
        assertEquals(12096, calc.getNPCDefenceRoll());
        assertEquals(29.10, calc.getHitChance() * 100, 0.01);
        assertEquals(0.677, calc.getDps(), 0.001);
    }

    // ------------------------------------------------------------------
    // GeneratedTests.test.ts — in-game verified max hits
    // ------------------------------------------------------------------

    @Test
    void fangMaxMelee() {
        Loadout loadout = maxMelee(LUNGE_STAB_AGGRESSIVE, 19553, 26382);
        equip(loadout, GearSlot.WEAPON, 26219);
        assertEquals(50, maxHit(loadout, ABYSSAL_DEMON));
    }

    @Test
    void fangWithSalveMaxMelee() {
        Loadout loadout = maxMelee(LUNGE_STAB_AGGRESSIVE, 12018, 26382);
        equip(loadout, GearSlot.WEAPON, 26219);
        assertEquals(57, maxHit(loadout, VORKATH));
    }

    @Test
    void fangWithAvariceMaxMelee() {
        Loadout loadout = maxMelee(LUNGE_STAB_AGGRESSIVE, 22557, 26382);
        equip(loadout, GearSlot.WEAPON, 26219);
        assertEquals(58, maxHit(loadout, REVENANT_KNIGHT));
    }

    @Test
    void fangWithSlayerHelmetMaxMelee() {
        Loadout loadout = maxMelee(LUNGE_STAB_AGGRESSIVE, 19553, 11865);
        equip(loadout, GearSlot.WEAPON, 26219);
        assertEquals(56, maxHit(loadout, ABYSSAL_DEMON));
    }

    @Test
    void fangInVoid() {
        Loadout loadout = voidMelee(LUNGE_STAB_AGGRESSIVE, 19553);
        equip(loadout, GearSlot.WEAPON, 26219);
        assertEquals(47, maxHit(loadout, ABYSSAL_DEMON));
    }

    @Test
    void fangWithSalveInVoid() {
        Loadout loadout = voidMelee(LUNGE_STAB_AGGRESSIVE, 12018);
        equip(loadout, GearSlot.WEAPON, 26219);
        assertEquals(53, maxHit(loadout, VORKATH));
    }

    @Test
    void dragonHunterLanceMaxMelee() {
        Loadout loadout = maxMelee(LUNGE_STAB_CONTROLLED, 19553, 26382);
        equip(loadout, GearSlot.WEAPON, 22978);
        assertEquals(58, maxHit(loadout, VORKATH));
    }

    @Test
    void blisterwoodFlailMaxMelee() {
        Loadout loadout = maxMelee(POUND_CRUSH_AGGRESSIVE, 19553, 26382);
        equip(loadout, GearSlot.WEAPON, 24699);
        assertEquals(55, maxHit(loadout, VANSTROM_KLAUSE));
    }

    @Test
    void obsidianSwordInObsidianArmour() {
        assertEquals(46, maxHit(obsidian(19553), ABYSSAL_DEMON));
    }

    @Test
    void obsidianSwordWithSalve() {
        assertEquals(52, maxHit(obsidian(12018), VORKATH));
    }

    @Test
    void obsidianSwordWithAvarice() {
        assertEquals(53, maxHit(obsidian(22557), REVENANT_KNIGHT));
    }

    @Test
    void viggorasChainmaceMaxMelee() {
        Loadout loadout = maxMelee(PUMMEL_CRUSH_AGGRESSIVE, 19553, 26382);
        equip(loadout, GearSlot.WEAPON, 22545);
        loadout.setInWilderness(true);
        assertEquals(73, maxHit(loadout, ABYSSAL_DEMON));
    }

    @Test
    void viggorasChainmaceInVoid() {
        Loadout loadout = voidMelee(PUMMEL_CRUSH_AGGRESSIVE, 19553);
        equip(loadout, GearSlot.WEAPON, 22545);
        loadout.setInWilderness(true);
        assertEquals(67, maxHit(loadout, ABYSSAL_DEMON));
    }

    @Test
    void viggorasChainmaceWithAvarice() {
        Loadout loadout = maxMelee(PUMMEL_CRUSH_AGGRESSIVE, 22557, 26382);
        equip(loadout, GearSlot.WEAPON, 22545);
        loadout.setInWilderness(true);
        assertEquals(85, maxHit(loadout, REVENANT_KNIGHT));
    }

    @Test
    void viggorasChainmaceWithAvariceInVoid() {
        Loadout loadout = voidMelee(PUMMEL_CRUSH_AGGRESSIVE, 22557);
        equip(loadout, GearSlot.WEAPON, 22545);
        loadout.setInWilderness(true);
        assertEquals(78, maxHit(loadout, REVENANT_KNIGHT));
    }

    @Test
    void tumekensShadowMaxMage() {
        assertEquals(65, maxHit(maxMage(21018, 12002, 27275, null), ABYSSAL_DEMON));
    }

    @Test
    void tumekensShadowWithSalve() {
        assertEquals(67, maxHit(maxMage(21018, 12018, 27275, null), VORKATH));
    }

    @Test
    void tumekensShadowWithSlayerHelmet() {
        assertEquals(70, maxHit(maxMage(11865, 12002, 27275, null), ABYSSAL_DEMON));
    }

    @Test
    void tumekensShadowInVoid() {
        Loadout loadout = player(MAGIC_ACCURATE, skills(99, 99, 99, 112), Collections.emptyList()).build();
        equip(loadout,
                GearSlot.HEAD, 11663,
                GearSlot.CAPE, 21780,
                GearSlot.NECK, 12002,
                GearSlot.WEAPON, 27275,
                GearSlot.BODY, 13072,
                GearSlot.LEGS, 13073,
                GearSlot.HANDS, 8842,
                GearSlot.FEET, 13235,
                GearSlot.RING, 28313);
        assertEquals(51, maxHit(loadout, ABYSSAL_DEMON));
    }

    @Test
    void tumekensShadowWithSalveInVoid() {
        Loadout loadout = player(MAGIC_ACCURATE, skills(118, 118, 99, 112), Collections.emptyList()).build();
        equip(loadout,
                GearSlot.HEAD, 11663,
                GearSlot.CAPE, 21780,
                GearSlot.NECK, 12018,
                GearSlot.WEAPON, 27275,
                GearSlot.BODY, 13072,
                GearSlot.LEGS, 13073,
                GearSlot.HANDS, 8842,
                GearSlot.FEET, 13235,
                GearSlot.RING, 28313);
        assertEquals(53, maxHit(loadout, VORKATH));
    }

    @Test
    void boneStaffMaxMage() {
        assertEquals(53, maxHit(maxMage(21018, 12002, 28796, 25985), SCURRIUS));
    }

    @Test
    void boneStaffWithSlayerHelmet() {
        assertEquals(59, maxHit(maxMage(11865, 12002, 28796, 25985), GIANT_CRYPT_RAT));
    }

    @Test
    void boneStaffInVoid() {
        Loadout loadout = player(MAGIC_ACCURATE, skills(99, 99, 99, 112), Collections.emptyList()).build();
        equip(loadout,
                GearSlot.HEAD, 11663,
                GearSlot.CAPE, 21780,
                GearSlot.NECK, 12002,
                GearSlot.WEAPON, 28796,
                GearSlot.BODY, 13072,
                GearSlot.SHIELD, 25985,
                GearSlot.LEGS, 13073,
                GearSlot.HANDS, 8842,
                GearSlot.FEET, 13235,
                GearSlot.RING, 28313);
        assertEquals(49, maxHit(loadout, SCURRIUS));
    }

    @Test
    void fireBoltWithChaosGauntlets() {
        Loadout loadout = maxMage(21018, 12002, 11791, 25985);
        equip(loadout, GearSlot.HANDS, 777);
        loadout.setStyle(AUTOCAST);
        loadout.setSpell(DATA.spell("Fire Bolt"));
        assertEquals(20, maxHit(loadout, ABYSSAL_DEMON));
    }

    @Test
    void fireBoltWithChaosGauntletsAndSalve() {
        Loadout loadout = maxMage(21018, 12018, 11791, 25985);
        equip(loadout, GearSlot.HANDS, 777);
        loadout.setStyle(AUTOCAST);
        loadout.setSpell(DATA.spell("Fire Bolt"));
        assertEquals(28, maxHit(loadout, VORKATH));
    }

    @Test
    void fireBoltWithChaosGauntletsAndTomeOfFire() {
        Loadout loadout = maxMage(21018, 12002, 11791, 20714);
        equip(loadout, GearSlot.HANDS, 777);
        loadout.setStyle(AUTOCAST);
        loadout.setSpell(DATA.spell("Fire Bolt"));
        assertEquals(22, maxHit(loadout, ABYSSAL_DEMON));
    }

    @Test
    void fireBoltWithChaosGauntletsSalveAndTomeOfFire() {
        Loadout loadout = maxMage(21018, 12018, 11791, 20714);
        equip(loadout, GearSlot.HANDS, 777);
        loadout.setStyle(AUTOCAST);
        loadout.setSpell(DATA.spell("Fire Bolt"));
        assertEquals(30, maxHit(loadout, VORKATH));
    }

    // ------------------------------------------------------------------
    // Twisted bow scaling sanity (formula-derived, matches the wiki calculator)
    // ------------------------------------------------------------------

    @Test
    void twistedBowScalesWithMonsterMagic() {
        // vs a high-magic target the tbow should vastly outperform its base stats
        Loadout loadout = player(new AttackStyle("Rapid", CombatStyleType.RANGED, CombatStance.RAPID),
                PlayerSkills.maxed(), Collections.singletonList(PrayerBoost.RIGOUR)).build();
        equip(loadout,
                GearSlot.WEAPON, 20997,
                GearSlot.AMMO, 11212); // dragon arrows

        // Zulrah (serpentine) has 300 magic
        DpsCalculator vsZulrah = new DpsCalculator(DATA, loadout, monster(2042));
        // Abyssal demon has low magic
        DpsCalculator vsDemon = new DpsCalculator(DATA, loadout, monster(ABYSSAL_DEMON));

        org.junit.jupiter.api.Assertions.assertTrue(vsZulrah.getMax() > vsDemon.getMax(),
                "tbow max hit should scale up against high-magic monsters");
        // bare tbow + dragon arrows + rigour: 29 base max scaled by the 215% zulrah modifier = 62,
        // then capped to 50 by Zulrah's reroll of hits above 50
        assertEquals(50, vsZulrah.getMax());
        assertEquals(15, vsDemon.getMax());
    }
}
