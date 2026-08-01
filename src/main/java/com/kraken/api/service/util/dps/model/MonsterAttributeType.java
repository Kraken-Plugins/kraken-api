package com.kraken.api.service.util.dps.model;

/**
 * Attributes a monster can have which interact with specific weapons and gear
 * (e.g. dragonbane weapons against DRAGON monsters, salve amulet against UNDEAD).
 * @see <a href="https://oldschool.runescape.wiki/w/Monster_attribute">wiki monster attributes</a>
 */
public enum MonsterAttributeType {
    DEMON("demon"),
    DRAGON("dragon"),
    FIERY("fiery"),
    FLYING("flying"),
    GOLEM("golem"),
    KALPHITE("kalphite"),
    LEAFY("leafy"),
    PENANCE("penance"),
    RAT("rat"),
    SHADE("shade"),
    SPECTRAL("spectral"),
    UNDEAD("undead"),
    VAMPYRE_1("vampyre1"),
    VAMPYRE_2("vampyre2"),
    VAMPYRE_3("vampyre3"),
    XERICIAN("xerician");

    private final String key;

    MonsterAttributeType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

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
