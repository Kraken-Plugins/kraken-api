package com.kraken.api.service.util.dps.model;

/**
 * Attributes a monster can have which interact with specific weapons and gear
 * (e.g. dragonbane weapons against DRAGON monsters, salve amulet against UNDEAD).
 * @see <a href="https://oldschool.runescape.wiki/w/Monster_attribute">wiki monster attributes</a>
 */
public enum MonsterAttributeType {
    /** The demon attribute. */
    DEMON("demon"),
    /** The dragon attribute. */
    DRAGON("dragon"),
    /** The fiery attribute. */
    FIERY("fiery"),
    /** The flying attribute. */
    FLYING("flying"),
    /** The golem attribute. */
    GOLEM("golem"),
    /** The kalphite attribute. */
    KALPHITE("kalphite"),
    /** The leafy attribute. */
    LEAFY("leafy"),
    /** The penance attribute. */
    PENANCE("penance"),
    /** The rat attribute. */
    RAT("rat"),
    /** The shade attribute. */
    SHADE("shade"),
    /** The spectral attribute. */
    SPECTRAL("spectral"),
    /** The undead attribute. */
    UNDEAD("undead"),
    /** The first vampyre tier attribute. */
    VAMPYRE_1("vampyre1"),
    /** The second vampyre tier attribute. */
    VAMPYRE_2("vampyre2"),
    /** The third vampyre tier attribute. */
    VAMPYRE_3("vampyre3"),
    /** The xerician attribute. */
    XERICIAN("xerician");

    private final String key;

    MonsterAttributeType(String key) {
        this.key = key;
    }

    /**
     * Gets the lowercase wiki attribute key for this attribute, e.g. "undead".
     * @return String the wiki attribute key
     */
    public String getKey() {
        return key;
    }

    /**
     * Whether this attribute is any tier of vampyre.
     * @return True for VAMPYRE_1, VAMPYRE_2 and VAMPYRE_3
     */
    public boolean isVampyre() {
        return this == VAMPYRE_1 || this == VAMPYRE_2 || this == VAMPYRE_3;
    }

    /**
     * Parses a wiki attribute key (e.g. "undead") into a MonsterAttributeType.
     * @param key The attribute key
     * @return MonsterAttributeType or null for unknown keys
     */
    public static MonsterAttributeType fromKey(String key) {
        if (key == null) return null;
        for (MonsterAttributeType t : values()) {
            if (t.key.equalsIgnoreCase(key)) return t;
        }
        return null;
    }
}
