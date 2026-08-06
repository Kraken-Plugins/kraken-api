package com.kraken.api.service.util.dps.calc;

import com.kraken.api.service.util.dps.data.DpsDataStore;
import com.kraken.api.service.util.dps.data.RangedAmmo;
import com.kraken.api.service.util.dps.model.AttackStyle;
import com.kraken.api.service.util.dps.model.CombatStance;
import com.kraken.api.service.util.dps.model.CombatStyleType;
import com.kraken.api.service.util.dps.model.EquipmentItem;
import com.kraken.api.service.util.dps.model.GearSlot;
import com.kraken.api.service.util.dps.model.Loadout;
import com.kraken.api.service.util.dps.model.MonsterAttributeType;
import com.kraken.api.service.util.dps.model.MonsterData;
import com.kraken.api.service.util.dps.model.PrayerBoost;
import com.kraken.api.service.util.dps.model.RangedDamageType;
import com.kraken.api.service.util.dps.model.SpellData;
import com.kraken.api.service.util.dps.model.WeaponCategory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kraken.api.service.util.dps.calc.DpsConstants.*;

/**
 * Player-vs-NPC combat calculator, ported from the OSRS wiki DPS calculator's
 * PlayerVsNPCCalc.ts. Computes attack rolls, hit chance, max hits, full hit
 * distributions and DPS for normal attacks including gear special effects
 * (void, slayer helmet, salve, twisted bow, Osmumten's fang, scythe, enchanted
 * bolts, and many more).
 *
 * <p>Not modelled: weapon special attacks, raid party scaling (CoX/ToB/ToA),
 * and multi-phase boss phase mechanics.</p>
 */
@Slf4j
public class DpsCalculator {

    private final DpsDataStore data;
    private final Loadout player;
    private final MonsterData monster;
    private final AttackStyle style;
    private final Set<String> equippedNames = new HashSet<>();

    // aggregate equipment bonuses (post set-effect adjustments)
    private int totalStr;
    private int totalRangedStr;
    private int totalMagicStr;
    private int totalPrayerBonus;
    private int offStab;
    private int offSlash;
    private int offCrush;
    private int offMagic;
    private int offRanged;
    private int defStab;
    private int defSlash;
    private int defCrush;
    private int defMagic;
    private int defRanged;

    private final Integer defenceRollOverride;

    private SpellData spell;
    private AttackDistribution memoizedDist;
    private Double memoizedHitChance;

    /**
     * Creates a calculator for the given loadout against the given monster.
     * @param data The bundled wiki data store
     * @param loadout The player's loadout; a defensive copy is taken and sanitized
     * @param monster The target monster; a copy is taken and defence reductions applied
     */
    public DpsCalculator(DpsDataStore data, Loadout loadout, MonsterData monster) {
        this(data, loadout, monster, null);
    }

    private DpsCalculator(DpsDataStore data, Loadout loadout, MonsterData monster, Integer defenceRollOverride) {
        this.data = data;
        this.player = loadout.copy();
        this.monster = monster.copy();
        this.defenceRollOverride = defenceRollOverride;

        canonicalizeEquipment();
        this.style = resolveStyle();
        this.spell = player.getSpell();
        sanitizeInputs();
        MonsterScaling.applyDefenceReductions(this.monster);
        computeEquipmentTotals();
    }

    private DpsCalculator subCalcWithDefenceRoll(int defenceRoll) {
        return new DpsCalculator(data, player, monster, defenceRoll);
    }

    // ------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------

    private void canonicalizeEquipment() {
        Map<GearSlot, EquipmentItem> eq = player.getEquipment();
        for (GearSlot slot : GearSlot.values()) {
            EquipmentItem item = eq.get(slot);
            if (item == null) continue;
            EquipmentItem canonical = data.equipment(item.getId());
            if (canonical != null) {
                eq.put(slot, canonical);
                equippedNames.add(canonical.getName());
            } else {
                equippedNames.add(item.getName());
            }
        }
    }

    private AttackStyle resolveStyle() {
        if (player.getStyle() != null) return player.getStyle();
        EquipmentItem weapon = player.getWeapon();
        WeaponCategory category = weapon == null ? WeaponCategory.UNARMED : weapon.getWeaponCategory();
        return category.getAttackStyles().get(0);
    }

    private void sanitizeInputs() {
        if (style.getStance() == null || !style.getStance().isCastStance()) {
            spell = null;
        }

        String spellName = spell == null ? null : spell.getName();
        if (spellName != null) {
            // certain spells require specific weapons to be equipped
            if (("Iban Blast".equals(spellName) && !wearing("Iban's staff", "Iban's staff (u)"))
                    || ("Saradomin Strike".equals(spellName) && !wearing("Saradomin staff", "Staff of light"))
                    || ("Claws of Guthix".equals(spellName) && !wearing("Guthix staff", "Void knight mace", "Staff of balance"))
                    || ("Flames of Zamorak".equals(spellName) && !wearing("Zamorak staff", "Staff of the dead", "Toxic staff of the dead", "Thammaron's sceptre (a)", "Accursed sceptre (a)"))
                    || ("Magic Dart".equals(spellName) && !wearing("Slayer's staff", "Slayer's staff (e)", "Staff of the dead", "Toxic staff of the dead", "Staff of light", "Staff of balance"))) {
                spell = null;
            }
        }

        spellName = spell == null ? null : spell.getName();
        if (spellName != null) {
            // certain spells can only be cast on specific monsters
            if ((spellName.contains("Demonbane") && !monster.hasAttribute(MonsterAttributeType.DEMON))
                    || ("Crumble Undead".equals(spellName) && !monster.hasAttribute(MonsterAttributeType.UNDEAD))) {
                spell = null;
            }
        }
    }

    private void computeEquipmentTotals() {
        EquipmentItem weapon = player.getWeapon();
        Integer weaponId = weapon == null ? null : weapon.getId();

        for (GearSlot slot : GearSlot.values()) {
            EquipmentItem piece = player.get(slot);
            if (piece == null) continue;

            // skip the ammo slot's ranged bonuses when the ammo is not fired by the weapon
            boolean applyRangedStats = slot != GearSlot.AMMO
                    || RangedAmmo.applicability(weaponId, piece.getId()) == RangedAmmo.AmmoApplicability.INCLUDED;

            totalStr += piece.getBonuses().getStr();
            if (applyRangedStats) {
                totalRangedStr += piece.getBonuses().getRanged_str();
            }
            totalMagicStr += piece.getBonuses().getMagic_str();
            totalPrayerBonus += piece.getBonuses().getPrayer();

            offStab += piece.getOffensive().getStab();
            offSlash += piece.getOffensive().getSlash();
            offCrush += piece.getOffensive().getCrush();
            offMagic += piece.getOffensive().getMagic();
            if (applyRangedStats) {
                offRanged += piece.getOffensive().getRanged();
            }

            defStab += piece.getDefensive().getStab();
            defSlash += piece.getDefensive().getSlash();
            defCrush += piece.getDefensive().getCrush();
            defMagic += piece.getDefensive().getMagic();
            defRanged += piece.getDefensive().getRanged();
        }

        if (weapon != null && BLOWPIPE_IDS.contains(weapon.getId()) && player.getBlowpipeDartId() != null) {
            EquipmentItem dart = data.equipment(player.getBlowpipeDartId());
            if (dart != null) {
                totalRangedStr += dart.getBonuses().getRanged_str();
            }
        }

        if (weapon != null && "Tumeken's shadow".equals(weapon.getName()) && style.getStance() != CombatStance.MANUAL_CAST) {
            int factor = TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId()) ? 4 : 3;
            totalMagicStr = Math.min(1000, totalMagicStr * factor);
            offMagic *= factor;
        }

