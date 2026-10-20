package com.kraken.api.service.util.dps.model;

import net.runelite.api.EquipmentInventorySlot;

/**
 * The eleven equipment slots the DPS calculator considers, keyed by the wiki slot names
 * used in the bundled equipment data.
 */
public enum GearSlot {
    /** The head slot. */
    HEAD("head"),
    /** The cape slot. */
    CAPE("cape"),
    /** The neck slot, which holds amulets. */
    NECK("neck"),
    /** The ammunition slot. */
    AMMO("ammo"),
    /** The weapon slot. */
    WEAPON("weapon"),
    /** The body slot. */
    BODY("body"),
    /** The shield slot. */
    SHIELD("shield"),
    /** The legs slot. */
    LEGS("legs"),
    /** The hands slot, which holds gloves. */
    HANDS("hands"),
    /** The feet slot, which holds boots. */
    FEET("feet"),
    /** The ring slot. */
    RING("ring");

    private final String wikiName;

    GearSlot(String wikiName) {
        this.wikiName = wikiName;
    }

    /**
     * Gets the wiki slot name for this slot, as used in the bundled equipment data.
     * @return String the wiki slot name, e.g. "head"
     */
    public String getWikiName() {
        return wikiName;
    }

    /**
     * Resolves a wiki slot name (e.g. "head") into a GearSlot.
     * @param name The wiki slot name
     * @return GearSlot or null for unknown names
     */
    public static GearSlot fromWikiName(String name) {
        if (name == null) return null;
        for (GearSlot s : values()) {
            if (s.wikiName.equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    /**
     * Maps a RuneLite equipment inventory slot to the corresponding GearSlot.
     * @param slot The RuneLite equipment slot
     * @return GearSlot or null for slots the calculator does not track
     */
    public static GearSlot fromRuneLite(EquipmentInventorySlot slot) {
        if (slot == null) return null;
        switch (slot) {
            case HEAD: return HEAD;
            case CAPE: return CAPE;
            case AMULET: return NECK;
            case AMMO: return AMMO;
            case WEAPON: return WEAPON;
            case BODY: return BODY;
            case SHIELD: return SHIELD;
            case LEGS: return LEGS;
            case GLOVES: return HANDS;
            case BOOTS: return FEET;
            case RING: return RING;
            default: return null;
        }
    }
}
