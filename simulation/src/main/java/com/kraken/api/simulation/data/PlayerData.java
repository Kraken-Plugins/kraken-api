package com.kraken.api.simulation.data;

import java.util.List;
import java.util.Map;

/**
 * Player side mechanics constants from a pack's player.json: run energy formulas, prayer drain constants, spec
 * regeneration, venom, and the consumable table.
 */
public final class PlayerData {
    /** Run drain per running tick before weight scaling, in hundredths of a percent. */
    public int runDrainBase = 60;
    /** Numerator of the weight term added to the drain base: num x weight / den. */
    public int runDrainWeightNum = 67;
    /** Denominator of the weight term added to the drain base. */
    public int runDrainWeightDen = 64;
    /** Agility term subtrahend: the drain scales by (sub minus agility level) / den. */
    public int runDrainAgilitySub = 300;
    /** Agility term denominator of the run drain. */
    public int runDrainAgilityDen = 300;
    /** Flat part of run regeneration per non running tick, in hundredths of a percent. */
    public int runRegenFlat = 15;
    /** Divisor of the agility level in run regeneration (level / div is added to the flat part). */
    public int runRegenAgilityDiv = 10;
    /** Carried weight in kg is clamped to this before the drain formula. */
    public int weightClampMax = 64;

    /** Ticks between special attack regeneration steps. */
    public int specRegenTicks = 50;
    /** Ticks between special attack regeneration steps with a lightbearer. */
    public int specRegenTicksLightbearer = 25;
    /** Special energy restored per regeneration step, in tenths of a percent (100 = 10%). */
    public int specRegenAmount10 = 100;

    /** Drain effect of an active protection prayer, added to the drain accumulator each tick. */
    public int prayerDrainEffect = 12;
    /** Drain effect of an offensive prayer, in the same units as {@link #prayerDrainEffect}. */
    public int offensiveDrainEffect = 24;
    /** Base prayer drain resistance; the accumulator pays one prayer point per this much. */
    public int prayerResistBase = 60;
    /** Drain resistance gained per point of prayer bonus. */
    public int prayerResistBonusMult = 2;

    /** Damage of the first venom tick. */
    public int venomStartDamage = 6;
    /** Damage added to the venom each time it steps up. */
    public int venomStepDamage = 2;
    /** Highest damage a venom tick reaches. */
    public int venomMaxDamage = 20;
    /** Ticks between venom damage steps. */
    public int venomIntervalTicks = 30;

    /** Consumables a frame starts with until live capture names the items carried (null = first of its category). */
    public String defaultFood;
    /** Name of the karambwan a frame starts with, or null for the first one. */
    public String defaultKarambwan;
    /** Name of the brew a frame starts with, or null for the first one. */
    public String defaultBrew;
    /** Name of the prayer restore a frame starts with, or null for the first one. */
    public String defaultRestore;

    /** Every consumable the pack knows. */
    public List<ConsumableDef> consumables;
    /** Provenance notes keyed by constant name. */
    public Map<String, String> src;

    /** One consumable item. */
    public static final class ConsumableDef {
        /** Item id. */
        public int id;
        /** Item name. */
        public String name;
        /** "food", "karambwan" or "potion". */
        public String category;
        /** Hit points restored per use. */
        public int heal;
        /** Prayer points restored per use. */
        public int prayerRestore;
        /** Ticks the use delays the next use of the same kind. */
        public int delayTicks = 3;
        /** Ticks added to the player's attack cooldown per use. */
        public int attackPushback;
        /** How far above max hp this item can heal (anglerfish). */
        public int overhealCap;
    }

    /**
     * First consumable of a category, or the one with a given name.
     * @param category "food", "karambwan" or "potion"
     * @param name the item name, or null for the first of the category
     * @return the consumable, or null when the table has none
     */
    public ConsumableDef consumable(String category, String name) {
        if (consumables == null) {
            return null;
        }
        for (ConsumableDef c : consumables) {
            if (category.equals(c.category) && (name == null || name.equals(c.name))) {
                return c;
            }
        }
        return null;
    }

    /**
     * Consumable by item id.
     * @param itemId the item id
     * @return the consumable, or null when the table has none
     */
    public ConsumableDef consumableById(int itemId) {
        if (consumables == null) {
            return null;
        }
        for (ConsumableDef c : consumables) {
            if (c.id == itemId) {
                return c;
            }
        }
        return null;
    }

    /**
     * Energy lost per running game tick, in hundredths of a percent of the 0 to 10000 pool
     * (https://oldschool.runescape.wiki/w/Energy).
     * @param weight the player's carried weight in kg
     * @param agilityLevel the player's agility level
     * @return the drain per running tick
     */
    public int runDrainPerTick(int weight, int agilityLevel) {
        int w = Math.min(Math.max(weight, 0), weightClampMax);
        return (runDrainBase + runDrainWeightNum * w / runDrainWeightDen)
                * (runDrainAgilitySub - agilityLevel) / runDrainAgilityDen;
    }

    /**
     * Energy regained per non running game tick, same units.
     * @param agilityLevel the player's agility level
     * @return the regeneration per tick
     */
    public int runRegenPerTick(int agilityLevel) {
        return agilityLevel / runRegenAgilityDiv + runRegenFlat;
    }
}