        if (weapon != null && "Keris partisan of amascut".equals(weapon.getName())
                && !TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId())) {
            totalStr -= 22;
            offStab -= 50;
        }

        if (weapon != null && ("Dinh's bulwark".equals(weapon.getName()) || "Dinh's blazing bulwark".equals(weapon.getName()))) {
            int defenceSum = defStab + defSlash + defCrush + defRanged;
            totalStr += Math.max(0, (defenceSum - 800) / 12 - 38);
        }

        if (spell != null && spell.isAncientSpellbook() && style.getStance() != null && style.getStance().isCastStance()) {
            int virtusPieces = 0;
            for (GearSlot slot : new GearSlot[]{GearSlot.HEAD, GearSlot.BODY, GearSlot.LEGS}) {
                EquipmentItem piece = player.get(slot);
                if (piece != null && piece.getName().contains("Virtus")) virtusPieces++;
            }
            totalMagicStr += 30 * virtusPieces;
        }

        // void mage is a visible 5% magic damage bonus
        if (itemNameIs(GearSlot.HEAD, "Void mage helm")
                && itemNameIs(GearSlot.BODY, "Elite void top")
                && itemNameIs(GearSlot.LEGS, "Elite void robe")
                && itemNameIs(GearSlot.HANDS, "Void knight gloves")) {
            totalMagicStr += 50;
        }

        EquipmentItem cape = player.get(GearSlot.CAPE);
        boolean dizanasQuiverCharged = cape != null
                && ("Dizana's max cape".equals(cape.getName())
                || "Blessed dizana's quiver".equals(cape.getName())
                || ("Dizana's quiver".equals(cape.getName()) && "Charged".equals(cape.getVersion())));
        EquipmentItem ammo = player.get(GearSlot.AMMO);
        if (dizanasQuiverCharged
                && RangedAmmo.applicability(weaponId, ammo == null ? null : ammo.getId()) == RangedAmmo.AmmoApplicability.INCLUDED) {
            offRanged += 10;
            totalRangedStr += 1;
        }
    }

    private boolean itemNameIs(GearSlot slot, String name) {
        EquipmentItem item = player.get(slot);
        return item != null && name.equals(item.getName());
    }

    // ------------------------------------------------------------------
    // Equipment predicates (ported from BaseCalc.ts)
    // ------------------------------------------------------------------

    private boolean wearing(String... names) {
        for (String n : names) {
            if (equippedNames.contains(n)) return true;
        }
        return false;
    }

    private boolean wearingAll(String... names) {
        for (String n : names) {
            if (!equippedNames.contains(n)) return false;
        }
        return true;
    }

    private boolean anyEquippedNameContains(String fragment) {
        for (String n : equippedNames) {
            if (n.contains(fragment)) return true;
        }
        return false;
    }

    private boolean isUsingMeleeStyle() {
        return style.getType() != null && style.getType().isMelee();
    }

    private boolean isWearingVoidRobes() {
        return wearing("Void knight top", "Void knight top (or)", "Elite void top", "Elite void top (or)")
                && wearing("Void knight robe", "Void knight robe (or)", "Elite void robe", "Elite void robe (or)")
                && wearing("Void knight gloves");
    }

    private boolean isWearingEliteVoidRobes() {
        return wearing("Elite void top", "Elite void top (or)")
                && wearing("Elite void robe", "Elite void robe (or)")
                && wearing("Void knight gloves");
    }

    private boolean isWearingMeleeVoid() {
        return isWearingVoidRobes() && wearing("Void melee helm", "Void melee helm (or)");
    }

    private boolean isWearingRangedVoid() {
        return isWearingVoidRobes() && wearing("Void ranger helm", "Void ranger helm (or)");
    }

    private boolean isWearingEliteRangedVoid() {
        return isWearingEliteVoidRobes() && wearing("Void ranger helm", "Void ranger helm (or)");
    }

    private boolean isWearingMagicVoid() {
        return isWearingVoidRobes() && wearing("Void mage helm", "Void mage helm (or)");
    }

    private boolean isWearingEliteMagicVoid() {
        return isWearingEliteVoidRobes() && wearing("Void mage helm", "Void mage helm (or)");
    }

    private boolean isWearingBlackMask() {
        return isWearingImbuedBlackMask() || wearing("Black mask", "Slayer helmet");
    }

    private boolean isWearingImbuedBlackMask() {
        return wearing("Black mask (i)", "Slayer helmet (i)", "V's helm");
    }

    private boolean isWearingSmokeStaff() {
        return wearing("Smoke battlestaff", "Mystic smoke staff", "Twinflame staff");
    }

    private boolean isWearingTzhaarWeapon() {
        return wearing("Tzhaar-ket-em", "Tzhaar-ket-om", "Tzhaar-ket-om (t)", "Toktz-xil-ak", "Toktz-xil-ek", "Toktz-mej-tal");
    }

    private boolean isWearingObsidian() {
        return wearingAll("Obsidian helmet", "Obsidian platelegs", "Obsidian platebody");
    }

    private boolean isWearingBerserkerNecklace() {
        return wearing("Berserker necklace", "Berserker necklace (or)");
    }

    private boolean isWearingCrystalBow() {
        return wearing("Crystal bow") || anyEquippedNameContains("Bow of faerdhinen");
    }

    private boolean isWearingFang() {
        return wearing("Osmumten's fang", "Osmumten's fang (or)");
    }

    private boolean isWearingScythe() {
        return wearing("Scythe of vitur") || anyEquippedNameContains("of vitur");
    }

    private boolean isWearingTwoHitWeapon() {
        return wearing("Torag's hammers", "Sulphur blades", "Glacial temotli", "Earthbound tecpatl");
    }

    private boolean isWearingKeris() {
        return anyEquippedNameContains("Keris");
    }

    private boolean isWearingDharok() {
        return wearingAll("Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe");
    }

    private boolean isWearingVeracs() {
        return wearingAll("Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");
    }

    private boolean isWearingKarils() {
        return wearingAll("Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Karil's crossbow", "Amulet of the damned");
    }

    private boolean isWearingAhrims() {
        return wearingAll("Ahrim's staff", "Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Amulet of the damned");
    }

    private boolean isWearingBloodMoonSet() {
        return wearingAll("Dual macuahuitl", "Blood moon helm", "Blood moon chestplate", "Blood moon tassets");
    }

    private boolean isWearingSilverWeapon() {
        if (style.getType() == CombatStyleType.RANGED && wearing(
                "Silver bolts", "Blisterwood stake")) {
            return true;
        }

        return isUsingMeleeStyle() && wearing(
                "Blessed axe", "Ivandis flail", "Blisterwood flail", "Hallowed flail", "Silver sickle",
                "Silver sickle (b)", "Emerald sickle", "Emerald sickle (b)", "Enchanted emerald sickle (b)",
                "Ruby sickle (b)", "Enchanted ruby sickle (b)", "Blisterwood sickle", "Silverlight",
                "Darklight", "Arclight", "Rod of ivandis", "Wolfbane");
    }

    private boolean wearingVampyrebane(MonsterAttributeType tier) {
        boolean t2 = tier == MonsterAttributeType.VAMPYRE_2;
        EquipmentItem weapon = player.getWeapon();
        if (style.getType() == CombatStyleType.RANGED && weapon != null && "Blisterwood stake".equals(weapon.getName())) {
            return true;
        }
        if (!t2 && !isUsingMeleeStyle()) {
            return false;
        }
        if (t2 && wearing("Rod of ivandis")) {
            return true;
        }
        return wearing("Ivandis flail", "Blisterwood sickle", "Blisterwood flail", "Hallowed flail", "Sunspear");
    }

    private boolean isWearingOgreBow() {
        return wearing("Ogre bow", "Comp ogre bow");
    }

    private boolean isWearingLeafBladedWeapon() {
        if (isUsingMeleeStyle() && wearing("Leaf-bladed battleaxe", "Leaf-bladed spear", "Leaf-bladed sword")) {
            return true;
        }
        if (spell != null && "Magic Dart".equals(spell.getName())) {
            return true;
        }
        return style.getType() == CombatStyleType.RANGED
                && wearing("Broad arrows", "Broad bolts", "Amethyst broad bolts");
    }

    private boolean isWearingCorpbaneWeapon() {
        EquipmentItem weapon = player.getWeapon();
        boolean isStab = style.getType() == CombatStyleType.STAB;
        if (weapon == null) {
            return false;
        }
        if (isWearingFang()) {
            return isStab;
        }
        if (weapon.getName().endsWith("halberd")) {
            return isStab;
        }
        if (weapon.getName().contains("spear") && !"Blue moon spear".equals(weapon.getName())) {
            return isStab;
        }
        return style.getType() == CombatStyleType.MAGIC;
    }

    private boolean isWearingRatBoneWeapon() {
        return wearing("Bone mace", "Bone shortbow", "Bone staff");
    }

    private boolean isRevWeaponBuffApplicable() {
        EquipmentItem weapon = player.getWeapon();
        if (!player.isInWilderness() || weapon == null || !"Charged".equals(weapon.getVersion())) {
            return false;
        }
        if (style.getType() == CombatStyleType.MAGIC) {
            return wearing("Accursed sceptre", "Accursed sceptre (a)", "Thammaron's sceptre", "Thammaron's sceptre (a)");
        }
        if (style.getType() == CombatStyleType.RANGED) {
            return wearing("Craw's bow", "Webweaver bow");
        }
        return wearing("Ursine chainmace", "Viggora's chainmace");
    }

    private boolean isChargeSpellApplicable() {
        if (!player.isChargeSpell() || spell == null) {
            return false;
        }
        switch (spell.getName()) {
            case "Saradomin Strike":
                return wearing("Saradomin cape", "Imbued saradomin cape", "Saradomin max cape", "Imbued saradomin max cape");
            case "Claws of Guthix":
                return wearing("Guthix cape", "Imbued guthix cape", "Guthix max cape", "Imbued guthix max cape");
            case "Flames of Zamorak":
                return wearing("Zamorak cape", "Imbued zamorak cape", "Zamorak max cape", "Imbued zamorak max cape");
            default:
                return false;
        }
    }

    private boolean isUsingDemonbane() {
        if (style.getType() == CombatStyleType.MAGIC) {
            return spell != null && spell.getName().contains("Demonbane");
        }
        if (style.getType() == CombatStyleType.RANGED) {
            return wearing("Scorching bow");
        }
        return wearing("Silverlight", "Darklight", "Arclight", "Emberlight", "Bone claws", "Burning claws");
    }

    private boolean isUsingAbyssal() {
        return isUsingMeleeStyle()
                && wearing("Abyssal bludgeon", "Abyssal dagger", "Abyssal whip", "Abyssal tentacle");
    }

    private boolean isSlayerMonster() {
        return monster.isSlayerMonster() || monster.getId() == -1;
    }

    private boolean isAmmoInvalid() {
        EquipmentItem weapon = player.getWeapon();
        EquipmentItem ammo = player.get(GearSlot.AMMO);
        return RangedAmmo.applicability(
                weapon == null ? null : weapon.getId(),
                ammo == null ? null : ammo.getId()) == RangedAmmo.AmmoApplicability.INVALID;
    }

    private int demonbaneVulnerability() {
        if ("Duke Sucellus".equals(monster.getName())) return 70;
        if (YAMA_IDS.contains(monster.getId())) return 120;
        if (YAMA_VOID_FLARE_IDS.contains(monster.getId())) return 200;
        if (ICE_DEMON_IDS.contains(monster.getId())) return 115;
        return 100;
    }

    /**
     * Computes the demonbane bonus percent for a weapon's base demonbane strength,
     * scaled by the monster's demonbane vulnerability.
     */
    private int demonbaneFactorPercent(int weaponDemonbane) {
        int vulnerability = monster.hasAttribute(MonsterAttributeType.DEMON) ? demonbaneVulnerability() : 100;
        return weaponDemonbane * vulnerability / 100;
    }

    private String getSpellement() {
        return spell == null ? null : spell.getElement();
    }

    private int spellementWeaknessSeverity() {
        String spellement = getSpellement();
        MonsterData.Weakness weakness = monster.getWeakness();
        if (spellement == null || weakness == null || !spellement.equals(weakness.getElement())) {
            return 0;
        }
        return weakness.getSeverity();
    }

    // ------------------------------------------------------------------
    // Accuracy roll formulas (ported from BaseCalc.ts)
    // ------------------------------------------------------------------

    /**
     * Standard OSRS accuracy formula for an attack roll vs a defence roll.
     * @param atk The attacker's max attack roll
     * @param def The defender's max defence roll
     * @return double the chance to hit (0-1)
     */
    public static double getNormalAccuracyRoll(int atk, int def) {
        if (atk < 0) atk = Math.min(0, atk + 2);
        if (def < 0) def = Math.min(0, def + 2);

        if (atk >= 0 && def >= 0) return stdRoll(atk, def);
        if (atk >= 0) return 1 - 1.0 / (-def + 1) / (atk + 1);
        if (def >= 0) return 0;
        return stdRoll(-def, -atk);
    }

    private static double stdRoll(double attack, double defence) {
        return attack > defence
                ? 1 - ((defence + 2) / (2 * (attack + 1)))
                : attack / (2 * (defence + 1));
    }

    /**
     * Osmumten's fang accuracy formula (rolls twice, hit passes if either lands within bounds).
     * @param atk The attacker's max attack roll
     * @param def The defender's max defence roll
     * @return double the chance to hit (0-1)
     */
    public static double getFangAccuracyRoll(int atk, int def) {
        if (atk < 0) atk = Math.min(0, atk + 2);
        if (def < 0) def = Math.min(0, def + 2);
        if (atk >= 0 && def >= 0) return fangStdRoll(atk, def);
        if (atk >= 0) return 1 - 1.0 / (-def + 1) / (atk + 1);
        if (def >= 0) return 0;
        return fangRvRoll(-def, -atk);
    }

    private static double fangStdRoll(double attack, double defence) {
        return attack > defence
                ? 1 - (defence + 2) * (2 * defence + 3) / (attack + 1) / (attack + 1) / 6
                : attack * (4 * attack + 5) / 6 / (attack + 1) / (defence + 1);
    }

    private static double fangRvRoll(double attack, double defence) {
        return attack < defence
                ? attack * (defence * 6 - 2 * attack + 5) / 6 / (defence + 1) / (defence + 1)
                : 1 - (defence + 2) * (2 * defence + 3) / 6 / (defence + 1) / (attack + 1);
    }

    /**
     * Confliction gauntlets accuracy formula.
     * @param atk The attacker's max attack roll
     * @param def The defender's max defence roll
     * @return double the chance to hit (0-1)
     */
    public static double getConflictionGauntletsAccuracyRoll(int atk, int def) {
        double singleRoll = getNormalAccuracyRoll(atk, def);
        double doubleRoll = getFangAccuracyRoll(atk, def);
        return doubleRoll / (1 + doubleRoll - singleRoll);
    }

    // ------------------------------------------------------------------
    // NPC defence roll
    // ------------------------------------------------------------------

    /**
     * Gets the NPC's max defence roll against the player's current combat style.
     * @return int the NPC defence roll
     */
    public int getNPCDefenceRoll() {
        if (defenceRollOverride != null) {
            return defenceRollOverride;
        }

        CombatStyleType defenceStyle = style.getType();

        int level = defenceStyle == CombatStyleType.MAGIC
                && !USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS.contains(monster.getId())
                ? monster.getSkills().getMagic()
                : monster.getSkills().getDef();
        int effectiveLevel = level + 9;

        int bonus;
        if (defenceStyle == CombatStyleType.RANGED) {
            EquipmentItem weapon = player.getWeapon();
            RangedDamageType rangedType = weapon == null ? RangedDamageType.STANDARD
                    : weapon.getWeaponCategory().getRangedDamageType();
            if (rangedType == null) rangedType = RangedDamageType.STANDARD;
            switch (rangedType) {
                case MIXED:
                    bonus = (monster.getDefensive().getLight() + monster.getDefensive().getStandard() + monster.getDefensive().getHeavy()) / 3;
                    break;
                case LIGHT:
                    bonus = monster.getDefensive().getLight();
                    break;
                case HEAVY:
                    bonus = monster.getDefensive().getHeavy();
                    break;
                default:
                    bonus = monster.getDefensive().getStandard();
            }
        } else if (defenceStyle == CombatStyleType.MAGIC) {
            bonus = monster.getDefensive().getMagic();
        } else if (defenceStyle == CombatStyleType.STAB) {
            bonus = monster.getDefensive().getStab();
        } else if (defenceStyle == CombatStyleType.SLASH) {
            bonus = monster.getDefensive().getSlash();
        } else {
            bonus = monster.getDefensive().getCrush();
        }

        int statBonus = bonus + 64;
        return effectiveLevel * statBonus;
    }

    // ------------------------------------------------------------------
    // Prayers
    // ------------------------------------------------------------------

    private List<PrayerBoost> getCombatPrayers() {
        List<PrayerBoost> out = new ArrayList<>();
        if (player.getPrayers() == null) return out;
        for (PrayerBoost p : player.getPrayers()) {
            if (p != null && p.appliesTo(style.getType())) {
                out.add(p);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Melee
    // ------------------------------------------------------------------

    private int getPlayerMaxMeleeAttackRoll() {
        int effectiveLevel = player.getSkills().getAttack() + player.getBoosts().getAttack();

        for (PrayerBoost p : getCombatPrayers()) {
            if (p.getAccuracyFactor() > 0) {
                effectiveLevel = effectiveLevel * p.getAccuracyFactor() / 100;
            }
        }

        int stanceBonus = 8;
        if (style.getStance() == CombatStance.ACCURATE) {
            stanceBonus += 3;
        } else if (style.getStance() == CombatStance.CONTROLLED) {
            stanceBonus += 1;
        }
        effectiveLevel += stanceBonus;

        if (isWearingMeleeVoid()) {
            effectiveLevel = effectiveLevel * 11 / 10;
        }

        int gearBonus = (style.getType() != null ? offensiveTotal(style.getType()) : 0) + 64;
        int baseRoll = effectiveLevel * gearBonus;
        int attackRoll = baseRoll;

        // these bonuses do not stack with each other
        if (wearing("Amulet of avarice") && monster.getName().startsWith("Revenant")) {
            attackRoll = attackRoll * (player.isForinthrySurge() ? 27 : 24) / 20;
        } else if (wearing("Salve amulet (e)", "Salve amulet(ei)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            attackRoll = attackRoll * 6 / 5;
        } else if (wearing("Salve amulet", "Salve amulet(i)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            attackRoll = attackRoll * 7 / 6;
        } else if (isWearingBlackMask() && isSlayerMonster() && player.isOnSlayerTask()) {
            attackRoll = attackRoll * 7 / 6;
        }

        if (isWearingTzhaarWeapon() && isWearingObsidian()) {
            attackRoll += baseRoll / 10;
        }

        if (isRevWeaponBuffApplicable()) {
            attackRoll = attackRoll * 3 / 2;
        }
        boolean demon = monster.hasAttribute(MonsterAttributeType.DEMON);
        if (wearing("Arclight", "Emberlight") && demon) {
            attackRoll += attackRoll * demonbaneFactorPercent(70) / 100;
        }
        if (wearing("Silverlight", "Darklight") && demon) {
            attackRoll += attackRoll * demonbaneFactorPercent(60) / 100;
        }
        if (wearing("Bone claws", "Burning claws") && demon) {
            attackRoll += attackRoll * demonbaneFactorPercent(5) / 100;
        }
        if (monster.hasAttribute(MonsterAttributeType.DRAGON)) {
            if (wearing("Dragon hunter lance")) {
                attackRoll = attackRoll * 6 / 5;
            } else if (wearing("Dragon hunter wand")) {
                attackRoll = attackRoll * 7 / 4;
            }
        }
        if (wearing("Keris partisan of breaching") && monster.hasAttribute(MonsterAttributeType.KALPHITE)) {
            attackRoll = attackRoll * 133 / 100;
        }
        if (wearing("Keris partisan of the sun")
                && TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId())
                && monster.getCurrentHpOrFull() < monster.getSkills().getHp() / 4) {
            attackRoll = attackRoll * 5 / 4;
        }
        if (wearing("Blisterwood flail", "Blisterwood sickle") && monster.isVampyre()) {
            attackRoll = attackRoll * 105 / 100;
        }
        if (wearing("Hallowed flail", "Sunspear") && monster.isVampyre()) {
            attackRoll = attackRoll * 125 / 100;
        }
        if (isWearingSilverWeapon() && wearing("Efaritay's aid") && monster.isVampyre()) {
            attackRoll = attackRoll * 23 / 20;
        }
        if (wearing("Granite hammer") && monster.hasAttribute(MonsterAttributeType.GOLEM)) {
            attackRoll = attackRoll * 13 / 10;
        }

        if (style.getType() == CombatStyleType.CRUSH) {
            int inqBonus = inquisitorPieces();
            if (inqBonus > 0) {
                attackRoll = attackRoll * (200 + inqBonus) / 200;
            }
        }

        return attackRoll;
    }

    private int inquisitorPieces() {
        int bonus = 0;
        if (wearing("Inquisitor's great helm")) bonus += 1;
        if (wearing("Inquisitor's hauberk")) bonus += 2;
        if (wearing("Inquisitor's plateskirt")) bonus += 2;
        return bonus;
    }

    private int[] getPlayerMaxMeleeHit() {
        int baseLevel = player.getSkills().getStrength() + player.getBoosts().getStrength();
        int effectiveLevel = baseLevel;

        for (PrayerBoost p : getCombatPrayers()) {
            if (p.getStrengthFactor() <= 0) continue;
            if (p == PrayerBoost.BURST_OF_STRENGTH && effectiveLevel <= 20) {
                effectiveLevel += 1;
            } else {
                effectiveLevel = effectiveLevel * p.getStrengthFactor() / 100;
            }
        }

        if (wearing("Soulreaper axe") && player.getSoulreaperStacks() > 0) {
            // does not stack multiplicatively with prayers
            int stacks = Math.max(0, Math.min(5, player.getSoulreaperStacks()));
            effectiveLevel += baseLevel * (stacks * 6) / 100;
        }

        int stanceBonus = 8;
        if (style.getStance() == CombatStance.AGGRESSIVE) {
            stanceBonus += 3;
        } else if (style.getStance() == CombatStance.CONTROLLED) {
            stanceBonus += 1;
        }
        effectiveLevel += stanceBonus;

        if (isWearingMeleeVoid()) {
            effectiveLevel = effectiveLevel * 11 / 10;
        }

        int gearBonus = totalStr + 64;
        int baseMax = (effectiveLevel * gearBonus + 320) / 640;
        int minHit = 0;
        int maxHit = baseMax;

        if (wearing("Crystal blessing")) {
            int crystalPieces = (wearing("Crystal helm") ? 1 : 0) + (wearing("Crystal legs") ? 2 : 0) + (wearing("Crystal body") ? 3 : 0);
            maxHit = maxHit * (40 + crystalPieces) / 40;
        }

        boolean demon = monster.hasAttribute(MonsterAttributeType.DEMON);

        if (wearing("Amulet of avarice") && monster.getName().startsWith("Revenant")) {
            maxHit = maxHit * (player.isForinthrySurge() ? 27 : 24) / 20;
        } else if (wearing("Salve amulet (e)", "Salve amulet(ei)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            maxHit = maxHit * 6 / 5;
        } else if (wearing("Salve amulet", "Salve amulet(i)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            maxHit = maxHit * 7 / 6;
        } else if (isWearingBlackMask() && isSlayerMonster() && player.isOnSlayerTask()) {
            maxHit = maxHit * 7 / 6;
        }

        if (wearing("Arclight", "Emberlight") && demon) {
            maxHit += maxHit * demonbaneFactorPercent(70) / 100;
        }
        if (wearing("Bone claws", "Burning claws") && demon) {
            maxHit += maxHit * demonbaneFactorPercent(5) / 100;
        }
        if (isWearingTzhaarWeapon() && isWearingObsidian()) {
            maxHit += baseMax / 10;
        }
        if (wearing("Dragon hunter lance") && monster.hasAttribute(MonsterAttributeType.DRAGON)) {
            maxHit = maxHit * 6 / 5;
        }
        if (wearing("Dragon hunter wand") && monster.hasAttribute(MonsterAttributeType.DRAGON)) {
            maxHit = maxHit * 7 / 5;
        }
        if (isWearingKeris() && monster.hasAttribute(MonsterAttributeType.KALPHITE)) {
            if (wearing("Keris partisan of amascut")) {
                maxHit = maxHit * 115 / 100;
            } else {
                maxHit = maxHit * 133 / 100;
            }
        }
        if (wearing("Barronite mace") && monster.hasAttribute(MonsterAttributeType.GOLEM)) {
            maxHit = maxHit * 23 / 20;
        }
        if (wearing("Granite hammer") && monster.hasAttribute(MonsterAttributeType.GOLEM)) {
            maxHit = maxHit * 13 / 10;
        }
        if (isRevWeaponBuffApplicable()) {
            maxHit = maxHit * 3 / 2;
        }
        if (wearing("Silverlight", "Darklight", "Silverlight (dyed)") && demon) {
            maxHit += maxHit * demonbaneFactorPercent(60) / 100;
        }
        if (wearing("Leaf-bladed battleaxe") && monster.hasAttribute(MonsterAttributeType.LEAFY)) {
            maxHit = maxHit * 47 / 40;
        }
        if (wearing("Colossal blade")) {
            maxHit += Math.min(monster.getSize() * 2, 10);
        }
        if (isWearingRatBoneWeapon() && monster.hasAttribute(MonsterAttributeType.RAT)) {
            // applies before inquisitor
            maxHit += 10;
        }

        if (style.getType() == CombatStyleType.CRUSH) {
            int inqBonus = inquisitorPieces();
            if (inqBonus > 0) {
                maxHit = maxHit * (200 + inqBonus) / 200;
            }
        }

        if (isWearingFang()) {
            int shrink = maxHit * 3 / 20;
            minHit = shrink;
            maxHit -= shrink;
        }

        if ("Respiratory system".equals(monster.getName())) {
            minHit += maxHit / 2;
        }

        return new int[]{minHit, maxHit};
    }

    // ------------------------------------------------------------------
    // Ranged
    // ------------------------------------------------------------------

    private int getPlayerMaxRangedAttackRoll() {
        int effectiveLevel = player.getSkills().getRanged() + player.getBoosts().getRanged();
        for (PrayerBoost p : getCombatPrayers()) {
            if (p.getAccuracyFactor() > 0) {
                effectiveLevel = effectiveLevel * p.getAccuracyFactor() / 100;
            }
        }

        if (style.getStance() == CombatStance.ACCURATE) {
            effectiveLevel += 3;
        }
        effectiveLevel += 8;

        if (isWearingRangedVoid()) {
            effectiveLevel = effectiveLevel * 11 / 10;
        }

        int attackRoll = effectiveLevel * (offRanged + 64);

        if (isWearingCrystalBow()) {
            int crystalPieces = (wearing("Crystal helm") ? 1 : 0) + (wearing("Crystal legs") ? 2 : 0) + (wearing("Crystal body") ? 3 : 0);
            attackRoll = attackRoll * (20 + crystalPieces) / 20;
        }

        if (wearing("Amulet of avarice") && monster.getName().startsWith("Revenant")) {
            attackRoll = attackRoll * (player.isForinthrySurge() ? 27 : 24) / 20;
        } else if (wearing("Salve amulet(ei)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            attackRoll = attackRoll * 6 / 5;
        } else if (wearing("Salve amulet(i)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            attackRoll = attackRoll * 7 / 6;
        } else if (isWearingImbuedBlackMask() && isSlayerMonster() && player.isOnSlayerTask()) {
            attackRoll = attackRoll * 23 / 20;
        }

        if (wearing("Twisted bow")) {
            int cap = monster.hasAttribute(MonsterAttributeType.XERICIAN) ? 350 : 250;
            int tbowMagic = Math.min(cap, Math.max(monster.getSkills().getMagic(), monster.getOffensive().getMagic()));
            attackRoll = tbowScaling(attackRoll, tbowMagic, true);
            if (P2_WARDEN_IDS.contains(monster.getId())) {
                // game update on 2023-06-21 causes this to apply twice at P2 wardens
                attackRoll = tbowScaling(attackRoll, tbowMagic, true);
            }
        }
        if (isRevWeaponBuffApplicable()) {
            attackRoll = attackRoll * 3 / 2;
        }
        if (wearing("Dragon hunter crossbow") && monster.hasAttribute(MonsterAttributeType.DRAGON)) {
            attackRoll = attackRoll * 13 / 10;
        }

        EquipmentItem weapon = player.getWeapon();
        if (weapon != null && weapon.getWeaponCategory() == WeaponCategory.CHINCHOMPA) {
            int distance = Math.min(7, Math.max(1, player.getChinchompaDistance()));
            int numerator = 4;
            if ("Short fuse".equals(style.getName())) {
                if (distance >= 7) {
                    numerator = 2;
                } else if (distance >= 4) {
                    numerator = 3;
                }
            } else if ("Medium fuse".equals(style.getName())) {
                if (distance < 4 || distance >= 7) {
                    numerator = 3;
                }
            } else if ("Long fuse".equals(style.getName())) {
                if (distance < 4) {
                    numerator = 2;
                } else if (distance < 7) {
                    numerator = 3;
                }
            }
            attackRoll = attackRoll * numerator / 4;
        }

        if (wearing("Scorching bow") && monster.hasAttribute(MonsterAttributeType.DEMON)) {
            attackRoll += attackRoll * demonbaneFactorPercent(30) / 100;
        }

        return attackRoll;
    }

    private int[] getPlayerMaxRangedHit() {
        int effectiveLevel = player.getSkills().getRanged() + player.getBoosts().getRanged();
        boolean scalesWithStr = wearing("Eclipse atlatl", "Hunter's spear");
        if (scalesWithStr) {
            // atlatl uses strength level, melee strength bonus, and melee slayer/salve bonuses, but ranged void
            effectiveLevel = player.getSkills().getStrength() + player.getBoosts().getStrength();
        }

        if (wearing("Holy water")) {
            if (!monster.hasAttribute(MonsterAttributeType.DEMON)) {
                return new int[]{0, 0};
            }
            effectiveLevel += 10;
            EquipmentItem weapon = player.getWeapon();
            int str = 64 + (weapon != null ? weapon.getBonuses().getRanged_str() : 0);
            int maxHit = (effectiveLevel * str + 320) / 640;
            maxHit += maxHit * demonbaneFactorPercent(60) / 100;
            if ("Nezikchened".equals(monster.getName())) {
                maxHit += 5;
            }
            return new int[]{0, maxHit};
        }

        if (isWearingOgreBow()) {
            effectiveLevel += 10;
            EquipmentItem ammo = player.get(GearSlot.AMMO);
            int bonusStr = ammo != null ? ammo.getBonuses().getRanged_str() : 0;
            int maxHit = (effectiveLevel * (bonusStr + 64) + 320) / 640;
            // ogre bows ignore all other gear and bonuses
            return new int[]{0, maxHit};
        }

        for (PrayerBoost p : getCombatPrayers()) {
            if (p.getStrengthFactor() <= 0) continue;
            if (p == PrayerBoost.SHARP_EYE && effectiveLevel <= 20) {
                effectiveLevel += 1;
            } else {
                effectiveLevel = effectiveLevel * p.getStrengthFactor() / 100;
            }
        }

        if (style.getStance() == CombatStance.ACCURATE) {
            effectiveLevel += 3;
        }
        effectiveLevel += 8;

        if (isWearingEliteRangedVoid()) {
            effectiveLevel = effectiveLevel * 9 / 8;
        } else if (isWearingRangedVoid()) {
            effectiveLevel = effectiveLevel * 11 / 10;
        }

        int bonusStr = scalesWithStr ? totalStr : totalRangedStr;
        int baseMax = (effectiveLevel * (64 + bonusStr) + 320) / 640;
        int minHit = 0;
        int maxHit = baseMax;

        // crystal armour bonus applies before slayer helm (tested in-game)
        if (isWearingCrystalBow()) {
            int crystalPieces = (wearing("Crystal helm") ? 1 : 0) + (wearing("Crystal legs") ? 2 : 0) + (wearing("Crystal body") ? 3 : 0);
            maxHit = maxHit * (40 + crystalPieces) / 40;
        }

        boolean undead = monster.hasAttribute(MonsterAttributeType.UNDEAD);
        boolean needRevWeaponBonus = isRevWeaponBuffApplicable();
        boolean needDragonbane = wearing("Dragon hunter crossbow") && monster.hasAttribute(MonsterAttributeType.DRAGON);
        boolean needDemonbane = wearing("Scorching bow") && monster.hasAttribute(MonsterAttributeType.DEMON);

        if (wearing("Amulet of avarice") && monster.getName().startsWith("Revenant")) {
            maxHit = maxHit * (player.isForinthrySurge() ? 27 : 24) / 20;
        } else if ((wearing("Salve amulet(ei)") || (scalesWithStr && wearing("Salve amulet (e)"))) && undead) {
            maxHit = maxHit * 6 / 5;
        } else if ((wearing("Salve amulet(i)") || (scalesWithStr && wearing("Salve amulet"))) && undead) {
            maxHit = maxHit * 7 / 6;
        } else if (scalesWithStr && isWearingBlackMask() && isSlayerMonster() && player.isOnSlayerTask()) {
            maxHit = maxHit * 7 / 6;
        } else if (isWearingImbuedBlackMask() && isSlayerMonster() && player.isOnSlayerTask()) {
            int numerator = 23;
            // these are additive with the slayer bonus only
            if (needRevWeaponBonus) {
                needRevWeaponBonus = false;
                numerator += 10;
            }
            if (needDragonbane) {
                needDragonbane = false;
                numerator += 5;
            }
            if (needDemonbane) {
                needDemonbane = false;
                numerator += 6;
            }
            maxHit = maxHit * numerator / 20;
        }

        if (wearing("Twisted bow")) {
            int cap = monster.hasAttribute(MonsterAttributeType.XERICIAN) ? 350 : 250;
            int tbowMagic = Math.min(cap, Math.max(monster.getSkills().getMagic(), monster.getOffensive().getMagic()));
            maxHit = tbowScaling(maxHit, tbowMagic, false);
        }

        // multiplicative when not combined with the slayer helm
        if (needRevWeaponBonus) {
            maxHit = maxHit * 3 / 2;
        }
        if (needDragonbane) {
            maxHit = maxHit * 5 / 4;
        }
        if (needDemonbane) {
            maxHit += maxHit * demonbaneFactorPercent(30) / 100;
        }

        if (isWearingRatBoneWeapon() && monster.hasAttribute(MonsterAttributeType.RAT)) {
            maxHit += 10;
        }

        if (wearing("Tonalztics of ralos")) {
            // rolls 75% of max hit but can hit twice (second hit handled in the distribution)
            maxHit = maxHit * 3 / 4;
        }

        if (P2_WARDEN_IDS.contains(monster.getId())) {
            int[] minMax = applyP2WardensDamageModifier(maxHit);
            minHit = minMax[0];
            maxHit = minMax[1];
        }

        if ("Respiratory system".equals(monster.getName())) {
            minHit += maxHit / 2;
        }

        return new int[]{minHit, maxHit};
    }

    private static int tbowScaling(int current, int magic, boolean accuracyMode) {
        int factor = accuracyMode ? 10 : 14;
        int base = accuracyMode ? 140 : 250;
        int clamp = accuracyMode ? 140 : 250;

        int t2 = (3 * magic - factor) / 100;
        int t3sqrt = 3 * magic / 10 - 10 * factor;
        int t3 = t3sqrt * t3sqrt / 100;
        int bonus = Math.max(0, Math.min(clamp, base + t2 - t3));
        return (int) ((long) current * bonus / 100);
    }

    private int[] applyP2WardensDamageModifier(int max) {
        // 1/3 of enemy defence is removed from accuracy
        int reducedNpcDefence = getNPCDefenceRoll() / 3;
        int accuracyDelta = Math.max(getMaxAttackRoll() - reducedNpcDefence, 0);

        // remaining accuracy provides a % dmg modifier from 15% - 40% based on lerp from 0 to 42k
        int lerp = 15 + (40 - 15) * (accuracyDelta - 0) / (42000 - 0);
        int modifier = Math.max(Math.min(lerp, 40), 15);

        int maxPctRange = 20;
        return new int[]{max * modifier / 100, max * (modifier + maxPctRange) / 100};
    }

    // ------------------------------------------------------------------
    // Magic
    // ------------------------------------------------------------------

    private int getPlayerMaxMagicAttackRoll() {
        int effectiveLevel = player.getSkills().getMagic() + player.getBoosts().getMagic();
        for (PrayerBoost p : getCombatPrayers()) {
            if (p.getAccuracyFactor() > 0) {
                effectiveLevel = effectiveLevel * p.getAccuracyFactor() / 100;
            }
        }

        if (style.getStance() == CombatStance.ACCURATE) {
            effectiveLevel += 2;
        }
        effectiveLevel += 9;

        if (isWearingMagicVoid()) {
            effectiveLevel = effectiveLevel * 29 / 20;
        }

        int baseRoll = effectiveLevel * (offMagic + 64);
        int attackRoll = baseRoll;

        int additiveBonus = 0;
        boolean blackMaskBonus = false;
        if (wearing("Amulet of avarice") && monster.getName().startsWith("Revenant")) {
            additiveBonus += player.isForinthrySurge() ? 35 : 20;
        } else if (wearing("Salve amulet(ei)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            additiveBonus += 20;
        } else if (wearing("Salve amulet(i)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            additiveBonus += 15;
        } else if (isWearingImbuedBlackMask() && isSlayerMonster() && player.isOnSlayerTask()) {
            blackMaskBonus = true;
        }

        if (wearing("Efaritay's aid") && monster.isVampyre() && isWearingSilverWeapon()) {
            additiveBonus += 15;
        }

        if (isWearingSmokeStaff() && spell != null && spell.isStandardSpellbook()) {
            additiveBonus += 10;
        }

        if (additiveBonus != 0) {
            attackRoll = attackRoll * (100 + additiveBonus) / 100;
        }

        if (monster.hasAttribute(MonsterAttributeType.DRAGON)) {
            // these still apply when autocasting from the dragon hunter weapons
            if (wearing("Dragon hunter crossbow")) {
                attackRoll = attackRoll * 13 / 10;
            } else if (wearing("Dragon hunter lance")) {
                attackRoll = attackRoll * 6 / 5;
            } else if (wearing("Dragon hunter wand")) {
                attackRoll = attackRoll * 7 / 4;
            }
        }

        if (blackMaskBonus) {
            attackRoll = attackRoll * 23 / 20;
        }

        if (spell != null && spell.getName().contains("Demonbane") && monster.hasAttribute(MonsterAttributeType.DEMON)) {
            int demonbanePercent = player.isMarkOfDarknessSpell() ? 40 : 20;
            if (wearing("Purging staff")) {
                demonbanePercent *= 2;
            }
            attackRoll += attackRoll * demonbaneFactorPercent(demonbanePercent) / 100;
        }
        if (isRevWeaponBuffApplicable()) {
            attackRoll = attackRoll * 3 / 2;
        }
        if (wearing("Tome of water") && ("water".equals(getSpellement()) || (spell != null && spell.isBindSpell()))) {
            attackRoll = attackRoll * 6 / 5;
        }

        int severity = spellementWeaknessSeverity();
        if (severity > 0) {
            attackRoll += baseRoll * severity / 100;
        }

        return attackRoll;
    }

    private int[] getPlayerMaxMagicHit() {
        int minHit = 0;
        int maxHit = 0;
        int magicLevel = player.getSkills().getMagic() + player.getBoosts().getMagic();

        if (spell != null) {
            maxHit = data.spellMaxHit(spell, magicLevel);
            if ("Magic Dart".equals(spell.getName())) {
                if (wearing("Slayer's staff (e)") && isSlayerMonster() && player.isOnSlayerTask()) {
                    maxHit = 13 + magicLevel / 6;
                } else {
                    maxHit = 10 + magicLevel / 10;
                }
            }
        } else if (wearing("Starter staff")) {
            maxHit = 8;
        } else if (wearing("Trident of the seas", "Trident of the seas (e)")) {
            maxHit = Math.max(1, magicLevel / 3 - 5);
        } else if (wearing("Thammaron's sceptre")) {
            maxHit = Math.max(1, magicLevel / 3 - 8);
        } else if (wearing("Accursed sceptre")) {
            maxHit = Math.max(1, magicLevel / 3 - 6);
        } else if (wearing("Trident of the swamp", "Trident of the swamp (e)")) {
            maxHit = Math.max(1, magicLevel / 3 - 2);
        } else if (wearing("Sanguinesti staff", "Holy sanguinesti staff")) {
            maxHit = Math.max(1, magicLevel / 3);
        } else if (wearing("Dawnbringer")) {
            maxHit = Math.max(1, magicLevel / 6 - 1);
        } else if (wearing("Tumeken's shadow")) {
            maxHit = Math.max(1, magicLevel / 3 + 1);
        } else if (wearing("Eye of ayak")) {
            maxHit = Math.max(1, magicLevel / 3 - 6);
        } else if (wearing("Warped sceptre")) {
            maxHit = Math.max(1, (8 * magicLevel + 96) / 37);
        } else if (wearing("Bone staff")) {
            maxHit = Math.max(1, magicLevel / 3 - 5) + 10;
        } else if (wearing("Crystal staff (basic)", "Corrupted staff (basic)")) {
            maxHit = 23;
        } else if (wearing("Crystal staff (attuned)", "Corrupted staff (attuned)")) {
            maxHit = 31;
        } else if (wearing("Crystal staff (perfected)", "Corrupted staff (perfected)")) {
            maxHit = 39;
        } else if (wearing("Swamp lizard")) {
            maxHit = (magicLevel * (56 + 64) + 320) / 640;
        } else if (wearing("Orange salamander")) {
            maxHit = (magicLevel * (59 + 64) + 320) / 640;
        } else if (wearing("Red salamander")) {
            maxHit = (magicLevel * (77 + 64) + 320) / 640;
        } else if (wearing("Black salamander")) {
            maxHit = (magicLevel * (92 + 64) + 320) / 640;
        } else if (wearing("Tecu salamander")) {
            maxHit = (magicLevel * (104 + 64) + 320) / 640;
        }

        if (maxHit == 0) {
            // either a 0-damage spell or a staff-casting option without a spell selected
            return new int[]{0, 0};
        }

        if (wearing("Chaos gauntlets") && spell != null && spell.getName().toLowerCase().contains("bolt")) {
            maxHit += 3;
        }
        if (isChargeSpellApplicable()) {
            maxHit += 10;
        }

        int baseMax = maxHit;
        int magicDmgBonus = totalMagicStr;

        if (isWearingSmokeStaff() && spell != null && spell.isStandardSpellbook()) {
            magicDmgBonus += 100;
        }

        boolean blackMaskBonus = false;
        if (wearing("Salve amulet(ei)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            magicDmgBonus += 200;
        } else if (wearing("Salve amulet(i)") && monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            magicDmgBonus += 150;
        } else if (wearing("Amulet of avarice") && monster.getName().startsWith("Revenant")) {
            magicDmgBonus += player.isForinthrySurge() ? 350 : 200;
        } else if (isWearingImbuedBlackMask() && isSlayerMonster() && player.isOnSlayerTask()) {
            blackMaskBonus = true;
        }

        for (PrayerBoost p : getCombatPrayers()) {
            magicDmgBonus += p.getMagicDamageBonus();
        }

        maxHit += maxHit * magicDmgBonus / 1000;

        if (blackMaskBonus) {
            maxHit = maxHit * 23 / 20;
        }

        if (monster.hasAttribute(MonsterAttributeType.DRAGON)) {
            if (wearing("Dragon hunter lance")) {
                maxHit = maxHit * 6 / 5;
            } else if (wearing("Dragon hunter wand")) {
                maxHit = maxHit * 7 / 5;
            } else if (wearing("Dragon hunter crossbow")) {
                maxHit = maxHit * 5 / 4;
            }
        }

        if (isRevWeaponBuffApplicable()) {
            maxHit = maxHit * 3 / 2;
        }

        int severity = spellementWeaknessSeverity();
        if (severity > 0) {
            maxHit += (int) (baseMax * (severity / 100.0));
        }

        if (player.isUsingSunfireRunes() && spell != null && spell.canUseSunfireRunes()) {
            // sunfire minimum applies pre-tome
            minHit = maxHit / 10;
        }

        EquipmentItem shield = player.get(GearSlot.SHIELD);
        boolean shieldCharged = shield != null && "Charged".equals(shield.getVersion());
        if ((wearing("Tome of fire") && shieldCharged && "fire".equals(getSpellement()))
                || (wearing("Tome of water") && shieldCharged && "water".equals(getSpellement()))
                || (wearing("Tome of earth") && shieldCharged && "earth".equals(getSpellement()))) {
            maxHit = maxHit * 11 / 10;
        }

        if (P2_WARDEN_IDS.contains(monster.getId())) {
            int[] minMax = applyP2WardensDamageModifier(maxHit);
            minHit = minMax[0];
            maxHit = minMax[1];
        }

        if ("Respiratory system".equals(monster.getName())) {
            minHit += maxHit / 2;
        }

        return new int[]{minHit, maxHit};
    }

    // ------------------------------------------------------------------
    // Aggregation
    // ------------------------------------------------------------------

    private int offensiveTotal(CombatStyleType type) {
        switch (type) {
            case STAB: return offStab;
            case SLASH: return offSlash;
            case CRUSH: return offCrush;
            case MAGIC: return offMagic;
            case RANGED: return offRanged;
            default: return 0;
        }
    }

    /**
     * Gets the player's min and max hit for the current style, before distribution effects.
     * @return int[] of {minHit, maxHit}
     */
    public int[] getMinAndMax() {
        if (style.getStance() != CombatStance.MANUAL_CAST && isAmmoInvalid()) {
            return new int[]{0, 0};
        }

        int[] minMax = {0, 0};
        if (isUsingMeleeStyle()) {
            minMax = getPlayerMaxMeleeHit();
        } else if (style.getType() == CombatStyleType.RANGED) {
            minMax = getPlayerMaxRangedHit();
        } else if (style.getType() == CombatStyleType.MAGIC) {
            minMax = getPlayerMaxMagicHit();
        }

        if (minMax[0] > minMax[1]) {
            minMax[1] = minMax[0];
        }
        if (minMax[0] < 0) minMax[0] = 0;
        if (minMax[1] < 0) minMax[1] = 0;
        return minMax;
    }

    /**
     * Gets the player's max attack roll for the current combat style.
     * @return int the max attack roll
     */
    public int getMaxAttackRoll() {
        if (style.getStance() != CombatStance.MANUAL_CAST && isAmmoInvalid()) {
            return 0;
        }

        if (isUsingMeleeStyle()) {
            return getPlayerMaxMeleeAttackRoll();
        }
        if (style.getType() == CombatStyleType.RANGED) {
            return getPlayerMaxRangedAttackRoll();
        }
        if (style.getType() == CombatStyleType.MAGIC) {
            return getPlayerMaxMagicAttackRoll();
        }
        return 0;
    }

    /**
     * Gets the chance (0-1) for the player's attack to land.
     * @return double the hit chance
     */
    public double getHitChance() {
        if (memoizedHitChance != null) {
            return memoizedHitChance;
        }
        memoizedHitChance = computeHitChance();
        return memoizedHitChance;
    }

    private double computeHitChance() {
        int monsterId = monster.getId();

        if (GUARANTEED_ACCURACY_MONSTERS.contains(monsterId)) {
            return 1.0;
        }
        if (VERZIK_P1_IDS.contains(monsterId) && wearing("Dawnbringer")) {
            return 1.0;
        }
        if (P2_WARDEN_IDS.contains(monsterId)) {
            return 1.0;
        }
        // Giant rat (Scurrius)
        if (monsterId == 7223 && style.getStance() != CombatStance.MANUAL_CAST) {
            return 1.0;
        }
        // Royal Titans elementals
        if (TITAN_ELEMENTAL_IDS.contains(monsterId) && style.getType() == CombatStyleType.MAGIC) {
            double accuracy = Math.min(1.0, Math.max(0, offMagic) / 100.0 + 0.3);
            if (isWearingEliteMagicVoid() || isWearingMagicVoid()) {
                accuracy = Math.min(1.0, accuracy * 1.45);
            }
            return accuracy;
        }
        // Eclipse Moon clone phase
        if (ECLIPSE_MOON_IDS.contains(monsterId) && "Clone".equals(monster.getVersion()) && isUsingMeleeStyle()) {
            return 1.0;
        }
        if (style.getType() == CombatStyleType.MAGIC && ALWAYS_MAX_HIT_MAGIC.contains(monsterId)) {
            return 1.0;
        }
        if (style.getType() == CombatStyleType.RANGED && ALWAYS_MAX_HIT_RANGED.contains(monsterId)) {
            return 1.0;
        }
        if (isUsingMeleeStyle() && ALWAYS_MAX_HIT_MELEE.contains(monsterId)) {
            return 1.0;
        }

        int atk = getMaxAttackRoll();
        int def = getNPCDefenceRoll();

        double hitChance = getNormalAccuracyRoll(atk, def);

        if (isWearingFang() && style.getType() == CombatStyleType.STAB) {
            if (TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monsterId)) {
                hitChance = 1 - Math.pow(1 - hitChance, 2);
            } else {
                hitChance = getFangAccuracyRoll(atk, def);
            }
        }

        EquipmentItem weapon = player.getWeapon();
        if (wearing("Confliction gauntlets") && style.getType() == CombatStyleType.MAGIC
                && (weapon == null || !weapon.isTwoHanded())) {
            hitChance = getConflictionGauntletsAccuracyRoll(atk, def);
        }

        return hitChance;
    }

    // ------------------------------------------------------------------
    // Distribution
    // ------------------------------------------------------------------

    /**
     * Gets the full damage distribution for one attack, including gear effects and
     * NPC-side damage modifiers.
     * @return AttackDistribution the attack's damage distribution
     */
    public AttackDistribution getDistribution() {
        if (memoizedDist == null) {
            AttackDistribution attackerDist = getAttackerDist();
            memoizedDist = attackerDist.transform(buildNpcTransformer(style.getType()));
        }
        return memoizedDist;
    }

    private AttackDistribution getAttackerDist() {
        double acc = getHitChance();
        int[] minMax = getMinAndMax();
        int min = minMax[0];
        int max = minMax[1];

        if (max == 0) {
            return AttackDistribution.of(HitDistribution.single(1.0, Hitsplat.INACCURATE));
        }

        HitDistribution standardHitDist = HitDistribution.linear(acc, min, max);
        AttackDistribution dist = AttackDistribution.of(standardHitDist);

        // monsters that always die in one hit
        if (ONE_HIT_MONSTERS.contains(monster.getId())) {
            return AttackDistribution.of(HitDistribution.single(1.0, new Hitsplat(monster.getSkills().getHp())));
        }

        if ("Respiratory system".equals(monster.getName()) && isUsingDemonbane()) {
            return AttackDistribution.of(HitDistribution.single(acc, new Hitsplat(monster.getSkills().getHp())));
        }

        EquipmentItem weapon = player.getWeapon();
        if (style.getType() == CombatStyleType.RANGED && wearing("Tonalztics of ralos")
                && weapon != null && "Charged".equals(weapon.getVersion())) {
            // rolls two independent hits
            dist = AttackDistribution.of(standardHitDist, standardHitDist);
        }

        if (isUsingMeleeStyle() && wearing("Gadderhammer") && monster.hasAttribute(MonsterAttributeType.SHADE)) {
            HitDistribution mixed = new HitDistribution();
            mixed.addHits(standardHitDist.scaleProbability(0.95).scaleDamage(5, 4).getHits());
            mixed.addHits(standardHitDist.scaleProbability(0.05).scaleDamage(2, 1).getHits());
            dist = AttackDistribution.of(mixed);
        }

        if (style.getType() == CombatStyleType.RANGED && wearing("Dark bow")) {
            dist = AttackDistribution.of(standardHitDist, standardHitDist);
        }

        boolean accurateZeroApplicable = true;

        if (isUsingMeleeStyle() && isWearingVeracs()) {
            HitDistribution verac = new HitDistribution();
            verac.addHits(standardHitDist.scaleProbability(0.75).getHits());
            verac.addHits(HitDistribution.linear(1.0, 1, max + 1).scaleProbability(0.25).getHits());
            dist = AttackDistribution.of(verac);
        }

        if (style.getType() == CombatStyleType.RANGED && isWearingKarils()) {
            // 25% chance to deal a second hitsplat at half the damage of the first
            dist = dist.transform(h -> {
                HitDistribution d = new HitDistribution();
                d.addHit(new WeightedHit(0.75, WeightedHit.singleton(h)));
                List<Hitsplat> pair = new ArrayList<>(2);
                pair.add(h);
                pair.add(new Hitsplat(h.getDamage() / 2));
                d.addHit(new WeightedHit(0.25, pair));
                return d;
            }, false);
        }

        if (isUsingMeleeStyle() && isWearingScythe()) {
            List<HitDistribution> hits = new ArrayList<>();
            for (int i = 0; i < Math.min(Math.max(monster.getSize(), 1), 3); i++) {
                int splatMax = max / (1 << i);
                hits.add(HitDistribution.linear(acc, min, Math.max(min, splatMax)));
            }
            dist = new AttackDistribution(hits);
        }

        if (isUsingMeleeStyle() && wearing("Dual macuahuitl")) {
            int firstMax = max / 2;
            int secondMax = max - firstMax;
            HitDistribution secondHit = HitDistribution.linear(acc, min, Math.max(min, secondMax));
            AttackDistribution firstHit = AttackDistribution.of(HitDistribution.linear(acc, min, Math.max(min, firstMax)));
            dist = firstHit.transform(h -> {
                if (h.isAccurate()) {
                    return HitDistribution.single(1.0, h).zip(secondHit);
                }
                List<Hitsplat> pair = new ArrayList<>(2);
                pair.add(h);
                pair.add(Hitsplat.INACCURATE);
                return new HitDistribution(new ArrayList<>(List.of(new WeightedHit(1.0, pair))));
            });
        }

        if (isUsingMeleeStyle() && isWearingTwoHitWeapon()) {
            int firstMax = max / 2;
            int secondMax = max - firstMax;
            dist = AttackDistribution.of(
                    HitDistribution.linear(acc, min, Math.max(min, firstMax)),
                    HitDistribution.linear(acc, min, Math.max(min, secondMax)));
        }

        if (isUsingMeleeStyle() && isWearingKeris() && monster.hasAttribute(MonsterAttributeType.KALPHITE)) {
            HitDistribution keris = new HitDistribution();
            keris.addHits(standardHitDist.scaleProbability(50.0 / 51.0).getHits());
            keris.addHits(standardHitDist.scaleProbability(1.0 / 51.0).scaleDamage(3, 1).getHits());
            dist = AttackDistribution.of(keris);
        }

        if (isUsingMeleeStyle() && GUARDIAN_IDS.contains(monster.getId())
                && weapon != null && weapon.getWeaponCategory() == WeaponCategory.PICKAXE) {
            int pickBonus = pickaxeGuardianBonus(weapon.getName());
            int factor = 50 + player.getSkills().getMining() + pickBonus;
            dist = dist.transform(Transforms.multiply(factor, 150));
        }

        if (wearing("Sanguinesti staff", "Holy sanguinesti staff")) {
            dist = dist.transform(h -> {
                HitDistribution d = new HitDistribution();
                d.addHit(new WeightedHit(0.8, WeightedHit.singleton(h)));
                d.addHit(new WeightedHit(0.2, WeightedHit.singleton(new Hitsplat(h.getDamage() + 8, h.isAccurate()))));
                return d;
            }, false);
        }

        if (player.isMarkOfDarknessSpell() && spell != null && spell.getName().contains("Demonbane")
                && monster.hasAttribute(MonsterAttributeType.DEMON)) {
            int demonbaneFactor = wearing("Purging staff") ? 50 : 25;
            int vulnerability = demonbaneVulnerability();
            dist = dist.transform(h -> HitDistribution.single(1.0,
                    new Hitsplat(h.getDamage() + (h.getDamage() * demonbaneFactor / 100) * vulnerability / 100, h.isAccurate())));
        }

        if (style.getType() == CombatStyleType.MAGIC && isWearingAhrims()) {
            dist = dist.transform(h -> {
                HitDistribution d = new HitDistribution();
                d.addHit(new WeightedHit(0.75, WeightedHit.singleton(h)));
                d.addHit(new WeightedHit(0.25, WeightedHit.singleton(new Hitsplat(h.getDamage() * 13 / 10, h.isAccurate()))));
                return d;
            });
        }

        if (isUsingMeleeStyle() && isWearingDharok()) {
            int newMax = player.getSkills().getHitpoints();
            int curr = player.getSkills().getHitpoints() + player.getBoosts().getHitpoints();
            dist = dist.scaleDamage(10000 + (newMax - curr) * newMax, 10000);
        }

        if (isUsingMeleeStyle() && isWearingBerserkerNecklace() && isWearingTzhaarWeapon()) {
            dist = dist.scaleDamage(6, 5);
        }

        // vampyre damage bonuses (tested extensively by the wiki team)
        if (monster.isVampyre()) {
            boolean efaritay = wearing("Efaritay's aid");
            if (wearing("Blisterwood flail", "Hallowed flail", "Blisterwood stake")) {
                if (efaritay) dist = dist.scaleDamage(11, 10);
                dist = dist.scaleDamage(125, 100);
            } else if (wearing("Sunspear")) {
                if (efaritay) dist = dist.scaleDamage(11, 10);
                dist = dist.scaleDamage(150, 100);
            } else if (wearing("Blisterwood sickle")) {
                if (efaritay) dist = dist.scaleDamage(11, 10);
                dist = dist.scaleDamage(23, 20);
            } else if (wearing("Ivandis flail")) {
                if (efaritay) dist = dist.scaleDamage(11, 10);
                dist = dist.scaleDamage(6, 5);
            } else if (wearing("Rod of ivandis") && !monster.hasAttribute(MonsterAttributeType.VAMPYRE_3)) {
                if (efaritay) dist = dist.scaleDamage(11, 10);
                dist = dist.scaleDamage(11, 10);
            } else if (isWearingSilverWeapon() && monster.hasAttribute(MonsterAttributeType.VAMPYRE_1)) {
                if (efaritay) dist = dist.scaleDamage(11, 10);
                dist = dist.scaleDamage(11, 10);
            }
        }

        // enchanted bolt effects
        int rangedLvl = player.getSkills().getRanged() + player.getBoosts().getRanged();
        BoltEffects.BoltContext boltContext = new BoltEffects.BoltContext(
                rangedLvl, max, wearing("Zaryte crossbow"), player.isKandarinDiary(), monster);
        boolean usingCrossbow = style.getType() == CombatStyleType.RANGED
                && weapon != null && weapon.getWeaponCategory() == WeaponCategory.CROSSBOW;
        if (usingCrossbow) {
            if (wearing("Opal bolts (e)", "Opal dragon bolts (e)")) {
                dist = dist.transform(BoltEffects.opalBolts(boltContext));
            } else if (wearing("Pearl bolts (e)", "Pearl dragon bolts (e)")) {
                dist = dist.transform(BoltEffects.pearlBolts(boltContext));
            } else if (wearing("Diamond bolts (e)", "Diamond dragon bolts (e)")) {
                dist = dist.transform(BoltEffects.diamondBolts(boltContext));
            } else if (wearing("Dragonstone bolts (e)", "Dragonstone dragon bolts (e)")) {
                dist = dist.transform(BoltEffects.dragonstoneBolts(boltContext));
            } else if (wearing("Onyx bolts (e)", "Onyx dragon bolts (e)") && !monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
                dist = dist.transform(BoltEffects.onyxBolts(boltContext));
            }
        }

        if (spell != null && spell.getMaxHit() == 0) {
            // don't raise 0s for spells like Bind
            accurateZeroApplicable = false;
        }

        EquipmentItem ammoItem = player.get(GearSlot.AMMO);
        if (style.getType() == CombatStyleType.RANGED
                && ammoItem != null && ammoItem.getName().contains("Seeking")
                && RangedAmmo.applicability(weapon == null ? null : weapon.getId(), ammoItem.getId()) == RangedAmmo.AmmoApplicability.INCLUDED) {
            dist = dist.transform(h -> HitDistribution.single(1.0,
                    new Hitsplat(Math.max(h.getDamage(), 3), h.isAccurate())), false);
        }

        // raise accurate 0s to 1
        if (accurateZeroApplicable) {
            dist = dist.transform(h -> HitDistribution.single(1.0,
                    new Hitsplat(Math.max(h.getDamage(), 1))), false);
        }

        if (style.getType() == CombatStyleType.MAGIC && spell != null && spell.isStandardSpellbook()) {
            String name = spell.getName();
            boolean twinflameCompat = name.contains("Bolt") || name.contains("Blast") || name.contains("Wave");
            if (wearing("Twinflame staff") && twinflameCompat) {
                dist = dist.transform(h -> {
                    List<Hitsplat> pair = new ArrayList<>(2);
                    pair.add(new Hitsplat(h.getDamage()));
                    pair.add(new Hitsplat(h.getDamage() * 4 / 10));
                    HitDistribution d = new HitDistribution();
                    d.addHit(new WeightedHit(1.0, pair));
                    return d;
                });
            }
        }

        // corp is applied earlier than other limiters and rubies later than other bolts,
        // since corp takes full ruby bolt effect damage but reduced damage otherwise
        if ("Corporeal Beast".equals(monster.getName()) && !isWearingCorpbaneWeapon()) {
            dist = dist.transform(Transforms.division(2));
        }

        if (usingCrossbow) {
            int currentHp = player.getSkills().getHitpoints() + player.getBoosts().getHitpoints();
            if (wearing("Ruby bolts (e)", "Ruby dragon bolts (e)") && currentHp >= 10) {
                dist = dist.transform(BoltEffects.rubyBolts(boltContext));
            }
        }

        if (style.getType() == CombatStyleType.MAGIC && wearing("Brimstone ring") && defenceRollOverride == null) {
            double effectChance = 0.25;
            int effectDef = getNPCDefenceRoll() * 9 / 10;
            AttackDistribution effectDist = subCalcWithDefenceRoll(effectDef).getAttackerDist();

            List<HitDistribution> zipped = new ArrayList<>();
            for (int i = 0; i < dist.getDists().size(); i++) {
                HitDistribution combined = new HitDistribution();
                combined.addHits(dist.getDists().get(i).scaleProbability(1 - effectChance).getHits());
                combined.addHits(effectDist.getDists().get(i).scaleProbability(effectChance).getHits());
                zipped.add(combined);
            }
            dist = new AttackDistribution(zipped).flatten();
        }

        // monsters that are always max hit no matter what
        if ((style.getType() == CombatStyleType.MAGIC && ALWAYS_MAX_HIT_MAGIC.contains(monster.getId()))
                || (isUsingMeleeStyle() && ALWAYS_MAX_HIT_MELEE.contains(monster.getId()))
                || (style.getType() == CombatStyleType.RANGED && ALWAYS_MAX_HIT_RANGED.contains(monster.getId()))) {
            if (YAMA_VOID_FLARE_IDS.contains(monster.getId()) && player.isMarkOfDarknessSpell()
                    && spell != null && spell.getName().contains("Demonbane")) {
                int demonbaneFactor = wearing("Purging staff") ? 50 : 25;
                int bonus = (max * demonbaneFactor / 100) * demonbaneVulnerability() / 100;
                return AttackDistribution.of(HitDistribution.single(1.0, new Hitsplat(max + bonus)));
            }
            return AttackDistribution.of(HitDistribution.single(1.0, new Hitsplat(dist.getMax())));
        }

        return dist;
    }

    private static int pickaxeGuardianBonus(String pickName) {
        switch (pickName) {
            case "Bronze pickaxe":
            case "Iron pickaxe":
                return 1;
            case "Steel pickaxe":
                return 6;
            case "Black pickaxe":
                return 11;
            case "Mithril pickaxe":
                return 21;
            case "Adamant pickaxe":
                return 31;
            case "Rune pickaxe":
            case "Gilded pickaxe":
                return 41;
            default:
                return 61; // dragon pickaxe and variants, crystal
        }
    }

    // ------------------------------------------------------------------
    // NPC-side transforms and immunities
    // ------------------------------------------------------------------

    private HitTransformer buildNpcTransformer(CombatStyleType styleType) {
        if (isImmune(styleType)) {
            return h -> HitDistribution.single(1.0, Hitsplat.INACCURATE);
        }

        List<HitTransformer> effects = new ArrayList<>();
        List<Boolean> transformInaccurate = new ArrayList<>();
        EquipmentItem weapon = player.getWeapon();

        if ("Zulrah".equals(monster.getName())) {
            effects.add(Transforms.cappedReroll(50, 5, 45));
            transformInaccurate.add(true);
        }
        if ("Fragment of Seren".equals(monster.getName())) {
            effects.add(Transforms.linearMin(2, 22));
            transformInaccurate.add(true);
        }
        if (("Kraken".equals(monster.getName()) || "Cave kraken".equals(monster.getName())) && styleType == CombatStyleType.RANGED) {
            effects.add(Transforms.division(7, 1));
            transformInaccurate.add(true);
        }
        if (VERZIK_P1_IDS.contains(monster.getId()) && !wearing("Dawnbringer")) {
            int limit = isUsingMeleeStyle() ? 10 : 3;
            effects.add(Transforms.linearMin(limit, 0));
            transformInaccurate.add(true);
        }
        if (TEKTON_IDS.contains(monster.getId()) && styleType == CombatStyleType.MAGIC) {
            effects.add(Transforms.division(5, 1));
            transformInaccurate.add(true);
        }
        if (GLOWING_CRYSTAL_IDS.contains(monster.getId()) && styleType == CombatStyleType.MAGIC) {
            effects.add(Transforms.division(3));
            transformInaccurate.add(true);
        }
        if ((OLM_MELEE_HAND_IDS.contains(monster.getId()) || OLM_HEAD_IDS.contains(monster.getId())) && styleType == CombatStyleType.MAGIC) {
            effects.add(Transforms.division(3));
            transformInaccurate.add(true);
        }
        if ((OLM_MAGE_HAND_IDS.contains(monster.getId()) || OLM_MELEE_HAND_IDS.contains(monster.getId())) && styleType == CombatStyleType.RANGED) {
            effects.add(Transforms.division(3));
            transformInaccurate.add(true);
        }
        if (ICE_DEMON_IDS.contains(monster.getId()) && !"fire".equals(getSpellement()) && !isUsingDemonbane()) {
            effects.add(Transforms.division(3));
            transformInaccurate.add(true);
        }
        if ("Slagilith".equals(monster.getName()) && (weapon == null || weapon.getWeaponCategory() != WeaponCategory.PICKAXE)) {
            effects.add(Transforms.division(3));
            transformInaccurate.add(true);
        }
        if (NIGHTMARE_TOTEM_IDS.contains(monster.getId()) && styleType == CombatStyleType.MAGIC) {
            effects.add(Transforms.multiply(2, 1));
            transformInaccurate.add(true);
        }
        if ("Slash Bash".equals(monster.getName()) || "Zogre".equals(monster.getName()) || "Skogre".equals(monster.getName())) {
            if (spell != null && "Crumble Undead".equals(spell.getName())) {
                effects.add(Transforms.division(2));
                transformInaccurate.add(true);
            } else {
                EquipmentItem ammo = player.get(GearSlot.AMMO);
                boolean brutalOgreBow = style.getType() == CombatStyleType.RANGED
                        && ammo != null && ammo.getName().contains(" brutal")
                        && weapon != null && "Comp ogre bow".equals(weapon.getName());
                if (!brutalOgreBow) {
                    effects.add(Transforms.division(4));
                    transformInaccurate.add(true);
                }
            }
        }
        if ("Tormented Demon".equals(monster.getName()) && !isUsingDemonbane() && !isUsingAbyssal()) {
            // 20% damage reduction when the shield is up and not using demonbane/abyssal weapons
            effects.add(Transforms.multiply(4, 5, 1));
            transformInaccurate.add(true);
        }
        if (monster.hasAttribute(MonsterAttributeType.VAMPYRE_2)) {
            if (!wearingVampyrebane(MonsterAttributeType.VAMPYRE_2) && wearing("Efaritay's aid")) {
                effects.add(Transforms.division(2));
                transformInaccurate.add(true);
            } else if (isWearingSilverWeapon()) {
                effects.add(Transforms.flatLimit(10, 0));
                transformInaccurate.add(true);
            }
        }
        if (HUEYCOATL_TAIL_IDS.contains(monster.getId())) {
            boolean crush = styleType == CombatStyleType.CRUSH
                    && offCrush > offSlash
                    && offCrush > offStab;
            boolean earth = "earth".equals(getSpellement());
            effects.add(Transforms.linearMin((crush || earth) ? 9 : 4, 0));
            transformInaccurate.add(true);
            if (crush) {
                effects.add(h -> {
                    if (h.getDamage() > 0) {
                        return HitDistribution.single(1.0, h);
                    }
                    return HitDistribution.single(1.0, new Hitsplat(1));
                });
                transformInaccurate.add(true);
            }
        }

        int flatArmour = monster.getDefensive().getFlat_armour();
        if (flatArmour > 0 && styleType != CombatStyleType.MAGIC) {
            effects.add(Transforms.flatAdd(-flatArmour, 0));
            transformInaccurate.add(false);
        }

        return hitsplat -> {
            HitDistribution d = HitDistribution.single(1.0, hitsplat);
            for (int i = 0; i < effects.size(); i++) {
                d = d.wideTransform(effects.get(i), transformInaccurate.get(i));
            }
            return d.flatten();
        };
    }

    /**
     * Whether the monster is fully immune to the player's current attack.
     * @param styleType The player's damage type
     * @return True when the monster cannot be damaged by this attack
     */
    public boolean isImmune(CombatStyleType styleType) {
        int monsterId = monster.getId();
        EquipmentItem weapon = player.getWeapon();
        WeaponCategory category = weapon == null ? WeaponCategory.UNARMED : weapon.getWeaponCategory();

        if (IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS.contains(monsterId) && styleType == CombatStyleType.MAGIC) {
            return true;
        }
        if (IMMUNE_TO_RANGED_DAMAGE_NPC_IDS.contains(monsterId) && styleType == CombatStyleType.RANGED) {
            return true;
        }
        if (IMMUNE_TO_MELEE_DAMAGE_NPC_IDS.contains(monsterId) && isUsingMeleeStyle()) {
            // Zulrah can be hit by melee with a polearm
            return !(ZULRAH_IDS.contains(monsterId) && category == WeaponCategory.POLEARM);
        }
        if (monster.hasAttribute(MonsterAttributeType.FLYING) && isUsingMeleeStyle()) {
            // Vespula is immune to melee despite the flying attribute exceptions
            if (VESPULA_IDS.contains(monsterId)) return true;
            return category != WeaponCategory.POLEARM && category != WeaponCategory.SALAMANDER;
        }
        if (IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS.contains(monsterId)
                && isUsingMeleeStyle() && category != WeaponCategory.SALAMANDER) {
            return true;
        }
        if (monster.hasAttribute(MonsterAttributeType.VAMPYRE_3) && !wearingVampyrebane(MonsterAttributeType.VAMPYRE_3)) {
            return true;
        }
        if (monster.hasAttribute(MonsterAttributeType.VAMPYRE_2)
                && !wearingVampyrebane(MonsterAttributeType.VAMPYRE_2)
                && !wearing("Efaritay's aid")
                && !isWearingSilverWeapon()) {
            return true;
        }
        if (GUARDIAN_IDS.contains(monsterId) && (!isUsingMeleeStyle() || category != WeaponCategory.PICKAXE)) {
            return true;
        }
        if (monster.hasAttribute(MonsterAttributeType.LEAFY) && !isWearingLeafBladedWeapon()) {
            return true;
        }
        if (!monster.hasAttribute(MonsterAttributeType.RAT) && isWearingRatBoneWeapon()) {
            return true;
        }
        EquipmentItem ammo = player.get(GearSlot.AMMO);
        if ("Fire Warrior of Lesarkus".equals(monster.getName())
                && (styleType != CombatStyleType.RANGED || ammo == null || !"Ice arrows".equals(ammo.getName()))) {
            return true;
        }
        if ("Fareed".equals(monster.getName())) {
            if ((styleType == CombatStyleType.MAGIC && !"water".equals(getSpellement()))
                    || (styleType == CombatStyleType.RANGED && (ammo == null || !ammo.getName().contains("arrow")))) {
                return true;
            }
        }
        // Eclipse moon clone is immune to non-melee attacks
        return ECLIPSE_MOON_IDS.contains(monsterId) && "Clone".equals(monster.getVersion()) && !isUsingMeleeStyle();
    }

    // ------------------------------------------------------------------
    // Attack speed and final outputs
    // ------------------------------------------------------------------

    /**
     * Gets the player's attack speed in ticks for the current weapon and stance.
     * @return int the attack speed in game ticks
     */
    public int getAttackSpeed() {
        if (player.getAttackSpeedOverride() > 0) {
            return player.getAttackSpeedOverride();
        }

        EquipmentItem weapon = player.getWeapon();
        int attackSpeed = weapon != null && weapon.getSpeed() > 0 ? weapon.getSpeed() : DEFAULT_ATTACK_SPEED;

        CombatStance stance = style.getStance();
        if (style.getType() == CombatStyleType.RANGED && stance == CombatStance.RAPID) {
            attackSpeed -= 1;
        } else if (stance != null && stance.isCastStance()) {
            if (weapon != null && "Harmonised nightmare staff".equals(weapon.getName())
                    && spell != null && spell.isStandardSpellbook()
                    && stance != CombatStance.MANUAL_CAST) {
                attackSpeed = 4;
            } else if (weapon != null && "Twinflame staff".equals(weapon.getName())) {
                attackSpeed = 6;
            } else {
                attackSpeed = 5;
            }
        }

        // Giant rat (Scurrius) with bone weapons
        if (monster.getId() == 7223 && stance != CombatStance.MANUAL_CAST
                && weapon != null && wearing("Bone mace", "Bone shortbow", "Bone staff")) {
            attackSpeed = 1;
        }

        return Math.max(attackSpeed, 1);
    }

    /**
     * Gets the expected attack speed in ticks, accounting for the blood moon set's
     * chance to speed up the next attack.
     * @return double the expected ticks between attacks
     */
    public double getExpectedAttackSpeed() {
        if (isWearingBloodMoonSet()) {
            double acc = getHitChance();
            double procChance = (acc / 3) + (acc * acc * 2 / 9);
            return getAttackSpeed() - procChance;
        }
        return getAttackSpeed();
    }

    /**
     * Gets the expected damage dealt per attack.
     * @return double the expected damage of one attack
     */
    public double getExpectedDamage() {
        return getDistribution().getExpectedDamage();
    }

    /**
     * Gets the highest possible hit of one attack, after all effects.
     * @return int the displayed max hit
     */
    public int getMax() {
        return getDistribution().getMax();
    }

    /**
     * Gets the expected damage per tick.
     * @return double the damage per tick
     */
    public double getDpt() {
        return getExpectedDamage() / getExpectedAttackSpeed();
    }

    /**
     * Gets the expected damage per second — the primary output of the calculator.
     * @return double the DPS
     */
    public double getDps() {
        return getDpt() / SECONDS_PER_TICK;
    }

    /**
     * Gets the average number of hits needed to kill the monster from its current hp.
     * @return double the expected hits-to-kill, or 0 when the player cannot deal damage
     */
    public double getHtk() {
        AttackDistribution dist = getDistribution();
        double[] hist = dist.asHistogram();
        int startHp = monster.getCurrentHpOrFull();
        int max = Math.min(startHp, dist.getMax());
        if (max == 0) {
            return 0;
        }

        double zeroProb = hist.length > 0 ? hist[0] : 0;
        if (1 - zeroProb <= 0) {
            return 0;
        }

        double[] htk = new double[startHp + 1];
        for (int hp = 1; hp <= startHp; hp++) {
            double val = 1.0; // takes at least one hit
            for (int hit = 1; hit <= Math.min(hp, max); hit++) {
                if (hit < hist.length) {
                    val += hist[hit] * htk[hp - hit];
                }
            }
            htk[hp] = val / (1 - zeroProb);
        }

        return htk[startHp];
    }

    /**
     * Gets the average time-to-kill in seconds.
     * @return double the expected seconds to kill the monster
     */
    public double getTtk() {
        return getHtk() * getExpectedAttackSpeed() * SECONDS_PER_TICK;
    }

    /**
     * Gets the resolved attack style used for this calculation.
     * @return AttackStyle the style in use
     */
    public AttackStyle getStyle() {
        return style;
    }

    /**
     * Gets the sanitized loadout used for this calculation.
     * @return Loadout the loadout copy used internally
     */
    public Loadout getPlayer() {
        return player;
    }

    /**
     * Gets the monster (after defence reductions) used for this calculation.
     * @return MonsterData the monster copy used internally
     */
    public MonsterData getMonster() {
        return monster;
    }

    /**
     * Gets the player's total prayer bonus from equipment.
     * @return int the prayer bonus
     */
    public int getTotalPrayerBonus() {
        return totalPrayerBonus;
    }
}
