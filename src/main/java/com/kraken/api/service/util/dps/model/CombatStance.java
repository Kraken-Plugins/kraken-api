package com.kraken.api.service.util.dps.model;

/**
 * The stance (attack option behaviour) the player has selected on the combat tab.
 * Stances grant invisible level bonuses and, for ranged/magic, can modify attack speed.
 */
public enum CombatStance {
    ACCURATE,
    AGGRESSIVE,
    CONTROLLED,
    DEFENSIVE,
    AUTOCAST,
    DEFENSIVE_AUTOCAST,
    LONGRANGE,
    RAPID,
    MANUAL_CAST;

    /**
     * Whether this stance casts a spell (autocast variants or a manual cast).
     * @return True when the stance casts a spell
     */
    public boolean isCastStance() {
        return this == AUTOCAST || this == DEFENSIVE_AUTOCAST || this == MANUAL_CAST;
    }
}
