package com.kraken.api.service.util.dps.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A piece of equipment with its full combat stats, loaded from the bundled OSRS wiki
 * equipment data. Field structure intentionally mirrors the JSON so Gson can map it directly.
 */
@Data
@NoArgsConstructor
public class EquipmentItem {

    /**
     * Strength-type bonuses of an item: melee strength, ranged strength, magic damage
     * (in tenths of a percent) and prayer bonus.
     */
    @Data
    @NoArgsConstructor
    public static class Bonuses {
        private int str;
        private int ranged_str;
        private int magic_str;
        private int prayer;
    }

    /**
     * Accuracy (offensive) or defensive bonuses of an item, one value per combat style type.
     */
    @Data
    @NoArgsConstructor
    public static class Styles {
        private int stab;
        private int slash;
        private int crush;
        private int magic;
        private int ranged;
    }

    private int id;
    private String name;
    private String version;
    private String slot;
    private int speed;
    private String category;
    private boolean isTwoHanded;
    private Bonuses bonuses = new Bonuses();
    private Styles offensive = new Styles();
    private Styles defensive = new Styles();

    public GearSlot getGearSlot() {
        return GearSlot.fromWikiName(slot);
    }

    public WeaponCategory getWeaponCategory() {
        return WeaponCategory.fromWikiName(category);
    }

    /**
     * Gets this item's offensive accuracy bonus for the given combat style type.
     * @param type The style type to look up
     * @return int the accuracy bonus for that style
     */
    public int offensiveBonus(CombatStyleType type) {
        switch (type) {
            case STAB: return offensive.getStab();
            case SLASH: return offensive.getSlash();
            case CRUSH: return offensive.getCrush();
            case MAGIC: return offensive.getMagic();
            case RANGED: return offensive.getRanged();
            default: return 0;
        }
    }

    /**
     * Classifies this item into a coarse gear category based on its stats. Weapons are
     * classified by their weapon category; armour by comparing its offensive bonuses per style.
     * For example the Masori body (+43 ranged accuracy, -4 magic) is RANGED gear, while a
     * piece with comparable positive bonuses in several styles is HYBRID.
     * @return GearCategory for this item
     */
    public GearCategory getGearCategory() {
        if (getGearSlot() == GearSlot.WEAPON) {
            WeaponCategory cat = getWeaponCategory();
            if (cat.isMagicWeapon()) return GearCategory.MAGIC;
            RangedDamageType ranged = cat.getRangedDamageType();
            if (ranged == RangedDamageType.MIXED) return GearCategory.HYBRID;
            if (ranged != null) return GearCategory.RANGED;
            return GearCategory.MELEE;
        }

        int melee = Math.max(offensive.getStab(), Math.max(offensive.getSlash(), offensive.getCrush())) + bonuses.getStr();
        int ranged = offensive.getRanged() + bonuses.getRanged_str();
        // magic_str is in tenths of a percent; scale it into rough parity with accuracy points
        int magic = offensive.getMagic() + bonuses.getMagic_str() / 10;

        int best = Math.max(melee, Math.max(ranged, magic));
        if (best <= 0) return GearCategory.NONE;

        int positiveStyles = 0;
        if (melee > 0) positiveStyles++;
        if (ranged > 0) positiveStyles++;
        if (magic > 0) positiveStyles++;

        if (positiveStyles > 1) {
            // hybrid only when the runner-up style is a meaningful fraction of the best
            int second = melee + ranged + magic - best - Math.min(melee, Math.min(ranged, magic));
            if (second * 2 >= best) return GearCategory.HYBRID;
        }

        if (best == melee) return GearCategory.MELEE;
        if (best == ranged) return GearCategory.RANGED;
        return GearCategory.MAGIC;
    }
}
