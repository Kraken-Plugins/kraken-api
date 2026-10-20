package com.kraken.api.service.util.dps.model;

/**
 * The stance (attack option behaviour) the player has selected on the combat tab.
 * Stances grant invisible level bonuses and, for ranged/magic, can modify attack speed.
 */
public enum CombatStance {
    /** Accurate stance, which trains the attack level. */
    ACCURATE,
    /** Aggressive stance, which trains the strength level. */
    AGGRESSIVE,
    /** Controlled stance, which spreads experience across attack, strength and defence. */
    CONTROLLED,
    /** Defensive stance, which trains the defence level. */
    DEFENSIVE,
    /** Autocast stance, which casts the selected spell automatically. */
    AUTOCAST,
    /** Defensive autocast stance, which autocasts the selected spell and trains defence. */
    DEFENSIVE_AUTOCAST,
    /** Long range stance, which extends the attack range. */
    LONGRANGE,
    /** Rapid stance, which shortens the attack speed. */
    RAPID,
    /** Manual cast stance, which casts spells one at a time instead of autocasting. */
    MANUAL_CAST;

    /**
     * Whether this stance casts a spell (autocast variants or a manual cast).
     * @return True when the stance casts a spell
     */
    public boolean isCastStance() {
        return this == AUTOCAST || this == DEFENSIVE_AUTOCAST || this == MANUAL_CAST;
    }
}
