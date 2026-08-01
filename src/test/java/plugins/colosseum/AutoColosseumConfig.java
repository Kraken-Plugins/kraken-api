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
    // Food & Potions Section
    //===================================

    @ConfigSection(
            name = "Food & Potions",
            description = "Options to tune when to eat food and drink potions.",
            position = 11
    )
    String foodAndPotions = "foodAndPotions";

    @Range(min = 1, max = 98)
    @ConfigItem(
            keyName = "eatFoodAtHp",
            name = "Eat at HP",
            description = "Rollout policy eats primary food at or below this HP (0 uses default of 48).",
            position = 15,
            section = foodAndPotions
    )
    default int eatFoodAtHp() {
        return 0;
    }

    @Range(min = 1, max = 98)
    @ConfigItem(
            keyName = "eatComboAtHp",
            name = "Combo Eat at HP",
            description = "Eats a combo eat if available at or below this HP (0 uses default of 34).",
            position = 13,
            section = foodAndPotions
    )
    default int eatComboAt() {
        return 0;
    }

    @ConfigSection(
            name = "Gear",
            description = "Weapon and armour ids for each combat style. Wear a setup, then use the "
                    + "copy buttons and paste the ids into the matching style's fields.",
            position = 20
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
            position = 30
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
