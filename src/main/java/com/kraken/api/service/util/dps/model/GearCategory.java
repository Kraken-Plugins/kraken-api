package com.kraken.api.service.util.dps.model;

/**
 * A coarse classification of a piece of gear by the combat style it benefits.
 * Used by the smart gear search to prune the loadout space: a Masori body with high
 * ranged accuracy and no melee strength is clearly RANGED gear, while a ring with
 * bonuses in every style is HYBRID.
 */
public enum GearCategory {
    MELEE,
    RANGED,
    MAGIC,
    /** Provides meaningful offensive bonuses to more than one combat style. */
    HYBRID,
    /** Provides no offensive bonuses (purely defensive or utility gear). */
    NONE
}
