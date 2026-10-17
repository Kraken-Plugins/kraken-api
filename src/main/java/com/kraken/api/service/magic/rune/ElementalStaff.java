package com.kraken.api.service.magic.rune;

import com.kraken.api.Context;
import com.kraken.api.core.Services;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.RuneLite;

import java.util.*;
import com.kraken.api.query.equipment.EquipmentEntity;

/** Staffs and wands that provide runes while they are worn in the weapon slot. */
@AllArgsConstructor
public enum ElementalStaff {

    /** Staff of air, which provides air runes. */
    STAFF_OF_AIR(1381, Rune.AIR),
    /** Staff of water, which provides water runes. */
    STAFF_OF_WATER(1383, Rune.WATER),
    /** Staff of earth, which provides earth runes. */
    STAFF_OF_EARTH(1385, Rune.EARTH),
    /** Staff of fire, which provides fire runes. */
    STAFF_OF_FIRE(1387, Rune.FIRE),

    /** Air battlestaff, which provides air runes. */
    AIR_BATTLESTAFF(1397, Rune.AIR),
    /** Water battlestaff, which provides water runes. */
    WATER_BATTLESTAFF(1395, Rune.WATER),
    /** Earth battlestaff, which provides earth runes. */
    EARTH_BATTLESTAFF(1399, Rune.EARTH),
    /** Fire battlestaff, which provides fire runes. */
    FIRE_BATTLESTAFF(1393, Rune.FIRE),

    /** Mystic air staff, which provides air runes. */
    MYSTIC_AIR_STAFF(1405, Rune.AIR),
    /** Mystic water staff, which provides water runes. */
    MYSTIC_WATER_STAFF(1403, Rune.WATER),
    /** Mystic earth staff, which provides earth runes. */
    MYSTIC_EARTH_STAFF(1407, Rune.EARTH),
    /** Mystic fire staff, which provides fire runes. */
    MYSTIC_FIRE_STAFF(1401, Rune.FIRE),

    /** Lava battlestaff, which provides earth and fire runes. */
    LAVA_BATTLESTAFF(3053, Rune.LAVA),
    /** Mud battlestaff, which provides water and earth runes. */
    MUD_BATTLESTAFF(6562, Rune.MUD),
    /** Steam battlestaff, which provides water and fire runes. */
    STEAM_BATTLESTAFF(11787, Rune.STEAM),
    /** Smoke battlestaff, which provides air and fire runes. */
    SMOKE_BATTLESTAFF(11998, Rune.SMOKE),
    /** Mist battlestaff, which provides air and water runes. */
    MIST_BATTLESTAFF(20730, Rune.MIST),
    /** Dust battlestaff, which provides air and earth runes. */
    DUST_BATTLESTAFF(20736, Rune.DUST),

    /** Mystic lava staff, which provides earth and fire runes. */
    MYSTIC_LAVA_STAFF(3054, Rune.LAVA),
    /** Mystic mud staff, which provides water and earth runes. */
    MYSTIC_MUD_STAFF(6563, Rune.MUD),
    /** Mystic steam staff, which provides water and fire runes. */
    MYSTIC_STEAM_STAFF(11789, Rune.STEAM),
    /** Mystic smoke staff, which provides air and fire runes. */
    MYSTIC_SMOKE_STAFF(12000, Rune.SMOKE),
    /** Mystic mist staff, which provides air and water runes. */
    MYSTIC_MIST_STAFF(20733, Rune.MIST),
    /** Mystic dust staff, which provides air and earth runes. */
    MYSTIC_DUST_STAFF(20739, Rune.DUST),

    /** Bryophyta's staff, which provides nature runes. */
    BRYOPHYTA_STAFF(22368, Rune.NATURE),
    /** Kodai wand, which provides water runes. */
    KODAI_WAND(21006, Rune.WATER);

    @Getter
    private final int itemId;

    @Getter
    private final Rune rune;

    private static Context ctx() {
        return Services.context();
    }

    /**
     * Retrieves an {@code ElementalStaff} corresponding to the provided item ID.
     *
     * <p>This method iterates through all {@code ElementalStaff} enum values and
     * matches the given ID against the {@code itemId} field of each staff. If a match
     * is found, the corresponding {@code ElementalStaff} is returned.</p>
     * @param id The id of the {@code ElementalStaff} to match.
     * @return The {@code ElementalStaff} corresponding to the provided item ID,
     */
    public static ElementalStaff forId(int id) {
        for(ElementalStaff staff : ElementalStaff.values()) {
            if (staff.getItemId() == id) {
                return staff;
            }
        }

        return null;
    }

    /**
     * Retrieves a mapping of runes provided by the currently equipped staff and their quantities.
     *
     * <p>This method determines the equipped weapon in the {@literal @literal WEAPON} equipment slot
     * and identifies whether it corresponds to an {@code ElementalStaff}. If the equipped staff
     * provides runes (either a single rune or base runes for combination runes), it returns a
     * map where the keys are the runes and the values are set to {@code Integer.MAX_VALUE},
     * indicating unlimited availability of those runes.</p>
     *
     * <ul>
     * <li>If no staff is equipped in the weapon slot, the method returns an empty map.</li>
     * <li>If the equipped weapon does not correspond to an {@code ElementalStaff}, the method
     * returns an empty map.</li>
     * <li>If the staff provides a single rune, the map contains the rune with unlimited quantity.</li>
     * <li>If the staff provides combination runes, the map contains the base runes with unlimited quantity.</li>
     * </ul>
     *
     * @return A {@code Map<Rune, Integer>} where the keys represent the runes provided by the
     * currently equipped staff and the values represent their quantities (or an empty map if no
     * applicable staff is equipped).
     */
    public static Map<Rune, Integer> getRunes() {
        Map<Rune, Integer> runes = new HashMap<>();

        int staffId = ctx().equipment().inSlot(EquipmentInventorySlot.WEAPON)
                .map(EquipmentEntity::getId).orElse(-1);

        if(staffId == -1) {
            return Collections.emptyMap();
        }

        ElementalStaff staff = ElementalStaff.forId(staffId);

        if(staff == null) {
            return Collections.emptyMap();
        }

        if(!staff.getRune().isComboRune()) {
            runes.put(staff.getRune(), Integer.MAX_VALUE);
            return runes;
        } else {
            for(Rune baseRune : staff.getRune().getBaseRunes()) {
                runes.put(baseRune, Integer.MAX_VALUE);
            }
        }

        return runes;
    }
}
