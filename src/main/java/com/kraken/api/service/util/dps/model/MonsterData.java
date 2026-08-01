package com.kraken.api.service.util.dps.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A monster's combat stats loaded from the bundled OSRS wiki monster data, plus a small
 * set of mutable inputs (current hp, defence reductions) that affect the calculation.
 */
@Data
@NoArgsConstructor
public class MonsterData {

    @Data
    @NoArgsConstructor
    public static class Skills {
        private int atk;
        private int def;
        private int hp;
        private int magic;
        private int ranged;
        private int str;
    }

    @Data
    @NoArgsConstructor
    public static class Offensive {
        private int atk;
        private int magic;
        private int magic_str;
        private int ranged;
        private int ranged_str;
        private int str;
    }

    @Data
    @NoArgsConstructor
    public static class Defensive {
        private int flat_armour;
        private int stab;
        private int slash;
        private int crush;
        private int magic;
        private int light;
        private int standard;
        private int heavy;
    }

    @Data
    @NoArgsConstructor
    public static class Weakness {
        /** Elemental weakness: "air", "water", "earth" or "fire". */
        private String element;
        /** Bonus severity as a percent, e.g. 50 for Zulrah's fire weakness. */
        private int severity;
    }

    /**
     * Defence reductions applied to the monster before the fight (special attacks and spells
     * that drain stats). Mirrors the wiki calculator's defence reduction inputs.
     */
    @Data
    @NoArgsConstructor
    public static class DefenceReductions {
        private boolean vulnerability;
        private boolean accursed;
        private int elderMaul;
        private int dwh;
        private int arclight;
        private int emberlight;
        private int bgs;
        private int tonalztic;
        private int seercull;
        private int ayak;
    }

    private int id;
    private String name;
    private String version;
    private int size;
    private int speed;
    /** The monster's own attack style key, e.g. "ranged" for Zulrah's serpentine form. */
    private String style;
    private Skills skills = new Skills();
    private Offensive offensive = new Offensive();
    private Defensive defensive = new Defensive();
    private List<String> attributes = new ArrayList<>();
    private Weakness weakness;
    /** Burn immunity tier: "Weak", "Normal", "Strong" or null. */
    private String burnImmunity;
    private boolean slayerMonster;

    /** Monster current hp; 0 means "full hp". Affects ruby bolt (e) effect damage. */
    private transient int currentHp;
    private transient DefenceReductions defenceReductions = new DefenceReductions();

    /**
     * Checks whether this monster has the given attribute.
     * @param attribute The attribute to check
     * @return True when the monster has that attribute
     */
    public boolean hasAttribute(MonsterAttributeType attribute) {
        if (attribute == null || attributes == null) return false;
        return attributes.contains(attribute.getKey());
    }

    /**
     * Whether this monster is any tier of vampyre.
     * @return True for vampyre monsters
     */
    public boolean isVampyre() {
        return hasAttribute(MonsterAttributeType.VAMPYRE_1)
                || hasAttribute(MonsterAttributeType.VAMPYRE_2)
                || hasAttribute(MonsterAttributeType.VAMPYRE_3);
    }

    /**
     * Gets the monster's current hp, defaulting to full hp when unset.
     * @return int the monster's current hitpoints
     */
    public int getCurrentHpOrFull() {
        if (currentHp <= 0 || currentHp > skills.getHp()) return skills.getHp();
        return currentHp;
    }

    /**
     * Creates a deep copy of this monster so calculations can mutate stats
     * (e.g. defence reductions) without affecting the cached data.
     * @return MonsterData copy of this monster
     */
    public MonsterData copy() {
        MonsterData m = new MonsterData();
        m.id = id;
        m.name = name;
        m.version = version;
        m.size = size;
        m.speed = speed;
        m.style = style;
        m.skills = new Skills();
        m.skills.setAtk(skills.getAtk());
        m.skills.setDef(skills.getDef());
        m.skills.setHp(skills.getHp());
        m.skills.setMagic(skills.getMagic());
        m.skills.setRanged(skills.getRanged());
        m.skills.setStr(skills.getStr());
        m.offensive = new Offensive();
        m.offensive.setAtk(offensive.getAtk());
        m.offensive.setMagic(offensive.getMagic());
        m.offensive.setMagic_str(offensive.getMagic_str());
        m.offensive.setRanged(offensive.getRanged());
        m.offensive.setRanged_str(offensive.getRanged_str());
        m.offensive.setStr(offensive.getStr());
        m.defensive = new Defensive();
        m.defensive.setFlat_armour(defensive.getFlat_armour());
        m.defensive.setStab(defensive.getStab());
        m.defensive.setSlash(defensive.getSlash());
        m.defensive.setCrush(defensive.getCrush());
        m.defensive.setMagic(defensive.getMagic());
        m.defensive.setLight(defensive.getLight());
        m.defensive.setStandard(defensive.getStandard());
        m.defensive.setHeavy(defensive.getHeavy());
        m.attributes = attributes == null ? new ArrayList<>() : new ArrayList<>(attributes);
        if (weakness != null) {
            m.weakness = new Weakness();
            m.weakness.setElement(weakness.getElement());
            m.weakness.setSeverity(weakness.getSeverity());
        }
        m.burnImmunity = burnImmunity;
        m.slayerMonster = slayerMonster;
        m.currentHp = currentHp;
        m.defenceReductions = defenceReductions;
        return m;
    }
}
