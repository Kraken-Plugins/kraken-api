package com.kraken.api.service.util.dps.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A combat spell loaded from the bundled OSRS wiki spell data. Instances are read-only once loaded
 * and shared by every caller of {@link com.kraken.api.service.util.dps.data.DpsDataStore}.
 */
@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class SpellData {
    private String name;
    private int maxHit;
    /** One of "standard", "ancient", "lunar", "arceuus". */
    private String spellbook;
    /** Elemental class of the spell: "air", "water", "earth", "fire", or null. */
    private String element;

    public boolean isStandardSpellbook() {
        return "standard".equals(spellbook);
    }

    public boolean isAncientSpellbook() {
        return "ancient".equals(spellbook);
    }

    /**
     * Whether this spell is a binding spell (Bind, Snare, Entangle).
     * @return True for binding spells
     */
    public boolean isBindSpell() {
        return "Bind".equals(name) || "Snare".equals(name) || "Entangle".equals(name);
    }

    /**
     * Whether sunfire runes can be used to cast this spell (any spell needing fire runes).
     * @return True when the spell is fire-elemental
     */
    public boolean canUseSunfireRunes() {
        return "fire".equals(element);
    }
}
