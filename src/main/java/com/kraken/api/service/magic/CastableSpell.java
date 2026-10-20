package com.kraken.api.service.magic;

import com.kraken.api.Context;
import com.kraken.api.core.Services;
import com.kraken.api.service.magic.rune.Rune;
import com.kraken.api.service.magic.spellbook.Spellbook;
import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.RuneLite;

import java.util.Map;

/** A spell that can be cast from one of the spellbooks. */
public interface CastableSpell {
    /**
     * Gets the option number sent with the widget click when this spell is cast. Teleport spells that have a variant,
     * such as Varrock Teleport and Grand Exchange Teleport, pick it from the matching unlock varbit.
     *
     * @return 1 for most spells, or 1, 2 or 3 for a teleport with a variant
     */
    int getAction();

    /**
     * @return the Magic level required to cast this spell
     */
    int getLevel();

    /**
     * @return the id of the spellbook widget this spell is cast from
     */
    int getWidget();

    /**
     * Gets the index of this spell in its spellbook's enum composition. Looking up that index in the enum gives the
     * spell's id.
     *
     * @return the spell's index in the spellbook enum
     */
    int getIndex();

    /**
     * @return the spellbook that contains this spell
     */
    Spellbook getSpellbook();

    /**
     * @return the name of this spell, which is the name of its enum constant
     */
    String getName();

    /**
     * @return the runes this spell consumes mapped to the quantity of each, or an empty map when it needs no runes
     */
    Map<Rune, Integer> getRuneRequirement();

    /**
     * Returns whether the client accepts this spell as castable with the currently worn weapon and shield.
     * The check runs client script 2620 on the client thread, which verifies the spell's requirements, including its runes.
     *
     * @return {@code true} if the client reports the spell as castable
     */
    default boolean isCastable() {
        Context ctx = Services.get(Context.class);
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            EnumComposition spellsEnum = client.getEnum(getSpellbook().getEnumCompositionIndex());
            int spellItemId = spellsEnum.getIntValue(getIndex());

            ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
            int weapon = -1;
            int shield = -1;

            if (equipment != null) {
                Item weaponItem = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
                Item shieldItem = equipment.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
                if (weaponItem != null) weapon = weaponItem.getId();
                if (shieldItem != null) shield = shieldItem.getId();
            }

            // Script checks that the spell being cast has the right runes to cast the spell
            client.runScript(2620, spellItemId, weapon, shield);
            return client.getIntStack()[client.getIntStackSize() - 1] == 1;
        });
    }
}
