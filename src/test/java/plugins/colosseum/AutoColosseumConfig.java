package plugins.colosseum;

import net.runelite.client.config.*;

@ConfigGroup("krakenColosseum")
public interface AutoColosseumConfig extends Config {

    @ConfigItem(
            keyName = "licenseKey",
            name = "License Key",
            description = "License key required to enable the plugin.",
            position = -2,
            secret = true
    )
    default String licenseKey() {
        return "";
    }

    // ==================================
    // Simulation Section
    //===================================

    @ConfigSection(
            name = "Simulation",
            description = "Options to tune the game simulation.",
            position = 10
    )
    String simulationSection = "simulationSection";

    @ConfigItem(
            keyName = "enabled",
            name = "Enable Simulation",
            description = "Capture, plan and visualize every game tick.",
            position = -50,
            section = simulationSection
    )
    default boolean enabled() {
        return true;
    }

    @ConfigItem(
            keyName = "autoExecute",
            name = "Execute Simulation Decisions",
            description = "Execute the planned prayer/eat/gear/move/attack actions that are the outcome of the <br>" +
                    "game simulation",
            position = -48,
            section = simulationSection
    )
    default boolean autoExecute() {
        return false;
    }

    @Units(Units.MILLISECONDS)
    @Range(min = 2, max = 40)
    @ConfigItem(
            keyName = "budgetMillis",
            name = "Simulation Budget",
            description = "The maximum amount of time the simulation can use per game tick to run.",
            position = 11,
            section = simulationSection
    )
    default int budgetMillis() {
        return 15;
    }

    @Units(Units.TICKS)
    @Range(min = 4, max = 30)
    @ConfigItem(
            keyName = "horizonTicks",
            name = "Search Horizon",
            description = "How many ticks in advance of the current tick to simulate.",
            position = 12,
            section = simulationSection
    )
    default int horizonTicks() {
        return 12;
    }

