package com.kraken.api.service.magic.spellbook;

import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.rune.Rune;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

import java.util.Map;

/** The spells of the Arceuus spellbook. */
@Getter
public enum Arceuus implements CastableSpell {
    /** Arceuus Home Teleport, which teleports the player to Arceuus. */
    ARCEUUS_HOME_TELEPORT(
            1,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_ARCEUUS,
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

    // Reanimation Spells
    /** Basic Reanimation, the first tier of the reanimation spells. */
    BASIC_REANIMATION(
            16,
            InterfaceID.MagicSpellbook.REANIMATION_BASIC,
            2,
            Map.of(Rune.BODY, 4, Rune.NATURE, 2)
    ),
    /** Adept Reanimation, the second tier of the reanimation spells. */
    ADEPT_REANIMATION(
            41,
            InterfaceID.MagicSpellbook.REANIMATION_ADEPT,
            3,
            Map.of(Rune.BODY, 4, Rune.NATURE, 3, Rune.SOUL, 1)
    ),
    /** Expert Reanimation, the third tier of the reanimation spells. */
    EXPERT_REANIMATION(
            72,
            InterfaceID.MagicSpellbook.REANIMATION_EXPERT,
            4,
            Map.of(Rune.BLOOD, 1, Rune.NATURE, 3, Rune.SOUL, 2)
    ),
    /** Master Reanimation, the highest tier of the reanimation spells. */
    MASTER_REANIMATION(
            90,
            InterfaceID.MagicSpellbook.REANIMATION_MASTER,
            5,
            Map.of(Rune.BLOOD, 2, Rune.NATURE, 4, Rune.SOUL, 4)
    ),

    // Teleport Spells
    /** Arceuus Library Teleport, which teleports the player to the Arceuus Library. */
    ARCEUUS_LIBRARY_TELEPORT(
            6,
            InterfaceID.MagicSpellbook.TELEPORT_ARCEUUS_LIBRARY,
            6,
            Map.of(Rune.EARTH, 2, Rune.LAW, 1)
    ),
    /** Draynor Manor Teleport, which teleports the player to Draynor Manor. */
    DRAYNOR_MANOR_TELEPORT(
            17,
            InterfaceID.MagicSpellbook.TELEPORT_DRAYNOR_MANOR,
            7,
            Map.of(Rune.EARTH, 1, Rune.WATER, 1, Rune.LAW, 1)
    ),
    /** Battlefront Teleport, which teleports the player to the Battlefront. */
    BATTLEFRONT_TELEPORT(
            23,
            InterfaceID.MagicSpellbook.TELEPORT_BATTLEFRONT,
            18,
            Map.of(Rune.EARTH, 1, Rune.FIRE, 1, Rune.LAW, 1)
    ),
    /** Mind Altar Teleport, which teleports the player to the Mind Altar. */
    MIND_ALTAR_TELEPORT(
            28,
            InterfaceID.MagicSpellbook.TELEPORT_MIND_ALTAR,
            8,
            Map.of(Rune.MIND, 2, Rune.LAW, 1)
    ),
    /** Respawn Teleport, which teleports the player to the Respawn Point. */
    RESPAWN_TELEPORT(
            34,
            InterfaceID.MagicSpellbook.TELEPORT_RESPAWN,
            16,
            Map.of(Rune.SOUL, 1, Rune.LAW, 1)
    ),
    /** Salve Graveyard Teleport, which teleports the player to the Salve Graveyard. */
    SALVE_GRAVEYARD_TELEPORT(
            40,
            InterfaceID.MagicSpellbook.TELEPORT_SALVE_GRAVEYARD,
            9,
            Map.of(Rune.SOUL, 2, Rune.LAW, 1)
    ),
    /** Fenkenstrain's Castle Teleport, which teleports the player to Fenkenstrain's Castle. */
    FENKENSTRAINS_CASTLE_TELEPORT(
            48,
            InterfaceID.MagicSpellbook.TELEPORT_FENKENSTRAIN_CASTLE,
            10,
            Map.of(Rune.EARTH, 1, Rune.SOUL, 1, Rune.LAW, 1)
    ),
    /** West Ardougne Teleport, which teleports the player to West Ardougne. */
    WEST_ARDOUGNE_TELEPORT(
            61,
            InterfaceID.MagicSpellbook.TELEPORT_WEST_ARDOUGNE,
            11,
            Map.of(Rune.SOUL, 2, Rune.LAW, 2)
    ),
    /** Harmony Island Teleport, which teleports the player to Harmony Island. */
    HARMONY_ISLAND_TELEPORT(
            65,
            InterfaceID.MagicSpellbook.TELEPORT_HARMONY_ISLAND,
            12,
            Map.of(Rune.NATURE, 1, Rune.SOUL, 1, Rune.LAW, 1)
    ),
    /** Cemetery Teleport, which teleports the player to the Cemetery. */
    CEMETERY_TELEPORT(
            71,
            InterfaceID.MagicSpellbook.TELEPORT_CEMETERY,
            13,
            Map.of(Rune.BLOOD, 1, Rune.SOUL, 1, Rune.LAW, 1)
    ),
    /** Barrows Teleport, which teleports the player to Barrows. */
    BARROWS_TELEPORT(
            83,
            InterfaceID.MagicSpellbook.TELEPORT_BARROWS,
            14,
            Map.of(Rune.BLOOD, 1, Rune.SOUL, 2, Rune.LAW, 2)
    ),
    /** Ape Atoll Teleport, which teleports the player to the Ape Atoll Dungeon. */
    APE_ATOLL_TELEPORT(
            90,
            InterfaceID.MagicSpellbook.TELEPORT_APE_ATOLL_DUNGEON,
            15,
            Map.of(Rune.BLOOD, 2, Rune.SOUL, 2, Rune.LAW, 2)
    ),

    // Combat Spells - Grasp
    /** Ghostly Grasp, an Arceuus attack spell. */
    GHOSTLY_GRASP(
            35,
            InterfaceID.MagicSpellbook.GHOSTLY_GRASP,
            23,
            Map.of(Rune.AIR, 4, Rune.CHAOS, 1)
    ),
    /** Skeletal Grasp, an Arceuus attack spell. */
    SKELETAL_GRASP(
            56,
            InterfaceID.MagicSpellbook.SKELETAL_GRASP,
            24,
            Map.of(Rune.EARTH, 8, Rune.DEATH, 1)
    ),
    /** Undead Grasp, an Arceuus attack spell. */
    UNDEAD_GRASP(
            79,
            InterfaceID.MagicSpellbook.UNDEAD_GRASP,
            25,
            Map.of(Rune.FIRE, 12, Rune.BLOOD, 1)
    ),

    // Combat Spells - Demonbane
    /** Inferior Demonbane, an Arceuus attack spell that is effective against demons. */
    INFERIOR_DEMONBANE(
            44,
            InterfaceID.MagicSpellbook.INFERIOR_DEMONBANE,
            19,
            Map.of(Rune.FIRE, 4, Rune.CHAOS, 1)
    ),
    /** Superior Demonbane, an Arceuus attack spell that is effective against demons. */
    SUPERIOR_DEMONBANE(
            62,
            InterfaceID.MagicSpellbook.SUPERIOR_DEMONBANE,
            20,
            Map.of(Rune.FIRE, 8, Rune.SOUL, 1)
    ),
    /** Dark Demonbane, an Arceuus attack spell that is effective against demons. */
    DARK_DEMONBANE(
            82,
            InterfaceID.MagicSpellbook.DARK_DEMONBANE,
            21,
            Map.of(Rune.FIRE, 12, Rune.SOUL, 2)
    ),

    // Combat Spells - Corruption
    /** Lesser Corruption, an Arceuus attack spell. */
    LESSER_CORRUPTION(
            64,
            InterfaceID.MagicSpellbook.LESSER_CORRUPTION,
            27,
            Map.of(Rune.DEATH, 1, Rune.SOUL, 2)
    ),
    /** Greater Corruption, an Arceuus attack spell. */
    GREATER_CORRUPTION(
            85,
            InterfaceID.MagicSpellbook.GREATER_CORRUPTION,
            28,
            Map.of(Rune.BLOOD, 1, Rune.SOUL, 3)
    ),

    // Resurrect Spells - Lesser
    /** Resurrect Lesser Ghost, which summons a lesser ghost thrall that attacks the player's target with Magic. */
    RESURRECT_LESSER_GHOST(
            38,
            InterfaceID.MagicSpellbook.RESURRECT_LESSER_GHOST,
            36,
            Map.of(Rune.AIR, 10, Rune.COSMIC, 1, Rune.MIND, 5)
    ),
    /** Resurrect Lesser Skeleton, which summons a lesser skeleton thrall that attacks the player's target with Ranged. */
    RESURRECT_LESSER_SKELETON(
            38,
            InterfaceID.MagicSpellbook.RESURRECT_LESSER_SKELETON,
            37,
            Map.of(Rune.AIR, 10, Rune.COSMIC, 1, Rune.MIND, 5)
    ),
    /** Resurrect Lesser Zombie, which summons a lesser zombie thrall that attacks the player's target with melee. */
    RESURRECT_LESSER_ZOMBIE(
            38,
            InterfaceID.MagicSpellbook.RESURRECT_LESSER_ZOMBIE,
            38,
            Map.of(Rune.AIR, 10, Rune.COSMIC, 1, Rune.MIND, 5)
    ),

    // Resurrect Spells - Superior
    /** Resurrect Superior Ghost, which summons a superior ghost thrall that attacks the player's target with Magic. */
    RESURRECT_SUPERIOR_GHOST(
            57,
            InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_GHOST,
            39,
            Map.of(Rune.EARTH, 10, Rune.COSMIC, 1, Rune.DEATH, 5)
    ),
    /** Resurrect Superior Skeleton, which summons a superior skeleton thrall that attacks the player's target with Ranged. */
    RESURRECT_SUPERIOR_SKELETON(
            57,
            InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_SKELETON,
            40,
            Map.of(Rune.EARTH, 10, Rune.COSMIC, 1, Rune.DEATH, 5)
    ),
    /** Resurrect Superior Zombie, which summons a superior zombie thrall that attacks the player's target with melee. */
    RESURRECT_SUPERIOR_ZOMBIE(
            57,
            InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_ZOMBIE,
            41,
            Map.of(Rune.EARTH, 10, Rune.COSMIC, 1, Rune.DEATH, 5)
    ),

    // Resurrect Spells - Greater
    /** Resurrect Greater Ghost, which summons a greater ghost thrall that attacks the player's target with Magic. */
    RESURRECT_GREATER_GHOST(
            76,
            InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST,
            42,
            Map.of(Rune.FIRE, 10, Rune.COSMIC, 1, Rune.BLOOD, 5)
    ),
    /** Resurrect Greater Skeleton, which summons a greater skeleton thrall that attacks the player's target with Ranged. */
    RESURRECT_GREATER_SKELETON(
            76,
            InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON,
            43,
            Map.of(Rune.FIRE, 10, Rune.COSMIC, 1, Rune.BLOOD, 5)
    ),
    /** Resurrect Greater Zombie, which summons a greater zombie thrall that attacks the player's target with melee. */
    RESURRECT_GREATER_ZOMBIE(
            76,
            InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE,
            44,
            Map.of(Rune.FIRE, 10, Rune.COSMIC, 1, Rune.BLOOD, 5)
    ),

    // Utility Spells
    /** Dark Lure, an Arceuus utility spell. */
    DARK_LURE(
            50,
            InterfaceID.MagicSpellbook.DARK_LURE,
            34,
            Map.of(Rune.DEATH, 1, Rune.NATURE, 1)
    ),
    /** Mark of Darkness, an Arceuus utility spell. */
    MARK_OF_DARKNESS(
            59,
            InterfaceID.MagicSpellbook.MARK_OF_DARKNESS,
            22,
            Map.of(Rune.COSMIC, 1, Rune.SOUL, 1)
    ),
    /** Ward of Arceuus, an Arceuus utility spell. */
    WARD_OF_ARCEUUS(
            73,
            InterfaceID.MagicSpellbook.WARD_OF_ARCEUUS,
            26,
            Map.of(Rune.COSMIC, 1, Rune.NATURE, 2, Rune.SOUL, 4)
    ),
    /** Demonic Offering, an Arceuus utility spell. */
    DEMONIC_OFFERING(
            84,
            InterfaceID.MagicSpellbook.DEMONIC_OFFERING,
            29,
            Map.of(Rune.SOUL, 1, Rune.WRATH, 1)
    ),
    /** Sinister Offering, an Arceuus utility spell. */
    SINISTER_OFFERING(
            92,
            InterfaceID.MagicSpellbook.SINISTER_OFFERING,
            30,
            Map.of(Rune.BLOOD, 1, Rune.WRATH, 1)
    ),
    /** Shadow Veil, an Arceuus utility spell. */
    SHADOW_VEIL(
            47,
            InterfaceID.MagicSpellbook.SHADOW_VEIL,
            32,
            Map.of(Rune.EARTH, 5, Rune.FIRE, 5, Rune.COSMIC, 5)
    ),
    /** Vile Vigour, an Arceuus utility spell. */
    VILE_VIGOUR(
            66,
            InterfaceID.MagicSpellbook.VILE_VIGOUR,
            33,
            Map.of(Rune.AIR, 3, Rune.SOUL, 1)
    ),
    /** Degrime, an Arceuus utility spell. */
    DEGRIME(
            70,
            InterfaceID.MagicSpellbook.DEGRIME,
            31,
            Map.of(Rune.EARTH, 4, Rune.NATURE, 2)
    ),
    /** Resurrect Crops, an Arceuus utility spell. */
    RESURRECT_CROPS(
            78,
            InterfaceID.MagicSpellbook.RESURRECT_CROPS,
            17,
            Map.of(Rune.EARTH, 25, Rune.BLOOD, 8, Rune.NATURE, 12, Rune.SOUL, 8)
    ),
    /** Death Charge, an Arceuus spell that charges the player with a death charge. */
    DEATH_CHARGE(
            80,
            InterfaceID.MagicSpellbook.DEATH_CHARGE,
            35,
            Map.of(Rune.BLOOD, 1, Rune.DEATH, 1, Rune.SOUL, 1)
    );

    private final int level;
    private final int widget;
    private final int index;
    private final Map<Rune, Integer> runeRequirement;
    private final Spellbook spellbook = Spellbook.ARCEUUS;
    private final String name;

    Arceuus(int level, int widget, int index, Map<Rune, Integer> runeRequirement) {
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
