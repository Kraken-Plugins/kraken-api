package com.kraken.api.service.magic.spellbook;

import com.kraken.api.Context;
import com.kraken.api.core.Services;
import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.rune.Rune;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.RuneLite;

import java.util.Map;

/** The spells of the Standard spellbook. */
@Getter
public enum Standard implements CastableSpell {

    /** Home Teleport, which teleports the player to Lumbridge. */
    HOME_TELEPORT(
            0,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_STANDARD,
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

    /** Varrock Teleport, which teleports the player to Varrock. */
    VARROCK_TELEPORT(
            25,
            InterfaceID.MagicSpellbook.VARROCK_TELEPORT,
            16,
            Map.of(Rune.AIR, 3, Rune.FIRE, 1, Rune.LAW, 1)
    ),
    /** Grand Exchange Teleport, the Varrock Teleport variant that teleports the player to the Grand Exchange. */
    GRAND_EXCHANGE_TELEPORT(
            25,
            InterfaceID.MagicSpellbook.VARROCK_TELEPORT,
            16,
            Map.of(Rune.AIR, 3, Rune.FIRE, 1, Rune.LAW, 1)
    ),
    /** Lumbridge Teleport, which teleports the player to Lumbridge. */
    LUMBRIDGE_TELEPORT(
            31,
            InterfaceID.MagicSpellbook.LUMBRIDGE_TELEPORT,
            18,
            Map.of(Rune.AIR, 3, Rune.EARTH, 1, Rune.LAW, 1)
    ),
    /** Falador Teleport, which teleports the player to Falador. */
    FALADOR_TELEPORT(
            37,
            InterfaceID.MagicSpellbook.FALADOR_TELEPORT,
            21,
            Map.of(Rune.AIR, 3, Rune.WATER, 1, Rune.LAW, 1)
    ),
    /** Teleport to House, which teleports the player to their player-owned house. */
    TELEPORT_TO_HOUSE(
            40,
            InterfaceID.MagicSpellbook.TELEPORT_TO_YOUR_HOUSE,
            49,
            Map.of(Rune.AIR, 1, Rune.EARTH, 1, Rune.LAW, 1)
    ),
    /** Camelot Teleport, which teleports the player to Camelot. */
    CAMELOT_TELEPORT(
            45,
            InterfaceID.MagicSpellbook.CAMELOT_TELEPORT,
            26,
            Map.of(Rune.AIR, 5, Rune.LAW, 1)
    ),
    /** Seers' Village Teleport, the Camelot Teleport variant that teleports the player to Seers' Village. */
    SEERS_TELEPORT(
            45,
            InterfaceID.MagicSpellbook.CAMELOT_TELEPORT,
            26,
            Map.of(Rune.AIR, 5, Rune.LAW, 1)
    ),
    /** Ardougne Teleport, which teleports the player to Ardougne. */
    ARDOUGNE_TELEPORT(
            51,
            InterfaceID.MagicSpellbook.ARDOUGNE_TELEPORT,
            31,
            Map.of(Rune.WATER, 2, Rune.LAW, 2)
    ),
    /** Watchtower Teleport, which teleports the player to the Watchtower in Yanille. */
    WATCHTOWER_TELEPORT(
            58,
            InterfaceID.MagicSpellbook.WATCHTOWER_TELEPORT,
            35,
            Map.of(Rune.EARTH, 2, Rune.LAW, 2)
    ),
    /** Yanille Teleport, the Watchtower Teleport variant that teleports the player to Yanille. */
    YANILLE_TELEPORT(
            58,
            InterfaceID.MagicSpellbook.WATCHTOWER_TELEPORT,
            35,
            Map.of(Rune.EARTH, 2, Rune.LAW, 2)
    ),
    /** Trollheim Teleport, which teleports the player to Trollheim. */
    TROLLHEIM_TELEPORT(
            61,
            InterfaceID.MagicSpellbook.TROLLHEIM_TELEPORT,
            42,
            Map.of(Rune.FIRE, 2, Rune.LAW, 2)
    ),
    /** Teleport to Ape Atoll, which teleports the player to Ape Atoll. */
    TELEPORT_TO_APE_ATOLL(
            64,
            InterfaceID.MagicSpellbook.APE_TELEPORT,
            45,
            Map.of(Rune.FIRE, 2, Rune.WATER, 2, Rune.LAW, 2)
    ),
    /** Teleport to Kourend, which teleports the player to Kourend. */
    TELEPORT_TO_KOUREND(
            69,
            InterfaceID.MagicSpellbook.KOUREND_TELEPORT,
            50,
            Map.of(Rune.FIRE, 5, Rune.WATER, 4, Rune.SOUL, 2, Rune.LAW, 2)
    ),
    /** Teleport Other to Lumbridge, which teleports another player to Lumbridge. */
    TELEOTHER_LUMBRIDGE(
            74,
            InterfaceID.MagicSpellbook.TELEOTHER_LUMBRIDGE,
            53,
            Map.of(Rune.EARTH, 1, Rune.LAW, 1, Rune.SOUL, 1)
    ),
    /** Teleport Other to Falador, which teleports another player to Falador. */
    TELEOTHER_FALADOR(
            82,
            InterfaceID.MagicSpellbook.TELEOTHER_FALADOR,
            59,
            Map.of(Rune.WATER, 1, Rune.LAW, 1, Rune.SOUL, 1)
    ),
    /** Teleport to Bounty Target, which teleports the player to their Bounty Hunter target. */
    TELEPORT_TO_BOUNTY_TARGET(
            85,
            InterfaceID.MagicSpellbook.BOUNTY_TARGET,
            62,
            Map.of(Rune.CHAOS, 1, Rune.DEATH, 1, Rune.LAW, 1)
    ),
    /** Teleport Other to Camelot, which teleports another player to Camelot. */
    TELEOTHER_CAMELOT(
            90,
            InterfaceID.MagicSpellbook.TELEOTHER_CAMELOT,
            63,
            Map.of(Rune.LAW, 1, Rune.SOUL, 2)
    ),

    // Strike Spells
    /** Wind Strike, an air combat spell. */
    WIND_STRIKE(
            1,
            InterfaceID.MagicSpellbook.WIND_STRIKE,
            2,
            Map.of(Rune.AIR, 1, Rune.MIND, 1)
    ),
    /** Water Strike, a water combat spell. */
    WATER_STRIKE(
            5,
            InterfaceID.MagicSpellbook.WATER_STRIKE,
            5,
            Map.of(Rune.AIR, 1, Rune.WATER, 1, Rune.MIND, 1)
    ),
    /** Earth Strike, an earth combat spell. */
    EARTH_STRIKE(
            9,
            InterfaceID.MagicSpellbook.EARTH_STRIKE,
            7,
            Map.of(Rune.AIR, 1, Rune.EARTH, 2, Rune.MIND, 1)
    ),
    /** Fire Strike, a fire combat spell. */
    FIRE_STRIKE(
            13,
            InterfaceID.MagicSpellbook.FIRE_STRIKE,
            9,
            Map.of(Rune.AIR, 2, Rune.FIRE, 3, Rune.MIND, 1)
    ),

    // Bolt Spells
    /** Wind Bolt, an air combat spell. */
    WIND_BOLT(
            17,
            InterfaceID.MagicSpellbook.WIND_BOLT,
            11,
            Map.of(Rune.AIR, 2, Rune.CHAOS, 1)
    ),
    /** Water Bolt, a water combat spell. */
    WATER_BOLT(
            23,
            InterfaceID.MagicSpellbook.WATER_BOLT,
            15,
            Map.of(Rune.AIR, 2, Rune.WATER, 2, Rune.CHAOS, 1)
    ),
    /** Earth Bolt, an earth combat spell. */
    EARTH_BOLT(
            29,
            InterfaceID.MagicSpellbook.EARTH_BOLT,
            17,
            Map.of(Rune.AIR, 2, Rune.EARTH, 3, Rune.CHAOS, 1)
    ),
    /** Fire Bolt, a fire combat spell. */
    FIRE_BOLT(
            35,
            InterfaceID.MagicSpellbook.FIRE_BOLT,
            20,
            Map.of(Rune.AIR, 3, Rune.FIRE, 4, Rune.CHAOS, 1)
    ),

    // Blast Spells
    /** Wind Blast, an air combat spell. */
    WIND_BLAST(
            41,
            InterfaceID.MagicSpellbook.WIND_BLAST,
            23,
            Map.of(Rune.AIR, 3, Rune.DEATH, 1)
    ),
    /** Water Blast, a water combat spell. */
    WATER_BLAST(
            47,
            InterfaceID.MagicSpellbook.WATER_BLAST,
            27,
            Map.of(Rune.AIR, 3, Rune.WATER, 3, Rune.DEATH, 1)
    ),
    /** Earth Blast, an earth combat spell. */
    EARTH_BLAST(
            53,
            InterfaceID.MagicSpellbook.EARTH_BLAST,
            32,
            Map.of(Rune.AIR, 3, Rune.EARTH, 4, Rune.DEATH, 1)
    ),
    /** Fire Blast, a fire combat spell. */
    FIRE_BLAST(
            59,
            InterfaceID.MagicSpellbook.FIRE_BLAST,
            36,
            Map.of(Rune.AIR, 4, Rune.FIRE, 5, Rune.DEATH, 1)
    ),

    // Wave Spells
    /** Wind Wave, an air combat spell. */
    WIND_WAVE(
            62,
            InterfaceID.MagicSpellbook.WIND_WAVE,
            43,
            Map.of(Rune.AIR, 5, Rune.BLOOD, 1)
    ),
    /** Water Wave, a water combat spell. */
    WATER_WAVE(
            65,
            InterfaceID.MagicSpellbook.WATER_WAVE,
            46,
            Map.of(Rune.AIR, 5, Rune.WATER, 7, Rune.BLOOD, 1)
    ),
    /** Earth Wave, an earth combat spell. */
    EARTH_WAVE(
            70,
            InterfaceID.MagicSpellbook.EARTH_WAVE,
            51,
            Map.of(Rune.AIR, 5, Rune.EARTH, 7, Rune.BLOOD, 1)
    ),
    /** Fire Wave, a fire combat spell. */
    FIRE_WAVE(
            75,
            InterfaceID.MagicSpellbook.FIRE_WAVE,
            54,
            Map.of(Rune.AIR, 5, Rune.FIRE, 7, Rune.BLOOD, 1)
    ),

    // Surge Spells
    /** Wind Surge, an air combat spell. */
    WIND_SURGE(
            81,
            InterfaceID.MagicSpellbook.WIND_SURGE,
            58,
            Map.of(Rune.AIR, 7, Rune.WRATH, 1)
    ),
    /** Water Surge, a water combat spell. */
    WATER_SURGE(
            85,
            InterfaceID.MagicSpellbook.WATER_SURGE,
            60,
            Map.of(Rune.AIR, 7, Rune.WATER, 10, Rune.WRATH, 1)
    ),
    /** Earth Surge, an earth combat spell. */
    EARTH_SURGE(
            90,
            InterfaceID.MagicSpellbook.EARTH_SURGE,
            64,
            Map.of(Rune.AIR, 7, Rune.EARTH, 10, Rune.WRATH, 1)
    ),
    /** Fire Surge, a fire combat spell. */
    FIRE_SURGE(
            95,
            InterfaceID.MagicSpellbook.FIRE_SURGE,
            65,
            Map.of(Rune.AIR, 7, Rune.FIRE, 10, Rune.WRATH, 1)
    ),

    // God Spells
    /** Saradomin Strike, a god combat spell. */
    SARADOMIN_STRIKE(
            60,
            InterfaceID.MagicSpellbook.SARADOMIN_STRIKE,
            39,
            Map.of(Rune.AIR, 4, Rune.FIRE, 2, Rune.BLOOD, 2)
    ),
    /** Claws of Guthix, a god combat spell. */
    CLAWS_OF_GUTHIX(
            60,
            InterfaceID.MagicSpellbook.CLAWS_OF_GUTHIX,
            40,
            Map.of(Rune.AIR, 4, Rune.FIRE, 1, Rune.BLOOD, 2)
    ),
    /** Flames of Zamorak, a god combat spell. */
    FLAMES_OF_ZAMORAK(
            60,
            InterfaceID.MagicSpellbook.FLAMES_OF_ZAMORAK,
            41,
            Map.of(Rune.AIR, 1, Rune.FIRE, 4, Rune.BLOOD, 2)
    ),

    // Other Combat Spells
    /** Crumble Undead, a combat spell that is effective against undead monsters. */
    CRUMBLE_UNDEAD(
            39,
            InterfaceID.MagicSpellbook.CRUMBLE_UNDEAD,
            22,
            Map.of(Rune.AIR, 2, Rune.EARTH, 2, Rune.CHAOS, 1)
    ),
    /** Iban Blast, a combat spell. */
    IBAN_BLAST(
            50,
            InterfaceID.MagicSpellbook.IBAN_BLAST,
            28,
            Map.of(Rune.FIRE, 5, Rune.DEATH, 1)
    ),
    /** Magic Dart, a combat spell. */
    MAGIC_DART(
            50,
            InterfaceID.MagicSpellbook.MAGIC_DART,
            30,
            Map.of(Rune.DEATH, 1, Rune.MIND, 4)
    ),

    // Curse/Debuff Spells
    /** Confuse, a curse spell that reduces the target's attack. */
    CONFUSE(
            3,
            InterfaceID.MagicSpellbook.CONFUSE,
            3,
            Map.of(Rune.EARTH, 2, Rune.WATER, 3, Rune.BODY, 1)
    ),
    /** Weaken, a curse spell that reduces the target's strength. */
    WEAKEN(
            11,
            InterfaceID.MagicSpellbook.WEAKEN,
            8,
            Map.of(Rune.EARTH, 2, Rune.WATER, 3, Rune.BODY, 1)
    ),
    /** Curse, a curse spell that reduces the target's defence. */
    CURSE(
            19,
            InterfaceID.MagicSpellbook.CURSE,
            12,
            Map.of(Rune.EARTH, 3, Rune.WATER, 2, Rune.BODY, 1)
    ),
    /** Bind, a curse spell that holds the target in place. */
    BIND(
            20,
            InterfaceID.MagicSpellbook.BIND,
            13,
            Map.of(Rune.EARTH, 3, Rune.WATER, 3, Rune.NATURE, 2)
    ),
    /** Snare, a curse spell that holds the target in place. */
    SNARE(
            50,
            InterfaceID.MagicSpellbook.SNARE,
            29,
            Map.of(Rune.EARTH, 4, Rune.WATER, 4, Rune.NATURE, 3)
    ),
    /** Vulnerability, a curse spell that lowers the target's Defence by 10%. */
    VULNERABILITY(
            66,
            InterfaceID.MagicSpellbook.VULNERABILITY,
            48,
            Map.of(Rune.EARTH, 5, Rune.WATER, 5, Rune.SOUL, 1)
    ),
    /** Enfeeble, a curse spell that lowers the target's Strength by 10%. */
    ENFEEBLE(
            73,
            InterfaceID.MagicSpellbook.ENFEEBLE,
            52,
            Map.of(Rune.EARTH, 8, Rune.WATER, 8, Rune.SOUL, 1)
    ),
    /** Entangle, a curse spell that holds the target in place. */
    ENTANGLE(
            79,
            InterfaceID.MagicSpellbook.ENTANGLE,
            55,
            Map.of(Rune.EARTH, 5, Rune.WATER, 5, Rune.NATURE, 4)
    ),
    /** Stun, a curse spell that lowers the target's Attack by 10%. */
    STUN(
            80,
            InterfaceID.MagicSpellbook.STUN,
            56,
            Map.of(Rune.EARTH, 12, Rune.WATER, 12, Rune.SOUL, 1)
    ),
    /** Tele Block, a curse spell that stops the target from teleporting. */
    TELE_BLOCK(
            85,
            InterfaceID.MagicSpellbook.TELEPORT_BLOCK,
            61,
            Map.of(Rune.CHAOS, 1, Rune.DEATH, 1, Rune.LAW, 1)
    ),

    // Utility Spells
    /** Charge, a utility spell that grants the Charge effect. */
    CHARGE(
            80,
            InterfaceID.MagicSpellbook.CHARGE,
            57,
            Map.of(Rune.AIR, 3, Rune.FIRE, 3, Rune.BLOOD, 3)
    ),
    /** Bones to Bananas, which turns bones into bananas. */
    BONES_TO_BANANAS(
            15,
            InterfaceID.MagicSpellbook.BONES_BANANAS,
            10,
            Map.of(Rune.EARTH, 2, Rune.WATER, 2, Rune.NATURE, 1)
    ),
    /** Low Level Alchemy, which converts an item into coins. */
    LOW_LEVEL_ALCHEMY(
            21,
            InterfaceID.MagicSpellbook.LOW_ALCHEMY,
            14,
            Map.of(Rune.FIRE, 3, Rune.NATURE, 1)
    ),
    /** Superheat Item, which smelts an ore into a bar. */
    SUPERHEAT_ITEM(
            43,
            InterfaceID.MagicSpellbook.SUPERHEAT,
            25,
            Map.of(Rune.FIRE, 4, Rune.NATURE, 1)
    ),
    /** High Level Alchemy, which converts an item into coins at a higher rate than Low Level Alchemy. */
    HIGH_LEVEL_ALCHEMY(
            55,
            InterfaceID.MagicSpellbook.HIGH_ALCHEMY,
            33,
            Map.of(Rune.FIRE, 5, Rune.NATURE, 1)
    ),
    /** Bones to Peaches, which turns bones into peaches. */
    BONES_TO_PEACHES(
            60,
            InterfaceID.MagicSpellbook.BONES_PEACHES,
            38,
            Map.of(Rune.EARTH, 2, Rune.WATER, 4, Rune.NATURE, 2)
    ),

    // Enchantment Spells
    /** Level 1 Enchant, which enchants sapphire jewellery. */
    LVL_1_ENCHANT(
            7,
            InterfaceID.MagicSpellbook.ENCHANT_1,
            6,
            Map.of(Rune.WATER, 1, Rune.COSMIC, 1)
    ),
    /** Level 2 Enchant, which enchants emerald jewellery. */
    LVL_2_ENCHANT(
            27,
            InterfaceID.MagicSpellbook.ENCHANT_2,
            6,
            Map.of(Rune.AIR, 3, Rune.COSMIC, 1)
    ),
    /** Level 3 Enchant, which enchants ruby jewellery. */
    LVL_3_ENCHANT(
            49,
            InterfaceID.MagicSpellbook.ENCHANT_3,
            6,
            Map.of(Rune.FIRE, 5, Rune.COSMIC, 1)
    ),
    /** Charge Water Orb, which charges an orb of water. */
    CHARGE_WATER_ORB(
            56,
            InterfaceID.MagicSpellbook.CHARGE_WATER_ORB,
            34,
            Map.of(Rune.WATER, 30, Rune.COSMIC, 3)
    ),
    /** Level 4 Enchant, which enchants diamond jewellery. */
    LVL_4_ENCHANT(
            57,
            InterfaceID.MagicSpellbook.ENCHANT_4,
            6,
            Map.of(Rune.EARTH, 10, Rune.COSMIC, 1)
    ),
    /** Charge Earth Orb, which charges an orb of earth. */
    CHARGE_EARTH_ORB(
            60,
            InterfaceID.MagicSpellbook.CHARGE_EARTH_ORB,
            37,
            Map.of(Rune.EARTH, 30, Rune.COSMIC, 3)
    ),
    /** Charge Fire Orb, which charges an orb of fire. */
    CHARGE_FIRE_ORB(
            63,
            InterfaceID.MagicSpellbook.CHARGE_FIRE_ORB,
            44,
            Map.of(Rune.FIRE, 30, Rune.COSMIC, 3)
    ),
    /** Charge Air Orb, which charges an orb of air. */
    CHARGE_AIR_ORB(
            66,
            InterfaceID.MagicSpellbook.CHARGE_AIR_ORB,
            47,
            Map.of(Rune.AIR, 30, Rune.COSMIC, 3)
    ),
    /** Level 5 Enchant, which enchants dragonstone jewellery. */
    LVL_5_ENCHANT(
            68,
            InterfaceID.MagicSpellbook.ENCHANT_5,
            6,
            Map.of(Rune.EARTH, 15, Rune.WATER, 15, Rune.COSMIC, 1)
    ),
    /** Level 6 Enchant, which enchants onyx jewellery. */
    LVL_6_ENCHANT(
            87,
            InterfaceID.MagicSpellbook.ENCHANT_6,
            6,
            Map.of(Rune.EARTH, 20, Rune.FIRE, 20, Rune.COSMIC, 1)
    ),
    /** Level 7 Enchant, which enchants zenyte jewellery. */
    LVL_7_ENCHANT(
            93,
            InterfaceID.MagicSpellbook.ENCHANT_7,
            6,
            Map.of(Rune.BLOOD, 20, Rune.SOUL, 20, Rune.COSMIC, 1)
    ),
    /** Telekinetic Grab, which pulls a ground item to the player. */
    TELEKINETIC_GRAB(
            31,
            InterfaceID.MagicSpellbook.TELEGRAB,
            19,
            Map.of(Rune.AIR, 1, Rune.LAW, 1)
    );

    private final int level;
    private final int widget;
    private final int index;
    private final Map<Rune, Integer> runeRequirement;
    private final Spellbook spellbook = Spellbook.STANDARD;
    private final String name;

    Standard(int level, int widget, int index, Map<Rune, Integer> runeRequirement) {
        this.level = level;
        this.widget = widget;
        this.index = index;
        this.runeRequirement = runeRequirement;
        this.name = this.name();
    }

    @Override
    public int getAction() {
        int action = 1;
        if (this != VARROCK_TELEPORT && this != CAMELOT_TELEPORT && this != WATCHTOWER_TELEPORT && this != GRAND_EXCHANGE_TELEPORT && this != SEERS_TELEPORT && this != YANILLE_TELEPORT) {
            return action;
        }

        if (this == VARROCK_TELEPORT || this == GRAND_EXCHANGE_TELEPORT) {
            action = getVariantAction(VarbitID.VARROCK_GE_TELEPORT, this, VARROCK_TELEPORT, GRAND_EXCHANGE_TELEPORT);
        } else if (this == CAMELOT_TELEPORT || this == SEERS_TELEPORT) {
            action = getVariantAction(VarbitID.SEERS_CAMELOT_TELEPORT, this, CAMELOT_TELEPORT, SEERS_TELEPORT);
        } else if (this == WATCHTOWER_TELEPORT || this == YANILLE_TELEPORT) {
            action = getVariantAction(VarbitID.YANILLE_TELEPORT_LOCATION, this, WATCHTOWER_TELEPORT, YANILLE_TELEPORT);
        }

        return action;
    }

    /**
     * Returns the action for spells which have double teleport actions after certain
     * unlocks. i.e. Medium diaries unlock the ability to teleport to the GE with the Varrock teleport.
     * When the spell cast is GE, then this must return the correct action for the GE teleport, not the Varrock Teleport.
     * @param varbit The varbit denoting the action configuration
     * @param spell The spell being cast by the developer/player
     * @param baseSpell The base spell i.e. Varrock
     * @param variantSpell The variant spell i.e. Grand Exchange
     * @return The action number to send in the widget packet
     */
    private int getVariantAction(int varbit, CastableSpell spell, CastableSpell baseSpell, CastableSpell variantSpell) {
        Context ctx = Services.get(Context.class);
        int config = ctx.getVarbitValue(varbit);
        if (config == 0) {
            return spell == baseSpell ? 1 : 2;
        }
        return spell == variantSpell ? 3 : 2;
    }
}