    @Units(Units.TICKS)
    @Range(min = 4, max = 30)
    @ConfigItem(
            keyName = "stage2HorizonTicks",
            name = "S2 Search Horizon",
            description = "How many ticks in advance of the current tick to simulate for second stage simulations <br>" +
                    "(follow-up destinations).",
            position = 13,
            section = simulationSection
    )
    default int stageTwoHorizonTicks() {
        return 12;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(
            keyName = "stage2TopPlans",
            name = "S2 Top Plans",
            description = "Number of top first-stage plans that get second-stage refinement.",
            position = 14,
            section = simulationSection
    )
    default int stageTwoTopPlans() {
        return 12;
    }

    @ConfigItem(
            keyName = "maxDestinations",
            name = "Max Destinations",
            description = "Cap on candidate movement destinations per stage.",
            position = 15,
            section = simulationSection
    )
    default int maxDestinations() {
        return 24;
    }

    @ConfigItem(
            keyName = "maxSafeTiles",
            name = "Max Safe Tiles",
            description = "Cap on zero-exposure 'cover' tiles added as destinations.",
            position = 16,
            section = simulationSection
    )
    default int maxSafeTiles() {
        return 8;
    }

    @ConfigItem(
            keyName = "maxAttackTiles",
            name = "Max Attack Tiles",
            description = "Cap on low-danger attack-position tiles added as destinations.",
            position = 17,
            section = simulationSection
    )
    default int maxAttackTiles() {
        return 6;
    }

    @ConfigItem(
            keyName = "maxTargets",
            name = "Max Targets",
            description = "Cap on candidate attack targets considered per destination.",
            position = 18,
            section = simulationSection
    )
    default int maxTargets() {
        return 3;
    }

    // ==================================
    // Sim Decision Section
    //===================================

    @ConfigSection(
            name = "Simulation Decision",
            description = "Tweaks how futures are scored and decisions are determined.",
            position = 11
    )
    String simulationDecisionSection = "simulationDecision";

    @ConfigItem(
            keyName = "deathPenalty",
            name = "Death Penalty",
            description = "Flat penalty for futures where the expected-damage trajectory kills the player.",
            position = 2,
            section = simulationDecisionSection
    )
    default double deathPenalty() {
        return 1_000_000;
    }

    @ConfigItem(
            keyName = "lethalRiskPerHp",
            name = "Lethal Risk Per HP",
            description = "Penalty per hitpoint the worst-case burst floor dips below zero (could have died).",
            position = 3,
            section = simulationDecisionSection
    )
    default double lethalRiskPerHp() {
        return 4_000;
    }

    @ConfigItem(
            keyName = "hpWeight",
            name = "HP Weight",
            description = "Reward per remaining player hitpoint at the horizon.",
            position = 4,
            section = simulationDecisionSection
    )
    default double hpWeight() {
        return 6;
    }

    @ConfigItem(
            keyName = "damageTakenWeight",
            name = "Damage Taken Weight",
            description = "Penalty per expected hitpoint of damage taken during the rollout.",
            position = 5,
            section = simulationDecisionSection
    )
    default double damageTakenWeight() {
        return 14;
    }

    @ConfigItem(
            keyName = "killWeight",
            name = "Kill Weight",
            description = "Reward per NPC killed during the rollout.",
            position = 6,
            section = simulationDecisionSection
    )
    default double killWeight() {
        return 900;
    }

    @ConfigItem(
            keyName = "damageDealtWeight",
            name = "Damage Dealt Weight",
            description = "Reward per expected hitpoint of damage dealt.",
            position = 7,
            section = simulationDecisionSection
    )
    default double damageDealtWeight() {
        return 6;
    }

    @ConfigItem(
            keyName = "supplyWeight",
            name = "Supply Weight",
            description = "Penalty per supply consumed.",
            position = 8,
            section = simulationDecisionSection
    )
    default double supplyWeight() {
        return 25;
    }

    @ConfigItem(
            keyName = "prayerWeight",
            name = "Prayer Weight",
            description = "Penalty per prayer point below the cap at the horizon.",
            position = 9,
            section = simulationDecisionSection
    )
    default double prayerWeight() {
        return 2.5;
    }

    @ConfigItem(
            keyName = "endExposureWeight",
            name = "End Exposure Weight",
            description = "Penalty per NPC that has line of sight to the player at the horizon.",
            position = 10,
            section = simulationDecisionSection
    )
    default double endExposureWeight() {
        return 30;
    }

    @ConfigItem(
            keyName = "runEnergyWeight",
            name = "Run Energy Weight",
            description = "Small reward for retaining run energy (units of 1%).",
            position = 11,
            section = simulationDecisionSection
    )
    default double runEnergyWeight() {
        return 0.4;
    }

    // ==================================
    // Food & Potions Section
    //===================================

    @ConfigSection(
            name = "Food & Potions",
            description = "Options to tune when to eat food and drink potions.",
            position = 12
    )
    String foodAndPotions = "foodAndPotions";

    @Range(min = 1, max = 98)
    @ConfigItem(
            keyName = "eatFoodAtHp",
            name = "Eat at HP",
            description = "Eats primary food at or below this HP.",
            position = 15,
            section = foodAndPotions
    )
    default int eatFoodAtHp() {
        return 48;
    }

    @Range(min = 1, max = 98)
    @ConfigItem(
            keyName = "eatComboAtHp",
            name = "Combo Eat at HP",
            description = "Eats a combo eat if available at or below this HP.",
            position = 13,
            section = foodAndPotions
    )
    default int eatComboAt() {
        return 34;
    }

    @Range(min = 1, max = 98)
    @ConfigItem(
            keyName = "sipBrew",
            name = "Sip Saradomin Brew",
            description = "Sips a Saradomin brew at or below this HP when out of hard food.",
            position = 14,
            section = foodAndPotions
    )
    default int sipBrew() {
        return 55;
    }

    @Range(min = 1, max = 77)
    @ConfigItem(
            keyName = "sipRestore",
            name = "Sip Super Restore",
            description = "Sips a Super Restore at or below this Prayer",
            position = 15,
            section = foodAndPotions
    )
    default int sipRestore() {
        return 15;
    }

    @ConfigSection(
            name = "Gear",
            description = "Weapon and armour ids for each combat style. Wear a setup, then use the "
                    + "copy buttons and paste the ids into the matching style's fields.",
            position = 13
    )
    String gearSection = "gearSection";

    @ConfigItem(
            keyName = "copyWeaponButton",
            name = "Copy Weapon",
            description = "Copy the id of your currently equipped weapon to the clipboard.",
            position = 21,
            section = gearSection
    )
    default boolean copyWeaponButton() {
        return false;
    }

    @ConfigItem(
            keyName = "copyGearButton",
            name = "Copy Gear",
            description = "Copy the ids of everything you are wearing except the weapon to the clipboard.",
            position = 22,
            section = gearSection
    )
    default boolean copyGearButton() {
        return false;
    }

    @ConfigItem(
            keyName = "meleeWeaponIds",
            name = "Melee weapon",
            description = "Item id(s) of the melee weapon (comma separated for e.g. weapon + defender).",
            position = 23,
            section = gearSection
    )
    default String meleeWeaponIds() {
        return "";
    }

    @ConfigItem(
            keyName = "meleeGearIds",
            name = "Melee gear",
            description = "Comma-separated armour ids to swap alongside the melee weapon.",
            position = 24,
            section = gearSection
    )
    default String meleeGearIds() {
        return "";
    }

    @ConfigItem(
            keyName = "rangedWeaponIds",
            name = "Ranged weapon",
            description = "Item id(s) of the ranged weapon.",
            position = 25,
            section = gearSection
    )
    default String rangedWeaponIds() {
        return "";
    }

    @ConfigItem(
            keyName = "rangedGearIds",
            name = "Ranged gear",
            description = "Comma-separated armour ids to swap alongside the ranged weapon.",
            position = 26,
            section = gearSection
    )
    default String rangedGearIds() {
        return "";
    }

    @ConfigItem(
            keyName = "magicWeaponIds",
            name = "Magic weapon",
            description = "Item id(s) of the magic weapon.",
            position = 27,
            section = gearSection
    )
    default String magicWeaponIds() {
        return "";
    }

    @ConfigItem(
            keyName = "magicGearIds",
            name = "Magic gear",
            description = "Comma-separated armour ids to swap alongside the magic weapon.",
            position = 28,
            section = gearSection
    )
    default String magicGearIds() {
        return "";
    }

    @ConfigSection(
            name = "Overlay",
            description = "Debug visualization.",
            position = 14
    )
    String overlaySection = "overlaySection";

    @ConfigItem(
            keyName = "showCandidateTiles",
            name = "Show tile scores",
            description = "Heat map of candidate destination scores (green best, red worst).",
            position = 31,
            section = overlaySection
    )
    default boolean showCandidateTiles() {
        return true;
    }

    @ConfigItem(
            keyName = "showDangerTiles",
            name = "Show danger tiles",
            description = "Tiles currently attackable by NPCs, shaded by expected damage.",
            position = 32,
            section = overlaySection
    )
    default boolean showDangerTiles() {
        return true;
    }

    @ConfigItem(
            keyName = "dangerRadius",
            name = "Danger Radius",
            description = "The radius around the player of dangerous tiles. Defaults to 12",
            position = 33,
            section = overlaySection
    )
    default int dangerRadius() {
        return 12;
    }

    @ConfigItem(
            keyName = "showPlannedPath",
            name = "Show planned path",
            description = "Predicted player path along the chosen plan.",
            position = 33,
            section = overlaySection
    )
    default boolean showPlannedPath() {
        return true;
    }

    @ConfigItem(
            keyName = "showNpcPaths",
            name = "Show NPC paths",
            description = "Predicted NPC movement over the next few ticks.",
            position = 34,
            section = overlaySection
    )
    default boolean showNpcPaths() {
        return true;
    }

    @ConfigItem(
            keyName = "showInfoPanel",
            name = "Show info panel",
            description = "Decision reasoning and planner statistics panel.",
            position = 35,
            section = overlaySection
    )
    default boolean showInfoPanel() {
        return true;
    }

    @Range(min = 2, max = 12)
    @ConfigItem(
            keyName = "npcPathTicks",
            name = "NPC path ticks",
            description = "How many ticks of NPC movement to predict for the overlay.",
            position = 36,
            section = overlaySection
    )
    default int npcPathTicks() {
        return 6;
    }
}
