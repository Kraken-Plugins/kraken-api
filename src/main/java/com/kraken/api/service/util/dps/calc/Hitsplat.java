package com.kraken.api.service.util.dps.calc;

/**
 * A single hitsplat: a damage value and whether the underlying accuracy roll passed.
 * An inaccurate hitsplat always deals 0 damage but is tracked separately from an
 * accurate 0 so effects that only apply on successful hits behave correctly.
 */
public final class Hitsplat {

    /** A missed hitsplat, which deals no damage. */
    public static final Hitsplat INACCURATE = new Hitsplat(0, false);

    private final int damage;
    private final boolean accurate;

    /**
     * Creates an accurate hitsplat.
     * @param damage The damage dealt
     */
    public Hitsplat(int damage) {
        this(damage, true);
    }

    /**
     * Creates a hitsplat.
     * @param damage The damage dealt
     * @param accurate Whether the accuracy roll passed
     */
    public Hitsplat(int damage, boolean accurate) {
        this.damage = damage;
        this.accurate = accurate;
    }

    /**
     * Gets the damage dealt by this hitsplat.
     * @return int the damage
     */
    public int getDamage() {
        return damage;
    }

    /**
     * Checks whether the accuracy roll passed.
     * @return boolean true if the hitsplat is accurate, false if it is a miss
     */
    public boolean isAccurate() {
        return accurate;
    }

    @Override
    public String toString() {
        return "Hitsplat(" + damage + (accurate ? "" : ", miss") + ")";
    }
}
