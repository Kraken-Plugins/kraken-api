package com.kraken.api.service.util.dps.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A complete player setup for a DPS calculation: worn equipment, the selected attack style,
 * skills and boosts, prayers, spell, and situational buffs.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Loadout {

    /** Worn equipment by slot. Missing slots are treated as empty. */
    @Builder.Default
    private Map<GearSlot, EquipmentItem> equipment = new EnumMap<>(GearSlot.class);

    /** The selected attack style. When null, the calculator picks the first style of the weapon. */
    private AttackStyle style;

    /** Base skill levels (before boosts). */
    @Builder.Default
    private PlayerSkills skills = PlayerSkills.maxed();

    /** Skill boosts added on top of base levels (e.g. +19 attack from a super combat potion). */
    @Builder.Default
    private PlayerSkills boosts = PlayerSkills.none();

    /** Active offensive prayers. */
    @Builder.Default
    private List<PrayerBoost> prayers = new ArrayList<>();

    /** The spell being cast when using a cast stance, or null. */
    private SpellData spell;

    /** Whether the player is on a slayer task (enables black mask / slayer helmet bonuses). */
    private boolean onSlayerTask;

    /** Whether the player is in the wilderness (enables revenant weapon bonuses). */
    private boolean inWilderness;

    /** Whether the Forinthry Surge bonus is active (amulet of avarice vs revenants). */
    private boolean forinthrySurge;

    /** Whether the Kandarin hard diary is complete (+10% enchanted bolt proc chance). */
    @Builder.Default
    private boolean kandarinDiary = true;

    /** Whether Mark of Darkness is active (boosts demonbane spells). */
    private boolean markOfDarknessSpell;

    /** Whether the Charge spell buff is active (empowers god spells). */
    private boolean chargeSpell;

    /** Whether sunfire runes are being used for a fire spell (10% minimum hit). */
    private boolean usingSunfireRunes;

    /** Distance from the target, used only for chinchompa fuse accuracy. */
    @Builder.Default
    private int chinchompaDistance = 4;

    /** Soul stacks for the Soulreaper axe (0-5). */
    private int soulreaperStacks;

    /** The dart item id loaded in a blowpipe, used to add the dart's ranged strength. */
    private Integer blowpipeDartId;

    /** Overrides the computed attack speed in ticks when > 0. */
    private int attackSpeedOverride;

    /**
     * Gets the item worn in a slot.
     * @param slot The gear slot
     * @return EquipmentItem in that slot or null when empty
     */
    public EquipmentItem get(GearSlot slot) {
        return equipment == null ? null : equipment.get(slot);
    }

    /**
     * Gets the equipped weapon.
     * @return EquipmentItem in the weapon slot or null when unarmed
     */
    public EquipmentItem getWeapon() {
        return get(GearSlot.WEAPON);
    }

    /**
     * Creates a copy of this loadout with an independent equipment map, so the gear search
     * can swap items without mutating the original.
     * @return Loadout copy
     */
    public Loadout copy() {
        Loadout copy = toBuilder().build();
        copy.setEquipment(new EnumMap<>(GearSlot.class));
        if (equipment != null) {
            copy.getEquipment().putAll(equipment);
        }
        copy.setPrayers(new ArrayList<>(prayers == null ? new ArrayList<>() : prayers));
        return copy;
    }
}
