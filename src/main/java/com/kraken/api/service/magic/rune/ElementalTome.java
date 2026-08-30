package com.kraken.api.service.magic.rune;

import com.kraken.api.Context;
import com.kraken.api.core.Services;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.RuneLite;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.kraken.api.query.equipment.EquipmentEntity;

@AllArgsConstructor
public enum ElementalTome {
    TOME_OF_FIRE(20714, Rune.FIRE),
    TOME_OF_WATER(25574, Rune.WATER),
    TOME_OF_EARTH(30064, Rune.EARTH);

    @Getter
    private final int itemId;

    @Getter
    private final Rune rune;

    private static Context ctx() {
        return Services.context();
    }

    /**
     * Retrieves the {@code ElementalTome} associated with the specified item ID.
     * <p>
     * This method searches through all available {@code ElementalTome} values and compares their item IDs
     * to the provided {@code id}. If a match is found, the corresponding {@code ElementalTome} is returned.
     * Otherwise, {@code null} is returned if no matching {@code ElementalTome} exists.
     * </p>
     *
     * @param id The item ID used to identify the corresponding {@code ElementalTome}.
     *           This value represents the unique identifier assigned to an {@code ElementalTome}.
     *
     * @return The {@code ElementalTome} associated with the specified {@code id}, or {@code null} if no match is found.
     */
    public static ElementalTome forId(int id) {
        for(ElementalTome tome : ElementalTome.values()) {
            if (tome.getItemId() == id) {
                return tome;
            }
        }

        return null;
    }

    /**
     * Retrieves a mapping of runes currently provided by the equipped elemental tome.
     * <p>
     * This method checks the player's equipment slot for an equipped elemental tome
     * (e.g., Tome of Fire, Tome of Water, etc.). If an elemental tome is found in the shield slot,
     * it retrieves the associated rune and maps it with an effectively infinite supply
     * (represented by {@link Integer#MAX_VALUE}).
     * <p>
     * If no elemental tome is equipped or the equipped item does not correspond to a recognized tome,
     * an empty map is returned.
     * </p>
     *
     * <p><b>Note:</b> This method currently only supports single-element elemental tomes.
     * Future expansions may include support for combination runes provided by tomes.</p>
     *
     * @return A {@code Map<Rune, Integer>} where the key represents the {@code Rune}
     * and the value represents the quantity (always {@link Integer#MAX_VALUE} for valid matches).
     * Returns an empty map if no matching elemental tome is equipped or recognized.
     */
    public static Map<Rune, Integer> getRunes() {
        Map<Rune, Integer> runes = new HashMap<>();

        int tomeId = ctx().equipment().inSlot(EquipmentInventorySlot.SHIELD)
                .map(EquipmentEntity::getId).orElse(-1);

        if(tomeId == -1) {
            return Collections.emptyMap();
        }

        ElementalTome tome = ElementalTome.forId(tomeId);

        if(tome == null) {
            return Collections.emptyMap();
        }

        // TODO May have to expand this in the future if there are tomes supporting combination runes.
        runes.put(tome.getRune(), Integer.MAX_VALUE);
        return runes;
    }
}
