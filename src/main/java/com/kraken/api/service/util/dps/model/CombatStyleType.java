package com.kraken.api.service.util.dps.model;

/**
 * The type of combat attack the player (or an NPC) is using. Melee splits into the three
 * damage types which roll against separate NPC defensive bonuses.
 */
public enum CombatStyleType {
    STAB("stab"),
    SLASH("slash"),
    CRUSH("crush"),
    MAGIC("magic"),
    RANGED("ranged");

    private final String key;

    CombatStyleType(String key) {
        this.key = key;
    }

    /**
     * Gets the lowercase wiki key for this style, e.g. "stab".
     * @return String the wiki key for this combat style
     */
    public String getKey() {
        return key;
    }

    /**
     * Whether this style is one of the three melee damage types.
     * @return True when this style is stab, slash, or crush
     */
    public boolean isMelee() {
        return this == STAB || this == SLASH || this == CRUSH;
    }

    /**
     * Parses a wiki style key (e.g. "stab", "ranged") into a CombatStyleType.
     * @param key The lowercase style key
     * @return CombatStyleType or null when the key does not match any style
     */
    public static CombatStyleType fromKey(String key) {
        if (key == null) return null;
        for (CombatStyleType t : values()) {
            if (t.key.equalsIgnoreCase(key)) return t;
        }
        return null;
    }
}
