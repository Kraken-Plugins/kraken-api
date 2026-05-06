package com.kraken.api.service.magic.rune;

import com.kraken.api.Context;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.RuneLite;

import java.util.*;

@AllArgsConstructor
public enum ElementalStaff {

    STAFF_OF_AIR(1381, Rune.AIR),
    STAFF_OF_WATER(1383, Rune.WATER),
    STAFF_OF_EARTH(1385, Rune.EARTH),
    STAFF_OF_FIRE(1387, Rune.FIRE),

    AIR_BATTLESTAFF(1397, Rune.AIR),
    WATER_BATTLESTAFF(1395, Rune.WATER),
    EARTH_BATTLESTAFF(1399, Rune.EARTH),
    FIRE_BATTLESTAFF(1393, Rune.FIRE),

    MYSTIC_AIR_STAFF(1405, Rune.AIR),
    MYSTIC_WATER_STAFF(1403, Rune.WATER),
    MYSTIC_EARTH_STAFF(1407, Rune.EARTH),
    MYSTIC_FIRE_STAFF(1401, Rune.FIRE),

    LAVA_BATTLESTAFF(3053, Rune.LAVA),
    MUD_BATTLESTAFF(6562, Rune.MUD),
    STEAM_BATTLESTAFF(11787, Rune.STEAM),
    SMOKE_BATTLESTAFF(11998, Rune.SMOKE),
    MIST_BATTLESTAFF(20730, Rune.MIST),
    DUST_BATTLESTAFF(20736, Rune.DUST),

    MYSTIC_LAVA_STAFF(3054, Rune.LAVA),
    MYSTIC_MUD_STAFF(6563, Rune.MUD),
    MYSTIC_STEAM_STAFF(11789, Rune.STEAM),
    MYSTIC_SMOKE_STAFF(12000, Rune.SMOKE),
    MYSTIC_MIST_STAFF(20733, Rune.MIST),
    MYSTIC_DUST_STAFF(20739, Rune.DUST),

    BRYOPHYTA_STAFF(22368, Rune.NATURE),
    KODAI_WAND(21006, Rune.WATER);

    @Getter
    private final int itemId;

    @Getter
    private final Rune rune;

    private static final Context ctx = RuneLite.getInjector().getInstance(Context.class);

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

        int staffId = ctx.equipment().inSlot(EquipmentInventorySlot.WEAPON).getId();

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
