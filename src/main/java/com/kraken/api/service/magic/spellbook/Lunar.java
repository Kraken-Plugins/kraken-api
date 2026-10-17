package com.kraken.api.service.magic.spellbook;

import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.rune.Rune;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

import java.util.Map;

/** The spells of the Lunar spellbook. */
@Getter
public enum Lunar implements CastableSpell {
    // Home Teleport
    /** Lunar Home Teleport, which teleports the player to Lunar Isle. */
    LUNAR_HOME_TELEPORT(
            0,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_LUNAR,
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

    // Utility Spells
    /** Bake Pie, which cooks raw pies in the inventory. */
    BAKE_PIE(
            65,
            InterfaceID.MagicSpellbook.BAKE_PIE,
            2,
            Map.of(Rune.WATER, 4, Rune.FIRE, 5, Rune.ASTRAL, 1)
    ),
    /** Cure Plant, which cures a diseased farming plant. */
    CURE_PLANT(
            66,
            InterfaceID.MagicSpellbook.CURE_PLANT,
            3,
            Map.of(Rune.EARTH, 8, Rune.ASTRAL, 1)
    ),
    /** NPC Contact, a Lunar utility spell. */
    NPC_CONTACT(
            67,
            InterfaceID.MagicSpellbook.NPC_CONTACT,
            4,
            Map.of(Rune.AIR, 2, Rune.COSMIC, 1, Rune.ASTRAL, 1)
    ),
    /** Cure Other, which cures poison on another player. */
    CURE_OTHER(
            68,
            InterfaceID.MagicSpellbook.CURE_OTHER,
            5,
            Map.of(Rune.EARTH, 10, Rune.ASTRAL, 1, Rune.LAW, 1)
    ),
    /** Cure Me, which cures poison on the player. */
    CURE_ME(
            71,
            InterfaceID.MagicSpellbook.CURE_ME,
            8,
            Map.of(Rune.COSMIC, 2, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    /** Cure Group, which cures poison on the player and nearby group members. */
    CURE_GROUP(
            74,
            InterfaceID.MagicSpellbook.CURE_GROUP,
            11,
            Map.of(Rune.COSMIC, 2, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    /** Stat Spy, which shows another player's skill levels. */
    STAT_SPY(
            75,
            InterfaceID.MagicSpellbook.STAT_SPY,
            12,
            Map.of(Rune.BODY, 5, Rune.COSMIC, 1, Rune.ASTRAL, 2)
    ),
    /** Spin Flax, which turns flax into bowstrings. */
    SPIN_FLAX(
            76,
            InterfaceID.MagicSpellbook.SPIN_FLAX,
            15,
            Map.of(Rune.AIR, 5, Rune.ASTRAL, 1, Rune.NATURE, 2)
    ),
    /** Superglass Make, which makes superglass from sand and seaweed. */
    SUPERGLASS_MAKE(
            77,
            InterfaceID.MagicSpellbook.SUPERGLASS,
            16,
            Map.of(Rune.AIR, 10, Rune.FIRE, 6, Rune.ASTRAL, 2)
    ),
    /** Tan Leather, which tans hides into leather. */
    TAN_LEATHER(
            78,
            InterfaceID.MagicSpellbook.TAN_LEATHER,
            17,
            Map.of(Rune.FIRE, 5, Rune.ASTRAL, 2, Rune.NATURE, 1)
    ),
    /** String Jewellery, which strings unstrung amulets. */
    STRING_JEWELLERY(
            80,
            InterfaceID.MagicSpellbook.STRING_JEWEL,
            20,
            Map.of(Rune.EARTH, 10, Rune.WATER, 5, Rune.ASTRAL, 2)
    ),
    /** Stat Restore Pot Share, which shares a restore potion's effect with nearby players. */
    STAT_RESTORE_POT_SHARE(
            81,
            InterfaceID.MagicSpellbook.REST_POT_SHARE,
            21,
            Map.of(Rune.WATER, 10, Rune.EARTH, 10, Rune.ASTRAL, 2)
    ),
    /** Magic Imbue, a Lunar utility spell. */
    MAGIC_IMBUE(
            82,
            InterfaceID.MagicSpellbook.MAGIC_IMBUE,
            22,
            Map.of(Rune.WATER, 7, Rune.FIRE, 7, Rune.ASTRAL, 2)
    ),
    /** Fertile Soil, which fertilises a farming patch. */
    FERTILE_SOIL(
            83,
            InterfaceID.MagicSpellbook.FERTILE_SOIL,
            23,
            Map.of(Rune.EARTH, 15, Rune.ASTRAL, 3, Rune.NATURE, 2)
    ),
    /** Boost Potion Share, which shares a boost potion's effect with nearby players. */
    BOOST_POTION_SHARE(
            84,
            InterfaceID.MagicSpellbook.STREN_POT_SHARE,
            24,
            Map.of(Rune.WATER, 10, Rune.EARTH, 12, Rune.ASTRAL, 3)
    ),
    /** Recharge Dragonstone, which recharges dragonstone jewellery. */
    RECHARGE_DRAGONSTONE(
            89,
            InterfaceID.MagicSpellbook.RECHARGE_DRAGONSTONE,
            29,
            Map.of(Rune.WATER, 4, Rune.ASTRAL, 1, Rune.SOUL, 1)
    ),
    /** Energy Transfer, which transfers run energy to another player. */
    ENERGY_TRANSFER(
            91,
            InterfaceID.MagicSpellbook.ENERGY_TRANS,
            32,
            Map.of(Rune.ASTRAL, 3, Rune.NATURE, 1, Rune.LAW, 2)
    ),
    /** Heal Other, which heals another player by transferring hitpoints from the caster. */
    HEAL_OTHER(
            92,
            InterfaceID.MagicSpellbook.HEAL_OTHER,
            33,
            Map.of(Rune.ASTRAL, 3, Rune.LAW, 3, Rune.BLOOD, 1)
    ),
    /** Vengeance Other, which casts vengeance on another player. */
    VENGEANCE_OTHER(
            93,
            InterfaceID.MagicSpellbook.VENGEANCE_OTHER,
            34,
            Map.of(Rune.EARTH, 10, Rune.ASTRAL, 3, Rune.DEATH, 2)
    ),
    /** Vengeance, which casts vengeance on the player. */
    VENGEANCE(
            94,
            InterfaceID.MagicSpellbook.VENGEANCE,
            35,
            Map.of(Rune.EARTH, 10, Rune.ASTRAL, 4, Rune.DEATH, 2)
    ),
    /** Heal Group, which heals the player and nearby group members. */
    HEAL_GROUP(
            95,
            InterfaceID.MagicSpellbook.HEAL_GROUP,
            36,
            Map.of(Rune.ASTRAL, 4, Rune.LAW, 6, Rune.BLOOD, 3)
    ),
    /** Monster Examine, which shows information about a monster. */
    MONSTER_EXAMINE(
            66,
            InterfaceID.MagicSpellbook.MONSTER_EXAMINE,
            37,
            Map.of(Rune.MIND, 1, Rune.COSMIC, 1, Rune.ASTRAL, 1)
    ),
    /** Humidify, which fills buckets and pots with water. */
    HUMIDIFY(
            68,
            InterfaceID.MagicSpellbook.HUMIDIFY,
            38,
            Map.of(Rune.WATER, 3, Rune.FIRE, 1, Rune.ASTRAL, 1)
    ),
    /** Hunter Kit, which creates a hunter kit. */
    HUNTER_KIT(
            71,
            InterfaceID.MagicSpellbook.HUNTER_KIT,
            40,
            Map.of(Rune.EARTH, 2, Rune.ASTRAL, 2)
    ),
    /** Dream, a Lunar utility spell. */
    DREAM(
            79,
            InterfaceID.MagicSpellbook.DREAM,
            41,
            Map.of(Rune.BODY, 5, Rune.COSMIC, 1, Rune.ASTRAL, 2)
    ),
    /** Plank Make, which turns logs into planks. */
    PLANK_MAKE(
            86,
            InterfaceID.MagicSpellbook.PLANK_MAKE,
            42,
            Map.of(Rune.EARTH, 15, Rune.ASTRAL, 2, Rune.NATURE, 1)
    ),
    /** Spellbook Swap, which switches to another spellbook. */
    SPELLBOOK_SWAP(
            96,
            InterfaceID.MagicSpellbook.SPELLBOOK_SWAP,
            43,
            Map.of(Rune.COSMIC, 2, Rune.ASTRAL, 3, Rune.LAW, 1)
    ),
    /** Geomancy, a Lunar utility spell. */
    GEOMANCY(
            65,
            InterfaceID.MagicSpellbook.GEOMANCY,
            44,
            Map.of(Rune.EARTH, 8, Rune.ASTRAL, 3, Rune.NATURE, 3)
    ),

    // Teleport Spells
    /** Moonclan Teleport, which teleports the player to Moonclan Island. */
    MOONCLAN_TELEPORT(
            69,
            InterfaceID.MagicSpellbook.TELE_MOONCLAN,
            6,
            Map.of(Rune.EARTH, 2, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    /** Group Moonclan Teleport, which teleports the player and nearby group members to Moonclan Island. */
    TELE_GROUP_MOONCLAN(
            70,
            InterfaceID.MagicSpellbook.TELE_GROUP_MOONCLAN,
            7,
            Map.of(Rune.EARTH, 4, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    /** Waterbirth Teleport, which teleports the player to Waterbirth Island. */
    WATERBIRTH_TELEPORT(
            72,
            InterfaceID.MagicSpellbook.TELE_WATERBIRTH,
            9,
            Map.of(Rune.WATER, 1, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    /** Group Waterbirth Teleport, which teleports the player and nearby group members to Waterbirth Island. */
    TELE_GROUP_WATERBIRTH(
            73,
            InterfaceID.MagicSpellbook.TELE_GROUP_WATERBIRTH,
            10,
            Map.of(Rune.WATER, 5, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    /** Barbarian Outpost Teleport, which teleports the player to the Barbarian Outpost. */
    BARBARIAN_TELEPORT(
            75,
            InterfaceID.MagicSpellbook.TELE_BARB_OUT,
            13,
            Map.of(Rune.FIRE, 3, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    /** Group Barbarian Outpost Teleport, which teleports the player and nearby group members to the Barbarian Outpost. */
    TELE_GROUP_BARBARIAN(
            76,
            InterfaceID.MagicSpellbook.TELE_GROUP_BARBARIAN,
            14,
            Map.of(Rune.FIRE, 6, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    /** Khazard Teleport, which teleports the player to Port Khazard. */
    KHAZARD_TELEPORT(
            78,
            InterfaceID.MagicSpellbook.TELE_KHAZARD,
            18,
            Map.of(Rune.WATER, 4, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    /** Group Khazard Teleport, which teleports the player and nearby group members to Port Khazard. */
    TELE_GROUP_KHAZARD(
            79,
            InterfaceID.MagicSpellbook.TELE_GROUP_KHAZARD,
            19,
            Map.of(Rune.WATER, 8, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    /** Fishing Guild Teleport, which teleports the player to the Fishing Guild. */
    FISHING_GUILD_TELEPORT(
            85,
            InterfaceID.MagicSpellbook.TELE_FISH,
            25,
            Map.of(Rune.WATER, 10, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    /** Group Fishing Guild Teleport, which teleports the player and nearby group members to the Fishing Guild. */
    TELE_GROUP_FISHING_GUILD(
            86,
            InterfaceID.MagicSpellbook.TELE_GROUP_FISHING_GUILD,
            26,
            Map.of(Rune.WATER, 14, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    /** Catherby Teleport, which teleports the player to Catherby. */
    CATHERBY_TELEPORT(
            87,
            InterfaceID.MagicSpellbook.TELE_CATHER,
            27,
            Map.of(Rune.WATER, 10, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    /** Group Catherby Teleport, which teleports the player and nearby group members to Catherby. */
    TELE_GROUP_CATHERBY(
            88,
            InterfaceID.MagicSpellbook.TELE_GROUP_CATHERBY,
            28,
            Map.of(Rune.WATER, 15, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    /** Ice Plateau Teleport, which teleports the player to the Ice Plateau. */
    ICE_PLATEAU_TELEPORT(
            89,
            InterfaceID.MagicSpellbook.TELE_GHORROCK,
            30,
            Map.of(Rune.WATER, 8, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    /** Group Ice Plateau Teleport, which teleports the player and nearby group members to the Ice Plateau. */
    TELE_GROUP_ICE_PLATEAU(
            90,
            InterfaceID.MagicSpellbook.TELE_GROUP_GHORROCK,
            31,
            Map.of(Rune.WATER, 16, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    /** Ourania Teleport, which teleports the player to the Ourania Altar. */
    OURANIA_TELEPORT(
            71,
            InterfaceID.MagicSpellbook.OURANIA_TELEPORT,
            39,
            Map.of(Rune.EARTH, 6, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    /** Teleport to Target, which teleports the player to their Bounty Hunter target. */
    TELEPORT_TO_TARGET(
            85,
            InterfaceID.MagicSpellbook.BOUNTY_TARGET,
            45,
            Map.of(Rune.CHAOS, 1, Rune.DEATH, 1, Rune.LAW, 1)
    );

    private final int level;
    private final int widget;
    private final int index;
    private final Map<Rune, Integer> runeRequirement;
    private final Spellbook spellbook = Spellbook.LUNAR;
    private final String name;

    Lunar(int level, int widget, int index, Map<Rune, Integer> runeRequirement) {
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

