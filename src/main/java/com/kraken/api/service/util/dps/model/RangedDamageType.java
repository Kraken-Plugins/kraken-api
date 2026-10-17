package com.kraken.api.service.util.dps.model;

/**
 * The class of ranged damage a weapon deals. NPCs have separate defensive bonuses
 * for light, standard and heavy ranged damage.
 */
public enum RangedDamageType {
    /** Light ranged damage, dealt by thrown weapons. */
    LIGHT,
    /** Standard ranged damage, dealt by bows. */
    STANDARD,
    /** Heavy ranged damage, dealt by crossbows and chinchompas. */
    HEAVY,
    /** Mixed ranged damage, dealt by salamanders. */
    MIXED
}
