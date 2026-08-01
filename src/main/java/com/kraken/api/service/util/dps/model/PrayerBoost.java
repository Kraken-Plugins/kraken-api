package com.kraken.api.service.util.dps.model;

import net.runelite.api.Prayer;

/**
 * Offensive prayers and the multipliers they grant, ported from the OSRS wiki DPS calculator.
 * Accuracy and strength factors are expressed as a numerator over 100 (e.g. Piety accuracy = 120
 * for a 20% boost); the magic damage bonus is in tenths of a percent (e.g. Augury = 40 = +4%).
 */
public enum PrayerBoost {
    BURST_OF_STRENGTH(PrayerStyle.MELEE, 1, 0, 105, 0),
    CLARITY_OF_THOUGHT(PrayerStyle.MELEE, 1, 105, 0, 0),
    SHARP_EYE(PrayerStyle.RANGED, 1, 105, 105, 0),
    MYSTIC_WILL(PrayerStyle.MAGIC, 1, 105, 0, 0),
    SUPERHUMAN_STRENGTH(PrayerStyle.MELEE, 6, 0, 110, 0),
    IMPROVED_REFLEXES(PrayerStyle.MELEE, 6, 110, 0, 0),
    HAWK_EYE(PrayerStyle.RANGED, 6, 110, 110, 0),
    MYSTIC_LORE(PrayerStyle.MAGIC, 6, 110, 0, 10),
    ULTIMATE_STRENGTH(PrayerStyle.MELEE, 12, 0, 115, 0),
    INCREDIBLE_REFLEXES(PrayerStyle.MELEE, 12, 115, 0, 0),
    EAGLE_EYE(PrayerStyle.RANGED, 12, 115, 115, 0),
    MYSTIC_MIGHT(PrayerStyle.MAGIC, 12, 115, 0, 20),
    DEADEYE(PrayerStyle.RANGED, 12, 118, 118, 0),
    MYSTIC_VIGOUR(PrayerStyle.MAGIC, 12, 118, 0, 30),
    CHIVALRY(PrayerStyle.MELEE, 24, 115, 118, 0),
    PIETY(PrayerStyle.MELEE, 24, 120, 123, 0),
    RIGOUR(PrayerStyle.RANGED, 24, 120, 123, 0),
    AUGURY(PrayerStyle.MAGIC, 24, 125, 0, 40);

    /** The combat style class a prayer boosts. */
    public enum PrayerStyle {
        MELEE, RANGED, MAGIC
    }

    private final PrayerStyle style;
    private final int drainRate;
    private final int accuracyFactor;
    private final int strengthFactor;
    private final int magicDamageBonus;

    PrayerBoost(PrayerStyle style, int drainRate, int accuracyFactor, int strengthFactor, int magicDamageBonus) {
        this.style = style;
        this.drainRate = drainRate;
        this.accuracyFactor = accuracyFactor;
        this.strengthFactor = strengthFactor;
        this.magicDamageBonus = magicDamageBonus;
    }

    /**
     * Whether this prayer applies when attacking with the given style type. Melee prayers
     * apply to stab, slash and crush attacks.
     * @param type The player's attack style type
     * @return True when this prayer boosts that style
     */
    public boolean appliesTo(CombatStyleType type) {
        if (type == null) return false;
        switch (style) {
            case MELEE: return type.isMelee();
            case RANGED: return type == CombatStyleType.RANGED;
            case MAGIC: return type == CombatStyleType.MAGIC;
            default: return false;
        }
    }

    public PrayerStyle getStyle() {
        return style;
    }

    public int getDrainRate() {
        return drainRate;
    }

    /**
     * Gets this prayer's accuracy multiplier numerator over 100, or 0 when it has none.
     * @return int accuracy factor numerator, e.g. 120 for Piety
     */
    public int getAccuracyFactor() {
        return accuracyFactor;
    }

    /**
     * Gets this prayer's strength/damage multiplier numerator over 100, or 0 when it has none.
     * @return int strength factor numerator, e.g. 123 for Piety
     */
    public int getStrengthFactor() {
        return strengthFactor;
    }

    /**
     * Gets this prayer's magic damage bonus in tenths of a percent, or 0 when it has none.
     * @return int magic damage bonus, e.g. 40 for Augury
     */
    public int getMagicDamageBonus() {
        return magicDamageBonus;
    }

    /**
     * Maps a RuneLite prayer to its DPS-relevant boost, if it has one.
     * @param prayer The RuneLite prayer
     * @return PrayerBoost or null when the prayer has no offensive effect
     */
    public static PrayerBoost fromRuneLite(Prayer prayer) {
        if (prayer == null) return null;
        try {
            return valueOf(prayer.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
