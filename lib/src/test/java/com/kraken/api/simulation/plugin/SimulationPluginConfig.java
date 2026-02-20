package com.kraken.api.simulation.plugin;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("krakenSimulation")
public interface SimulationPluginConfig extends Config {
    @ConfigItem(
            keyName = "enabled",
            name = "Enable Simulation Plugin",
            description = "Enable simulation snapshotting, search, and overlays.",
            position = 0
    )
    default boolean enabled() {
        return true;
    }

    @ConfigSection(
            name = "Search",
            description = "Decision-tree search settings.",
            position = 10
    )
    String searchSection = "searchSection";

    @Range(min = 1, max = 64)
    @ConfigItem(
            keyName = "snapshotNpcRadius",
            name = "Snapshot NPC Radius",
            description = "Chebyshev radius from player used while capturing NPCs into simulation.",
            section = searchSection,
            position = 11
    )
    default int snapshotNpcRadius() {
        return 24;
    }

    @Range(min = 1, max = 5)
    @ConfigItem(
            keyName = "searchDepth",
            name = "Search Depth",
            description = "Decision tree depth in ticks.",
            section = searchSection,
            position = 12
    )
    default int searchDepth() {
        return 2;
    }

    @Range(min = 64, max = 50000)
    @ConfigItem(
            keyName = "maxSearchNodes",
            name = "Max Search Nodes",
            description = "Hard cap on explored tree nodes per game tick.",
            section = searchSection,
            position = 13
    )
    default int maxSearchNodes() {
        return 4000;
    }

    @ConfigItem(
            keyName = "includeRunActions",
            name = "Include Run Actions",
            description = "Include run-style candidate actions (2-tile movement) in search.",
            section = searchSection,
            position = 14
    )
    default boolean includeRunActions() {
        return false;
    }

    @ConfigSection(
            name = "Combat Simulation",
            description = "NPC combat metadata and player consumable simulation mappings.",
            position = 15
    )
    String combatSection = "combatSection";

    @ConfigItem(
            keyName = "npcCombatOverrides",
            name = "NPC Combat Overrides",
            description = "CSV: npcId=STYLE:range:speed:maxHit (example: 415=MELEE:1:4:30,3129=MAGIC:10:4:20).",
            section = combatSection,
            position = 16
    )
    default String npcCombatOverrides() {
        return "";
    }

    @ConfigItem(
            keyName = "foodHealingOverrides",
            name = "Food Heal Overrides",
            description = "CSV: itemId=heal (example: 385=20,3144=18).",
            section = combatSection,
            position = 17
    )
    default String foodHealingOverrides() {
        return "";
    }

    @ConfigSection(
            name = "Action Candidates",
            description = "Which non-movement action types should be generated in the decision tree.",
            position = 18
    )
    String candidateSection = "candidateSection";

    @ConfigItem(
            keyName = "includePrayerActions",
            name = "Include Prayer Actions",
            description = "Generate switch-prayer actions based on simulated NPC threats.",
            section = candidateSection,
            position = 19
    )
    default boolean includePrayerActions() {
        return true;
    }

