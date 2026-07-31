package com.kraken.api.service.util.dps.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.kraken.api.service.util.dps.model.CombatStance.*;
import static com.kraken.api.service.util.dps.model.CombatStyleType.*;

/**
 * The category of a weapon which determines the attack styles available on its combat tab.
 * Categories mirror the OSRS wiki's "Combat style" property.
 * @see <a href="https://oldschool.runescape.wiki/w/Property:Combat_style">wiki combat styles</a>
 */
public enum WeaponCategory {
    NONE(""),
    TWO_HANDED_SWORD("2h Sword"),
    AXE("Axe"),
    BANNER("Banner"),
    BLADED_STAFF("Bladed Staff"),
    BLASTER("Blaster"),
    BLUDGEON("Bludgeon"),
    BLUNT("Blunt"),
    BOW("Bow"),
    BULWARK("Bulwark"),
    CHINCHOMPA("Chinchompas"),
    CLAW("Claw"),
    CROSSBOW("Crossbow"),
    DAGGER("Dagger"),
    FLAIL("Flail"),
    GUN("Gun"),
    MULTI_MELEE("Multi-Melee"),
    PARTISAN("Partisan"),
    PICKAXE("Pickaxe"),
    POLEARM("Polearm"),
    POLESTAFF("Polestaff"),
    POWERED_STAFF("Powered Staff"),
    POWERED_WAND("Powered Wand"),
    SALAMANDER("Salamander"),
    SCYTHE("Scythe"),
    SLASH_SWORD("Slash Sword"),
    SPEAR("Spear"),
    SPIKED("Spiked"),
    STAB_SWORD("Stab Sword"),
    STAFF("Staff"),
    THROWN("Thrown"),
    UNARMED("Unarmed"),
    WHIP("Whip");

    private final String wikiName;

    WeaponCategory(String wikiName) {
        this.wikiName = wikiName;
    }

    public String getWikiName() {
        return wikiName;
    }

    /**
     * Resolves a wiki category name (e.g. "Slash Sword") into a WeaponCategory.
     * @param name The wiki category name
     * @return WeaponCategory, defaulting to NONE for unknown or empty names
     */
    public static WeaponCategory fromWikiName(String name) {
        if (name == null || name.isEmpty()) return NONE;
        for (WeaponCategory c : values()) {
            if (c.wikiName.equalsIgnoreCase(name)) return c;
        }
        return NONE;
    }

    /**
     * Whether this category is a magic weapon (can autocast or has a built-in spell).
     * @return True for staves, powered staves/wands, bladed staves and polestaves
     */
    public boolean isMagicWeapon() {
        return this == STAFF || this == POWERED_STAFF || this == POWERED_WAND || this == BLADED_STAFF || this == POLESTAFF;
    }

    /**
     * Gets the class of ranged damage dealt by weapons of this category.
     * @return RangedDamageType or null when this category is not a ranged weapon
     */
    public RangedDamageType getRangedDamageType() {
        switch (this) {
            case THROWN:
                return RangedDamageType.LIGHT;
            case BOW:
                return RangedDamageType.STANDARD;
            case CROSSBOW:
            case CHINCHOMPA:
                return RangedDamageType.HEAVY;
            case SALAMANDER:
                return RangedDamageType.MIXED;
            default:
                return null;
        }
    }

