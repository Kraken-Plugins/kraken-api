package com.kraken.api.service.magic.spellbook;

import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.rune.Rune;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

import java.util.Map;

/** The spells of the Ancient Magicks spellbook. */
@Getter
public enum Ancient implements CastableSpell {
    /** Edgeville Home Teleport, which teleports the player to Edgeville. */
    EDGEVILLE_HOME_TELEPORT(
            0,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_ZAROS,
            0,
            Map.of()
    ),

    /** Minigame Teleport, which opens the list of minigame destinations. */
    MINIGAME_TELEPORT(
            0,
            InterfaceID.MagicSpellbook.TELEPORT_MINIGAME_STANDARD,
            1,
            Map.of()
    ),

    // Rush Spells
    /** Smoke Rush, an Ancient Magicks attack spell. */
    SMOKE_RUSH(
            50,
            InterfaceID.MagicSpellbook.SMOKE_RUSH,
            2,
            Map.of(Rune.AIR, 1, Rune.FIRE, 1, Rune.CHAOS, 2, Rune.DEATH, 2)
    ),
    /** Shadow Rush, an Ancient Magicks attack spell. */
    SHADOW_RUSH(
            52,
            InterfaceID.MagicSpellbook.SHADOW_RUSH,
            3,
            Map.of(Rune.AIR, 1, Rune.CHAOS, 2, Rune.DEATH, 2, Rune.SOUL, 1)
    ),
    /** Blood Rush, an Ancient Magicks attack spell. */
    BLOOD_RUSH(
            56,
            InterfaceID.MagicSpellbook.BLOOD_RUSH,
            5,
            Map.of(Rune.CHAOS, 2, Rune.DEATH, 2, Rune.BLOOD, 1)
    ),
    /** Ice Rush, an Ancient Magicks attack spell. */
    ICE_RUSH(
            58,
            InterfaceID.MagicSpellbook.ICE_RUSH,
            6,
            Map.of(Rune.WATER, 2, Rune.CHAOS, 2, Rune.DEATH, 2)
    ),

    // Burst Spells
    /** Smoke Burst, an Ancient Magicks attack spell. */
    SMOKE_BURST(
            62,
            InterfaceID.MagicSpellbook.SMOKE_BURST,
            8,
            Map.of(Rune.AIR, 2, Rune.FIRE, 2, Rune.CHAOS, 4, Rune.DEATH, 2)
    ),
    /** Shadow Burst, an Ancient Magicks attack spell. */
    SHADOW_BURST(
            64,
            InterfaceID.MagicSpellbook.SHADOW_BURST,
            9,
            Map.of(Rune.AIR, 1, Rune.CHAOS, 4, Rune.DEATH, 2, Rune.SOUL, 2)
    ),
    /** Blood Burst, an Ancient Magicks attack spell. */
    BLOOD_BURST(
            68,
            InterfaceID.MagicSpellbook.BLOOD_BURST,
            11,
            Map.of(Rune.CHAOS, 2, Rune.DEATH, 4, Rune.BLOOD, 2)
    ),
    /** Ice Burst, an Ancient Magicks attack spell. */
    ICE_BURST(
            70,
            InterfaceID.MagicSpellbook.ICE_BURST,
            12,
            Map.of(Rune.WATER, 4, Rune.CHAOS, 4, Rune.DEATH, 2)
    ),

    // Blitz Spells
    /** Smoke Blitz, an Ancient Magicks attack spell. */
    SMOKE_BLITZ(
            74,
            InterfaceID.MagicSpellbook.SMOKE_BLITZ,
            14,
            Map.of(Rune.AIR, 2, Rune.FIRE, 2, Rune.DEATH, 2, Rune.BLOOD, 2)
    ),
    /** Shadow Blitz, an Ancient Magicks attack spell. */
    SHADOW_BLITZ(
            76,
            InterfaceID.MagicSpellbook.SHADOW_BLITZ,
            15,
            Map.of(Rune.AIR, 2, Rune.DEATH, 2, Rune.BLOOD, 2, Rune.SOUL, 2)
    ),
    /** Blood Blitz, an Ancient Magicks attack spell. */
    BLOOD_BLITZ(
            80,
            InterfaceID.MagicSpellbook.BLOOD_BLITZ,
            17,
            Map.of(Rune.DEATH, 2, Rune.BLOOD, 4)
    ),
    /** Ice Blitz, an Ancient Magicks attack spell. */
    ICE_BLITZ(
            82,
            InterfaceID.MagicSpellbook.ICE_BLITZ,
            18,
            Map.of(Rune.WATER, 3, Rune.DEATH, 2, Rune.BLOOD, 2)
    ),

    // Barrage Spells
    /** Smoke Barrage, an Ancient Magicks attack spell. */
    SMOKE_BARRAGE(
            86,
            InterfaceID.MagicSpellbook.SMOKE_BARRAGE,
            21,
            Map.of(Rune.AIR, 4, Rune.FIRE, 4, Rune.DEATH, 4, Rune.BLOOD, 2)
    ),
    /** Shadow Barrage, an Ancient Magicks attack spell. */
    SHADOW_BARRAGE(
            88,
            InterfaceID.MagicSpellbook.SHADOW_BARRAGE,
            22,
            Map.of(Rune.AIR, 4, Rune.DEATH, 4, Rune.BLOOD, 2, Rune.SOUL, 3)
    ),
    /** Blood Barrage, an Ancient Magicks attack spell. */
    BLOOD_BARRAGE(
            92,
            InterfaceID.MagicSpellbook.BLOOD_BARRAGE,
            24,
            Map.of(Rune.DEATH, 4, Rune.BLOOD, 4, Rune.SOUL, 1)
    ),
    /** Ice Barrage, an Ancient Magicks attack spell. */
    ICE_BARRAGE(
            94,
            InterfaceID.MagicSpellbook.ICE_BARRAGE,
            25,
            Map.of(Rune.WATER, 6, Rune.DEATH, 4, Rune.BLOOD, 2)
    ),

    // Teleport Spells
    /** Paddewwa Teleport, which teleports the player to Paddewwa. */
    PADDEWWA_TELEPORT(
            54,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT1,
            4,
            Map.of(Rune.AIR, 1, Rune.FIRE, 1, Rune.LAW, 2)
    ),
    /** Senntisten Teleport, which teleports the player to Senntisten. */
    SENNTISTEN_TELEPORT(
            60,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT2,
            7,
            Map.of(Rune.LAW, 2, Rune.SOUL, 1)
    ),
    /** Ghorrock Teleport, which teleports the player to Ghorrock. */
    GHORROCK_TELEPORT(
            96,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT8,
            26,
            Map.of(Rune.WATER, 8, Rune.LAW, 2)
    );

    private final int level;
    private final int widget;
    private final int index;
    private final Map<Rune, Integer> runeRequirement;
    private final Spellbook spellbook = Spellbook.ANCIENT;
    private final String name;

    Ancient(int level, int widget, int index, Map<Rune, Integer> runeRequirement) {
        this.level = level;
        this.widget = widget;
        this.index = index;
        this.runeRequirement = runeRequirement;
        this.name = this.name();
    }

    @Override
    public int getAction() {
        return 1;
    }
}