    @ConfigItem(
            keyName = "includeEatActions",
            name = "Include Eat Actions",
            description = "Generate eat/drink inventory actions when HP is low.",
            section = candidateSection,
            position = 20
    )
    default boolean includeEatActions() {
        return true;
    }

    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "eatAtOrBelowHp",
            name = "Eat At Or Below HP",
            description = "Generate eat actions when simulated HP is at or below this threshold.",
            section = candidateSection,
            position = 21
    )
    default int eatAtOrBelowHp() {
        return 45;
    }

    @ConfigItem(
            keyName = "includeGearSwapActions",
            name = "Include Gear Swap Actions",
            description = "Generate equip-item actions for configured gear item id.",
            section = candidateSection,
            position = 22
    )
    default boolean includeGearSwapActions() {
        return false;
    }

    @ConfigItem(
            keyName = "gearSwapItemId",
            name = "Gear Swap Item ID",
            description = "Inventory item id used when generating equip actions.",
            section = candidateSection,
            position = 23
    )
    default int gearSwapItemId() {
        return -1;
    }

    @ConfigItem(
            keyName = "includeSpellActions",
            name = "Include Spell Actions",
            description = "Generate cast-spell actions for configured standard spell name.",
            section = candidateSection,
            position = 24
    )
    default boolean includeSpellActions() {
        return false;
    }

    @ConfigItem(
            keyName = "standardSpellName",
            name = "Standard Spell Name",
            description = "Name of Standard spell enum (example: WIND_STRIKE, FIRE_BLAST).",
            section = candidateSection,
            position = 25
    )
    default String standardSpellName() {
        return "WIND_STRIKE";
    }

    @Range(min = 1, max = 15)
    @ConfigItem(
            keyName = "spellTargetDistance",
            name = "Spell Target Distance",
            description = "Max distance for selecting spell target NPC.",
            section = candidateSection,
            position = 26
    )
    default int spellTargetDistance() {
        return 10;
    }

    @ConfigSection(
            name = "Execution",
            description = "Apply search result to the live game.",
            position = 20
    )
    String executionSection = "executionSection";

    @ConfigItem(
            keyName = "autoExecuteBestAction",
            name = "Auto Execute Best Action",
            description = "Automatically execute the adapted action each game tick.",
            section = executionSection,
            position = 21
    )
    default boolean autoExecuteBestAction() {
        return false;
    }

    @ConfigItem(
            keyName = "executeNpcInteraction",
            name = "Execute NPC Interaction",
            description = "When enabled, include an NPC interaction target/action in adapted execution.",
            section = executionSection,
            position = 22
    )
    default boolean executeNpcInteraction() {
        return false;
    }

    @ConfigItem(
            keyName = "interactionAction",
            name = "Interaction Action",
            description = "NPC menu action to execute from adapter (example: Attack, Talk-to).",
            section = executionSection,
            position = 23
    )
    default String interactionAction() {
        return "Attack";
    }

    @Range(min = 1, max = 12)
    @ConfigItem(
            keyName = "interactionDistance",
            name = "Interaction Distance",
            description = "Max Chebyshev tile distance for choosing interaction NPC target.",
            section = executionSection,
            position = 24
    )
    default int interactionDistance() {
        return 1;
    }

    @ConfigItem(
            keyName = "executePrayerSwitches",
            name = "Execute Prayer Switches",
            description = "Allow adapter to execute prayer switching steps.",
            section = executionSection,
            position = 25
    )
    default boolean executePrayerSwitches() {
        return true;
    }

    @ConfigItem(
            keyName = "executeGearSwaps",
            name = "Execute Gear Swaps",
            description = "Allow adapter to execute equipment swap steps.",
            section = executionSection,
            position = 26
    )
    default boolean executeGearSwaps() {
        return true;
    }

    @ConfigItem(
            keyName = "executeInventoryActions",
            name = "Execute Inventory Actions",
            description = "Allow adapter to execute inventory interact/eat steps.",
            section = executionSection,
            position = 27
    )
    default boolean executeInventoryActions() {
        return true;
    }

    @ConfigItem(
            keyName = "executeSpells",
            name = "Execute Spell Actions",
            description = "Allow adapter to execute cast-spell steps.",
            section = executionSection,
            position = 28
    )
    default boolean executeSpells() {
        return true;
    }

    @ConfigSection(
            name = "Overlay",
            description = "Visualization options for simulation state and decisions.",
            position = 30
    )
    String overlaySection = "overlaySection";

    @ConfigItem(
            keyName = "showSceneOverlay",
            name = "Show Scene Overlay",
            description = "Show simulation visualizations in the 3D scene.",
            section = overlaySection,
            position = 31
    )
    default boolean showSceneOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "showInfoOverlay",
            name = "Show Info Overlay",
            description = "Show simulation summary panel.",
            section = overlaySection,
            position = 32
    )
    default boolean showInfoOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "showBestMoveTile",
            name = "Show Best Move Tile",
            description = "Highlight the best action's destination tile.",
            section = overlaySection,
            position = 33
    )
    default boolean showBestMoveTile() {
        return true;
    }

    @ConfigItem(
            keyName = "showNpcPaths",
            name = "Show NPC Paths",
            description = "Render predicted NPC greedy paths toward player.",
            section = overlaySection,
            position = 34
    )
    default boolean showNpcPaths() {
        return true;
    }

    @ConfigItem(
            keyName = "showNpcLosTiles",
            name = "Show NPC LoS Tiles",
            description = "Render line-of-sight tiles for visualized NPCs.",
            section = overlaySection,
            position = 35
    )
    default boolean showNpcLosTiles() {
        return false;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "maxVisualizedNpcs",
            name = "Max Visualized NPCs",
            description = "Maximum NPCs to render each frame.",
            section = overlaySection,
            position = 36
    )
    default int maxVisualizedNpcs() {
        return 8;
    }

    @Range(min = 1, max = 30)
    @ConfigItem(
            keyName = "maxNpcPathLength",
            name = "Max NPC Path Length",
            description = "Max predicted steps rendered per NPC path.",
            section = overlaySection,
            position = 37
    )
    default int maxNpcPathLength() {
        return 10;
    }

    @Range(min = 1, max = 30)
    @ConfigItem(
            keyName = "npcLosRangeCap",
            name = "NPC LoS Range Cap",
            description = "Maximum LoS scan range when rendering NPC LoS tiles.",
            section = overlaySection,
            position = 38
    )
    default int npcLosRangeCap() {
        return 12;
    }

    @ConfigItem(
            keyName = "showNpcDebugLabels",
            name = "Show NPC Debug Labels",
            description = "Show per-NPC labels for index/path length.",
            section = overlaySection,
            position = 39
    )
    default boolean showNpcDebugLabels() {
        return false;
    }

    @Alpha
    @ConfigItem(
            keyName = "bestMoveColor",
            name = "Best Move Color",
            description = "Color for best move tile overlay.",
            section = overlaySection,
            position = 40
    )
    default Color bestMoveColor() {
        return new Color(32, 220, 120, 180);
    }

    @Alpha
    @ConfigItem(
            keyName = "npcPathColor",
            name = "NPC Path Base Color",
            description = "Base color for NPC path overlays.",
            section = overlaySection,
            position = 41
    )
    default Color npcPathColor() {
        return new Color(255, 170, 30, 170);
    }

    @Alpha
    @ConfigItem(
            keyName = "npcLosColor",
            name = "NPC LoS Base Color",
            description = "Base color for NPC LoS overlays.",
            section = overlaySection,
            position = 42
    )
    default Color npcLosColor() {
        return new Color(40, 170, 255, 100);
    }
}
