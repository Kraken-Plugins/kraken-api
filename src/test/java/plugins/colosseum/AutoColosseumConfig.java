package plugins.colosseum;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import javax.swing.JButton;

@ConfigGroup("krakenColosseum")
public interface AutoColosseumConfig extends Config {
    @ConfigItem(
            keyName = "enabled",
            name = "Enable planner",
            description = "Capture, plan and visualize every game tick.",
            position = 0
    )
    default boolean enabled() {
        return true;
    }

    @ConfigItem(
            keyName = "autoExecute",
            name = "Auto execute decisions",
            description = "Execute the planned prayer/eat/gear/move/attack actions through the Kraken API.",
            position = 1
    )
    default boolean autoExecute() {
        return false;
    }

    @ConfigSection(
            name = "Planner",
            description = "Search tuning.",
            position = 10
    )
    String plannerSection = "plannerSection";

    @Range(min = 2, max = 40)
    @ConfigItem(
            keyName = "budgetMillis",
            name = "Budget (ms)",
            description = "Wall-clock planning budget per tick.",
            position = 11,
            section = plannerSection
    )
    default int budgetMillis() {
        return 15;
    }

    @Range(min = 4, max = 30)
    @ConfigItem(
            keyName = "horizonTicks",
            name = "Horizon (ticks)",
            description = "Rollout depth per candidate plan.",
            position = 12,
            section = plannerSection
    )
    default int horizonTicks() {
        return 12;
    }

    @Range(min = 0, max = 30)
    @ConfigItem(
            keyName = "eatFoodAtHp",
            name = "Eat at HP",
            description = "Rollout policy eats primary food at or below this HP (0 uses default).",
            position = 13,
            section = plannerSection
    )
    default int eatFoodAtHp() {
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
    default JButton copyWeaponButton() {
        return new JButton("Copy Weapon");
    }

    @ConfigItem(
            keyName = "copyGearButton",
            name = "Copy Gear",
            description = "Copy the ids of everything you are wearing except the weapon to the clipboard.",
            position = 22,
            section = gearSection
    )
    default JButton copyGearButton() {
        return new JButton("Copy Gear");
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
