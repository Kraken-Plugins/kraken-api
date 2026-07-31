package com.kraken.api.service.util.dps.calc;

/**
 * A single hitsplat: a damage value and whether the underlying accuracy roll passed.
 * An inaccurate hitsplat always deals 0 damage but is tracked separately from an
 * accurate 0 so effects that only apply on successful hits behave correctly.
 */
public final class Hitsplat {

    public static final Hitsplat INACCURATE = new Hitsplat(0, false);

    private final int damage;
    private final boolean accurate;

    public Hitsplat(int damage) {
        this(damage, true);
    }

    public Hitsplat(int damage, boolean accurate) {
        this.damage = damage;
        this.accurate = accurate;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isAccurate() {
        return accurate;
    }

    @Override
    public String toString() {
        return "Hitsplat(" + damage + (accurate ? "" : ", miss") + ")";
    }
}
