package com.kraken.api.simulation;

import net.runelite.api.Prayer;

/**
 * Simulated NPC combat attack style used for prayer-threat modeling.
 */
public enum NpcAttackStyle {
    MELEE,
    RANGED,
    MAGIC,
    UNKNOWN;

    /**
     * Maps attack style to the corresponding protection prayer.
     *
     * @return overhead protection prayer, or {@code null} when not mappable.
     */
    public Prayer toProtectionPrayer() {
        switch (this) {
            case MELEE:
                return Prayer.PROTECT_FROM_MELEE;
            case RANGED:
                return Prayer.PROTECT_FROM_MISSILES;
            case MAGIC:
                return Prayer.PROTECT_FROM_MAGIC;
            default:
                return null;
        }
    }
}
