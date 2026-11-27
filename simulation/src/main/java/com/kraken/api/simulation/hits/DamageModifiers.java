package com.kraken.api.simulation.hits;

/**
 * How a frame's damage model departs from the plain one, filled by the content pack from its active modifiers. Read
 * by the core at every NPC launch and every hit on the player, so it holds resolved numbers only.
 */
public final class DamageModifiers {
    /** No modifier active. */
    public static final DamageModifiers NONE = new DamageModifiers();

    /** Added to every NPC attack's max hit, in both damage channels. */
    public int npcMaxHitBonus;
    /** Added to the pipeline's unprayed expected damage factor, x1000. */
    public int unprayedFactorAdd1000;
    /** When at least 0, replaces the unprayed expected damage factor, x1000. */
    public int unprayedFactorOverride1000 = -1;
    /** Prayer points drained per hit, as a percentage of the expected damage taken. */
    public int prayerDrainPctOfDamage;
    /** Each damage instance with a worst case above zero adds a stack ({@code SimState.hitStacksWorst}). */
    public boolean stackPerHit;
}
