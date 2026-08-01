package plugins.colosseum.simulation;

import lombok.Builder;
import lombok.Getter;

/**
 * Player capability configuration for the colosseum simulation: gear sets with expected
 * damage output per NPC type, supply heal values, and the passive stats (prayer bonus,
 * agility, weight) that drive drain/regen formulas.
 *
 * <p>Expected damage values are deliberately simple averages (accuracy x mean hit) because
 * the planner optimises expected outcomes; exact damage rolls are unknowable one tick ahead.
 * Callers should tune GearSet#getExpectedDamageByType() to their actual gear.</p>
 */
@Getter
@Builder(toBuilder = true)
public final class LoadoutConfig {
    /** Index of the melee gear set in getGearSets() for the default loadout. */
    public static final int SET_MELEE = 0;
    /** Index of the ranged gear set in getGearSets() for the default loadout. */
    public static final int SET_RANGED = 1;
    /** Index of the magic gear set in getGearSets() for the default loadout. */
    public static final int SET_MAGIC = 2;

    /**
     * One switchable equipment loadout (weapon + armour swap executed as one batch of
     * same-tick equips).
     */
    @Getter
    @Builder(toBuilder = true)
    public static final class GearSet {
        private final String name;
        private final NpcAttackStyle style;
        private final int attackSpeedTicks;
        private final int attackRange;
        /** Inventory item ids the executor equips when switching to this set. */
        private final int[] equipItemIds;
        /** Expected damage per attack indexed by {@link NpcType#ordinal()}. */
        private final double[] expectedDamageByType;

        /**
         * @param type target npc type.
         * @return expected damage per attack against the type.
         */
        public double expectedDamageVs(NpcType type) {
            if (expectedDamageByType == null || type.ordinal() >= expectedDamageByType.length) {
                return 0;
            }
            return expectedDamageByType[type.ordinal()];
        }
    }

    private final GearSet[] gearSets;

    /** Hitpoints healed by one piece of primary food. */
    private final int foodHealAmount;
    /** Hitpoints healed by one combo food (karambwan). */
    private final int comboHealAmount;
    /** Hitpoints healed per Saradomin brew dose. */
    private final int brewHealPerDose;
    /** Prayer points restored per super restore dose. */
    private final int restorePrayerPerDose;

    /** Item ids used by the executor to click supplies. */
    private final int[] foodItemIds;
    private final int[] comboItemIds;
    private final int[] brewItemIds;
    private final int[] restoreItemIds;

    /** Total equipped prayer bonus, drives the prayer drain interval. */
    private final int prayerBonus;
    /** Agility level for run energy drain/restore. */
    private final int agilityLevel;
    /** Carried weight in kg (clamped 0-64 by the drain formula). */
    private final int weightKg;
    /** True when a stamina effect is active (drain multiplied by 0.3). */
    private final boolean staminaActive;
    /** True when a Lightbearer is worn (special attack regenerates twice as fast). */
    private final boolean lightbearer;

    /**
     * Fraction of an NPC's max hit taken as expected damage when the attack is not prayed
     * against (accuracy x average roll). Colosseum NPCs are accurate; 0.30 is conservative.
     */
    private final double npcExpectedDamageFactor;

