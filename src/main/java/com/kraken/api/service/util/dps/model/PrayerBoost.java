package com.kraken.api.service.util.dps.model;

import net.runelite.api.Prayer;

/**
 * Offensive prayers and the multipliers they grant, ported from the OSRS wiki DPS calculator.
 * Accuracy and strength factors are expressed as a numerator over 100 (e.g. Piety accuracy = 120
 * for a 20% boost); the magic damage bonus is in tenths of a percent (e.g. Augury = 40 = +4%).
 */
public enum PrayerBoost {
    /** Burst of Strength, which boosts melee strength by 5%. */
    BURST_OF_STRENGTH(PrayerStyle.MELEE, 1, 0, 105, 0),
    /** Clarity of Thought, which boosts melee accuracy by 5%. */
    CLARITY_OF_THOUGHT(PrayerStyle.MELEE, 1, 105, 0, 0),
    /** Sharp Eye, which boosts ranged accuracy and strength by 5%. */
    SHARP_EYE(PrayerStyle.RANGED, 1, 105, 105, 0),
    /** Mystic Will, which boosts magic accuracy by 5%. */
    MYSTIC_WILL(PrayerStyle.MAGIC, 1, 105, 0, 0),
    /** Superhuman Strength, which boosts melee strength by 10%. */
    SUPERHUMAN_STRENGTH(PrayerStyle.MELEE, 6, 0, 110, 0),
    /** Improved Reflexes, which boosts melee accuracy by 10%. */
    IMPROVED_REFLEXES(PrayerStyle.MELEE, 6, 110, 0, 0),
    /** Hawk Eye, which boosts ranged accuracy and strength by 10%. */
    HAWK_EYE(PrayerStyle.RANGED, 6, 110, 110, 0),
    /** Mystic Lore, which boosts magic accuracy by 10% and magic damage by 1%. */
    MYSTIC_LORE(PrayerStyle.MAGIC, 6, 110, 0, 10),
    /** Ultimate Strength, which boosts melee strength by 15%. */
    ULTIMATE_STRENGTH(PrayerStyle.MELEE, 12, 0, 115, 0),
    /** Incredible Reflexes, which boosts melee accuracy by 15%. */
    INCREDIBLE_REFLEXES(PrayerStyle.MELEE, 12, 115, 0, 0),
    /** Eagle Eye, which boosts ranged accuracy and strength by 15%. */
    EAGLE_EYE(PrayerStyle.RANGED, 12, 115, 115, 0),
    /** Mystic Might, which boosts magic accuracy by 15% and magic damage by 2%. */
    MYSTIC_MIGHT(PrayerStyle.MAGIC, 12, 115, 0, 20),
    /** Deadeye, which boosts ranged accuracy and strength by 18%. */
    DEADEYE(PrayerStyle.RANGED, 12, 118, 118, 0),
    /** Mystic Vigour, which boosts magic accuracy by 18% and magic damage by 3%. */
    MYSTIC_VIGOUR(PrayerStyle.MAGIC, 12, 118, 0, 30),
    /** Chivalry, which boosts melee accuracy by 15% and melee strength by 18%. */
    CHIVALRY(PrayerStyle.MELEE, 24, 115, 118, 0),
    /** Piety, which boosts melee accuracy by 20% and melee strength by 23%. */
    PIETY(PrayerStyle.MELEE, 24, 120, 123, 0),
    /** Rigour, which boosts ranged accuracy by 20% and ranged strength by 23%. */
    RIGOUR(PrayerStyle.RANGED, 24, 120, 123, 0),
    /** Augury, which boosts magic accuracy by 25% and magic damage by 4%. */
    AUGURY(PrayerStyle.MAGIC, 24, 125, 0, 40);

    /** The combat style class a prayer boosts. */
    public enum PrayerStyle {
        /** Melee prayers, which boost stab, slash and crush attacks. */
        MELEE,
        /** Ranged prayers, which boost ranged attacks. */
        RANGED,
        /** Magic prayers, which boost magic attacks. */
        MAGIC
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

    /**
     * Gets the combat style class this prayer boosts.
     * @return PrayerStyle for this prayer
     */
    public PrayerStyle getStyle() {
        return style;
    }

    /**
     * Gets this prayer's drain effect from the OSRS Wiki data. Prayers with a higher drain effect use prayer points
     * faster.
     * @return the prayer's drain effect
     */
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
