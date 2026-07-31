package com.kraken.api.service.util.dps.model;

import net.runelite.api.EquipmentInventorySlot;

/**
 * The eleven equipment slots the DPS calculator considers, keyed by the wiki slot names
 * used in the bundled equipment data.
 */
public enum GearSlot {
    HEAD("head"),
    CAPE("cape"),
    NECK("neck"),
    AMMO("ammo"),
    WEAPON("weapon"),
    BODY("body"),
    SHIELD("shield"),
    LEGS("legs"),
    HANDS("hands"),
    FEET("feet"),
    RING("ring");

    private final String wikiName;

    GearSlot(String wikiName) {
        this.wikiName = wikiName;
    }

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
