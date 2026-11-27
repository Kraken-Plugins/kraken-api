package com.kraken.api.simulation.data;

/**
 * One switchable gear set as the simulation sees it: weapon timing and range, per NPC type damage channels, and the
 * prayer economy it implies. Built by the plugin from its gear catalogue, or directly by tests.
 */
public final class EquipSet {
    /** The weapon's {@link com.kraken.api.simulation.hits.Style}. */
    public int styleCode;
    /** Ticks between the weapon's attacks. */
    public int attackSpeed = 4;
    /** The weapon's attack range in tiles. */
    public int attackRange = 1;
    /** Expected damage per attack, indexed by NpcDef type index. */
    public int[] expectedDamage;
    /** Max hit per attack, same indexing. */
    public int[] maxHit;
    /** Prayer bonus for the drain resistance formula 2 x bonus + 60. */
    public int prayerBonus;
    /** Drain effect of the offensive prayer this set keeps up (0 = none). */
    public int offensiveDrain;
    /** Special attack cost in tenths of a percent (500 = 50%). */
    public int specCost10 = 500;
    /** Damage multiplier x1000 applied by the special attack (2000 = double). */
    public int specMult1000 = 2000;
    /** Venator style bounce: a second hit at half damage on a nearby second target. */
    public boolean bounce;

    /**
     * Creates a set with zeroed damage tables.
     * @param styleCode the weapon's style
     * @param attackSpeed the weapon's attack speed in ticks
     * @param attackRange the weapon's range in tiles
     * @param typeCount the number of NPC types, the damage tables' length
     */
    public EquipSet(int styleCode, int attackSpeed, int attackRange, int typeCount) {
        this.styleCode = styleCode;
        this.attackSpeed = attackSpeed;
        this.attackRange = attackRange;
        expectedDamage = new int[typeCount];
        maxHit = new int[typeCount];
    }
}