    /**
     * Gets the attack style options available on the combat tab for weapons of this category,
     * in the same order they appear in-game.
     * @return List of attack styles for this weapon category
     */
    public List<AttackStyle> getAttackStyles() {
        switch (this) {
            case TWO_HANDED_SWORD:
                return Arrays.asList(
                        new AttackStyle("Chop", SLASH, ACCURATE),
                        new AttackStyle("Slash", SLASH, AGGRESSIVE),
                        new AttackStyle("Smash", CRUSH, AGGRESSIVE),
                        new AttackStyle("Block", SLASH, DEFENSIVE));
            case BANNER:
                return Arrays.asList(
                        new AttackStyle("Lunge", STAB, ACCURATE),
                        new AttackStyle("Swipe", SLASH, AGGRESSIVE),
                        new AttackStyle("Pound", CRUSH, CONTROLLED),
                        new AttackStyle("Block", STAB, DEFENSIVE));
            case BLADED_STAFF:
                return Arrays.asList(
                        new AttackStyle("Jab", STAB, ACCURATE),
                        new AttackStyle("Swipe", SLASH, AGGRESSIVE),
                        new AttackStyle("Fend", CRUSH, DEFENSIVE),
                        new AttackStyle("Spell", MAGIC, DEFENSIVE_AUTOCAST),
                        new AttackStyle("Spell", MAGIC, AUTOCAST));
            case BOW:
            case CROSSBOW:
            case THROWN:
                return Arrays.asList(
                        new AttackStyle("Accurate", RANGED, ACCURATE),
                        new AttackStyle("Rapid", RANGED, RAPID),
                        new AttackStyle("Longrange", RANGED, LONGRANGE));
            case GUN:
                return Collections.singletonList(new AttackStyle("Kick", CRUSH, AGGRESSIVE));
            case BULWARK:
                return Collections.singletonList(new AttackStyle("Pummel", CRUSH, ACCURATE));
            case MULTI_MELEE:
                return Arrays.asList(
                        new AttackStyle("Poke", STAB, ACCURATE),
                        new AttackStyle("Slash", SLASH, AGGRESSIVE),
                        new AttackStyle("Pound", CRUSH, AGGRESSIVE),
                        new AttackStyle("Block", SLASH, DEFENSIVE));
            case PARTISAN:
                return Arrays.asList(
                        new AttackStyle("Stab", STAB, ACCURATE),
                        new AttackStyle("Lunge", STAB, AGGRESSIVE),
                        new AttackStyle("Pound", CRUSH, AGGRESSIVE),
                        new AttackStyle("Block", STAB, DEFENSIVE));
            case PICKAXE:
                return Arrays.asList(
                        new AttackStyle("Spike", STAB, ACCURATE),
                        new AttackStyle("Impale", STAB, AGGRESSIVE),
                        new AttackStyle("Smash", CRUSH, AGGRESSIVE),
                        new AttackStyle("Block", STAB, DEFENSIVE));
            case POLEARM:
                return Arrays.asList(
                        new AttackStyle("Jab", STAB, CONTROLLED),
                        new AttackStyle("Swipe", SLASH, AGGRESSIVE),
                        new AttackStyle("Fend", STAB, DEFENSIVE));
            case POWERED_STAFF:
            case POWERED_WAND:
                return Arrays.asList(
                        new AttackStyle("Accurate", MAGIC, ACCURATE),
                        new AttackStyle("Accurate", MAGIC, ACCURATE),
                        new AttackStyle("Longrange", MAGIC, LONGRANGE));
            case SALAMANDER:
                return Arrays.asList(
                        new AttackStyle("Scorch", SLASH, AGGRESSIVE),
                        new AttackStyle("Flare", RANGED, RAPID),
                        new AttackStyle("Blaze", MAGIC, DEFENSIVE));
            case CHINCHOMPA:
                return Arrays.asList(
                        new AttackStyle("Short fuse", RANGED, ACCURATE),
                        new AttackStyle("Medium fuse", RANGED, RAPID),
                        new AttackStyle("Long fuse", RANGED, LONGRANGE));
            case CLAW:
            case SLASH_SWORD:
                return Arrays.asList(
                        new AttackStyle("Chop", SLASH, ACCURATE),
                        new AttackStyle("Slash", SLASH, AGGRESSIVE),
                        new AttackStyle("Lunge", STAB, CONTROLLED),
                        new AttackStyle("Block", SLASH, DEFENSIVE));
            case BLUDGEON:
                return Arrays.asList(
                        new AttackStyle("Pound", CRUSH, AGGRESSIVE),
                        new AttackStyle("Pummel", CRUSH, AGGRESSIVE),
                        new AttackStyle("Smash", CRUSH, AGGRESSIVE));
            case BLUNT:
                return Arrays.asList(
                        new AttackStyle("Pound", CRUSH, ACCURATE),
                        new AttackStyle("Pummel", CRUSH, AGGRESSIVE),
                        new AttackStyle("Block", CRUSH, DEFENSIVE));
            case POLESTAFF:
                return Arrays.asList(
                        new AttackStyle("Bash", CRUSH, ACCURATE),
                        new AttackStyle("Pound", CRUSH, AGGRESSIVE),
                        new AttackStyle("Block", CRUSH, DEFENSIVE));
            case SPIKED:
                return Arrays.asList(
                        new AttackStyle("Pound", CRUSH, ACCURATE),
                        new AttackStyle("Pummel", CRUSH, AGGRESSIVE),
                        new AttackStyle("Spike", STAB, CONTROLLED),
                        new AttackStyle("Block", CRUSH, DEFENSIVE));
            case STAFF:
                return Arrays.asList(
                        new AttackStyle("Bash", CRUSH, ACCURATE),
                        new AttackStyle("Pound", CRUSH, AGGRESSIVE),
                        new AttackStyle("Focus", CRUSH, DEFENSIVE),
                        new AttackStyle("Spell", MAGIC, DEFENSIVE_AUTOCAST),
                        new AttackStyle("Spell", MAGIC, AUTOCAST));
            case AXE:
                return Arrays.asList(
                        new AttackStyle("Chop", SLASH, ACCURATE),
                        new AttackStyle("Hack", SLASH, AGGRESSIVE),
                        new AttackStyle("Smash", CRUSH, AGGRESSIVE),
                        new AttackStyle("Block", SLASH, DEFENSIVE));
            case SCYTHE:
                return Arrays.asList(
                        new AttackStyle("Reap", SLASH, ACCURATE),
                        new AttackStyle("Chop", SLASH, AGGRESSIVE),
                        new AttackStyle("Jab", CRUSH, AGGRESSIVE),
                        new AttackStyle("Block", SLASH, DEFENSIVE));
            case SPEAR:
                return Arrays.asList(
                        new AttackStyle("Lunge", STAB, CONTROLLED),
                        new AttackStyle("Swipe", SLASH, CONTROLLED),
                        new AttackStyle("Pound", CRUSH, CONTROLLED),
                        new AttackStyle("Block", STAB, DEFENSIVE));
            case STAB_SWORD:
                return Arrays.asList(
                        new AttackStyle("Stab", STAB, ACCURATE),
                        new AttackStyle("Lunge", STAB, AGGRESSIVE),
                        new AttackStyle("Slash", SLASH, AGGRESSIVE),
                        new AttackStyle("Block", STAB, DEFENSIVE));
            case WHIP:
                return Arrays.asList(
                        new AttackStyle("Flick", SLASH, ACCURATE),
                        new AttackStyle("Lash", SLASH, CONTROLLED),
                        new AttackStyle("Deflect", SLASH, DEFENSIVE));
            case FLAIL:
                return Arrays.asList(
                        new AttackStyle("Chop", SLASH, ACCURATE),
                        new AttackStyle("Slash", SLASH, AGGRESSIVE),
                        new AttackStyle("Block", SLASH, DEFENSIVE));
            case NONE:
            case UNARMED:
            default:
                return Arrays.asList(
                        new AttackStyle("Punch", CRUSH, ACCURATE),
                        new AttackStyle("Kick", CRUSH, AGGRESSIVE),
                        new AttackStyle("Block", CRUSH, DEFENSIVE));
        }
    }
}