    /**
     * Builds a sensible high-level default loadout: fang-style melee, twisted-bow-style
     * ranged and shadow-style magic sets with weakness-aware expected damage, anglerfish,
     * karambwans, brews and restores.
     *
     * @return default loadout.
     */
    public static LoadoutConfig defaults() {
        int typeCount = NpcType.values().length;
        double[] melee = new double[typeCount];
        double[] ranged = new double[typeCount];
        double[] magic = new double[typeCount];

        set(melee, NpcType.MANTICORE, 24);
        set(melee, NpcType.SERPENT_SHAMAN, 18);
        set(melee, NpcType.JAVELIN_COLOSSUS, 16);
        set(melee, NpcType.SHOCKWAVE_COLOSSUS, 18);
        set(melee, NpcType.JAGUAR_WARRIOR, 25);
        set(melee, NpcType.MINOTAUR, 24);
        set(melee, NpcType.MINOTAUR_RED_FLAG, 24);
        set(melee, NpcType.FREMENNIK_BERSERKER, 12);
        set(melee, NpcType.FREMENNIK_SEER, 14);
        set(melee, NpcType.FREMENNIK_ARCHER, 30);

        set(ranged, NpcType.MANTICORE, 20);
        set(ranged, NpcType.SERPENT_SHAMAN, 16);
        set(ranged, NpcType.JAVELIN_COLOSSUS, 20);
        set(ranged, NpcType.SHOCKWAVE_COLOSSUS, 14);
        set(ranged, NpcType.JAGUAR_WARRIOR, 16);
        set(ranged, NpcType.MINOTAUR, 14);
        set(ranged, NpcType.MINOTAUR_RED_FLAG, 14);
        set(ranged, NpcType.FREMENNIK_BERSERKER, 12);
        set(ranged, NpcType.FREMENNIK_SEER, 38);
        set(ranged, NpcType.FREMENNIK_ARCHER, 14);

        set(magic, NpcType.MANTICORE, 18);
        set(magic, NpcType.SERPENT_SHAMAN, 20);
        set(magic, NpcType.JAVELIN_COLOSSUS, 18);
        set(magic, NpcType.SHOCKWAVE_COLOSSUS, 20);
        set(magic, NpcType.JAGUAR_WARRIOR, 12);
        set(magic, NpcType.MINOTAUR, 16);
        set(magic, NpcType.MINOTAUR_RED_FLAG, 16);
        set(magic, NpcType.FREMENNIK_BERSERKER, 44);
        set(magic, NpcType.FREMENNIK_SEER, 12);
        set(magic, NpcType.FREMENNIK_ARCHER, 12);

        GearSet meleeSet = GearSet.builder()
                .name("Melee")
                .style(NpcAttackStyle.MELEE)
                .attackSpeedTicks(5)
                .attackRange(1)
                .equipItemIds(new int[0])
                .expectedDamageByType(melee)
                .build();
        GearSet rangedSet = GearSet.builder()
                .name("Ranged")
                .style(NpcAttackStyle.RANGED)
                .attackSpeedTicks(5)
                .attackRange(10)
                .equipItemIds(new int[0])
                .expectedDamageByType(ranged)
                .build();
        GearSet magicSet = GearSet.builder()
                .name("Magic")
                .style(NpcAttackStyle.MAGIC)
                .attackSpeedTicks(5)
                .attackRange(10)
                .equipItemIds(new int[0])
                .expectedDamageByType(magic)
                .build();

        return LoadoutConfig.builder()
                .gearSets(new GearSet[]{meleeSet, rangedSet, magicSet})
                .foodHealAmount(22)
                .comboHealAmount(18)
                .brewHealPerDose(16)
                .restorePrayerPerDose(31)
                .foodItemIds(new int[]{13441})
                .comboItemIds(new int[]{3144})
                .brewItemIds(new int[]{6685, 6687, 6689, 6691})
                .restoreItemIds(new int[]{3024, 3026, 3028, 3030})
                .prayerBonus(30)
                .agilityLevel(99)
                .weightKg(20)
                .staminaActive(false)
                .lightbearer(false)
                .npcExpectedDamageFactor(0.30)
                .build();
    }

    /**
     * @param index gear set index.
     * @return gear set at index, or the first set when out of range.
     */
    public GearSet gearSet(int index) {
        if (gearSets == null || gearSets.length == 0) {
            throw new IllegalStateException("LoadoutConfig has no gear sets");
        }
        if (index < 0 || index >= gearSets.length) {
            return gearSets[0];
        }
        return gearSets[index];
    }

    /** @return number of configured gear sets. */
    public int gearSetCount() {
        return gearSets == null ? 0 : gearSets.length;
    }

    private static void set(double[] table, NpcType type, double value) {
        table[type.ordinal()] = value;
    }
}
