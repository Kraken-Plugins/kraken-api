package plugins.simulation;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;

@ConfigGroup("krakenSimulation")
public interface SimulationPluginConfig extends Config {
    @ConfigItem(
            keyName = "enabled",
            name = "Enable Simulation Plugin",
            description = "Enable snapshot capture, tree generation, and decision search.",
            position = 0
    )
    default boolean enabled() {
        return true;
    }

    @ConfigSection(
            name = "Search",
            description = "Simulation tree and movement expansion settings.",
            position = 10
    )
    String searchSection = "searchSection";

    @Range(min = 1, max = 64)
    @ConfigItem(
            keyName = "snapshotNpcRadius",
            name = "Snapshot NPC Radius",
            description = "Chebyshev radius from player while capturing NPCs.",
            section = searchSection,
            position = 11
    )
    default int snapshotNpcRadius() {
        return 24;
    }

    @Range(min = 1, max = 30)
    @ConfigItem(
            keyName = "searchDepth",
            name = "Simulation Ticks",
            description = "How many ticks to simulate into the future.",
            section = searchSection,
            position = 12
    )
    default int searchDepth() {
        return 15;
    }

    @Range(min = 256, max = 100000)
    @ConfigItem(
            keyName = "maxSearchNodes",
            name = "Max Tree Nodes",
            description = "Hard cap on generated tree nodes per game tick.",
            section = searchSection,
            position = 13
    )
    default int maxSearchNodes() {
        return 20000;
    }

    @ConfigItem(
            keyName = "movementMode",
            name = "Movement Mode",
            description = "RADIUS or REACHABLE destination expansion.",
            section = searchSection,
            position = 14
    )
    default String movementMode() {
        return "RADIUS";
    }

    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "movementRadius",
            name = "Movement Radius",
            description = "Destination radius for RADIUS movement mode.",
            section = searchSection,
            position = 15
    )
    default int movementRadius() {
        return 6;
    }

    @ConfigItem(
            keyName = "includeWalkActions",
            name = "Include Walk Actions",
            description = "Generate 1-step movement actions for reachable destinations.",
            section = searchSection,
            position = 16
    )
    default boolean includeWalkActions() {
        return true;
    }

    @ConfigItem(
            keyName = "includeRunActions",
            name = "Include Run Actions",
            description = "Generate 2-step movement actions for reachable destinations.",
            section = searchSection,
            position = 17
    )
    default boolean includeRunActions() {
        return true;
    }

    @Range(min = 1, max = 400)
    @ConfigItem(
            keyName = "maxMovementTargets",
            name = "Max Movement Targets",
            description = "Maximum movement destinations generated per node before walk/run variants.",
            section = searchSection,
            position = 18
    )
    default int maxMovementTargets() {
        return 80;
    }

    @Range(min = 1, max = 400)
    @ConfigItem(
            keyName = "maxActionsPerNode",
            name = "Max Actions Per Node",
            description = "Maximum actions kept per node after legality filtering.",
            section = searchSection,
            position = 19
    )
    default int maxActionsPerNode() {
        return 120;
    }

    @ConfigSection(
            name = "Combat Simulation",
            description = "NPC profile mapping and player consumable mapping.",
            position = 20
    )
    String combatSection = "combatSection";

    @ConfigItem(
            keyName = "npcCombatOverrides",
            name = "NPC Profiles",
            description = "CSV: npcId=STYLE:range:speed:maxHit:intelligent(0/1). Example 415=MELEE:1:4:30:1",
            section = combatSection,
            position = 21
    )
    default String npcCombatOverrides() {
        return "";
    }

    @ConfigItem(
            keyName = "foodHealingOverrides",
            name = "Food Heal Mapping",
            description = "CSV: itemId=heal (example: 385=20,3144=18).",
            section = combatSection,
            position = 22
    )
    default String foodHealingOverrides() {
        return "";
    }

    @ConfigSection(
            name = "Action Candidates",
            description = "Optional non-movement actions to add to the tree.",
            position = 30
    )
    String candidateSection = "candidateSection";

    @ConfigItem(
            keyName = "includePrayerActions",
            name = "Include Prayer Actions",
            description = "Generate switch-prayer actions based on simulated threats.",
            section = candidateSection,
            position = 31
    )
    default boolean includePrayerActions() {
        return true;
    }

    @ConfigItem(
            keyName = "includeEatActions",
            name = "Include Eat Actions",
            description = "Generate eat actions when HP is low and food is mapped.",
            section = candidateSection,
            position = 32
    )
    default boolean includeEatActions() {
        return true;
    }

    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "eatAtOrBelowHp",
            name = "Eat At Or Below HP",
            description = "Generate eat actions when HP is <= this value.",
            section = candidateSection,
            position = 33
    )
    default int eatAtOrBelowHp() {
        return 45;
    }

    @ConfigItem(
            keyName = "includeGearSwapActions",
            name = "Include Gear Swap Actions",
            description = "Generate equip-item actions for gearSwapItemId.",
            section = candidateSection,
            position = 34
    )
    default boolean includeGearSwapActions() {
        return false;
    }

    @ConfigItem(
            keyName = "gearSwapItemId",
            name = "Gear Swap Item ID",
            description = "Inventory item id used for equip actions.",
            section = candidateSection,
            position = 35
    )
    default int gearSwapItemId() {
        return -1;
    }

    @ConfigItem(
            keyName = "includeSpellActions",
            name = "Include Spell Actions",
            description = "Generate cast-spell actions for standardSpellName.",
            section = candidateSection,
            position = 36
    )
    default boolean includeSpellActions() {
        return false;
    }

    @ConfigItem(
            keyName = "standardSpellName",
            name = "Standard Spell Name",
            description = "Name of Standard enum spell (example: WIND_STRIKE).",
            section = candidateSection,
            position = 37
    )
    default String standardSpellName() {
        return "WIND_STRIKE";
    }

    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "spellTargetDistance",
            name = "Spell Target Distance",
            description = "Max distance for selecting spell target.",
            section = candidateSection,
            position = 38
    )
    default int spellTargetDistance() {
        return 10;
    }

    @ConfigSection(
            name = "Execution",
            description = "Apply best searched action to live game.",
            position = 40
    )
    String executionSection = "executionSection";

    @ConfigItem(
            keyName = "autoExecuteBestAction",
            name = "Auto Execute Best Action",
            description = "Automatically execute adapted action each game tick.",
            section = executionSection,
            position = 41
    )
    default boolean autoExecuteBestAction() {
        return false;
    }

    @ConfigItem(
            keyName = "executeNpcInteraction",
            name = "Execute NPC Interaction",
            description = "Allow NPC interaction steps during execution.",
            section = executionSection,
            position = 42
    )
    default boolean executeNpcInteraction() {
        return false;
    }

    @ConfigItem(
            keyName = "interactionAction",
            name = "Interaction Action",
            description = "NPC interaction action text (example: Attack).",
            section = executionSection,
            position = 43
    )
    default String interactionAction() {
        return "Attack";
    }

    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "interactionDistance",
            name = "Interaction Distance",
            description = "Max distance for selecting optional interaction target.",
            section = executionSection,
            position = 44
    )
    default int interactionDistance() {
        return 1;
    }

    @ConfigItem(
            keyName = "executePrayerSwitches",
            name = "Execute Prayer Switches",
            description = "Allow prayer switch steps.",
            section = executionSection,
            position = 45
    )
    default boolean executePrayerSwitches() {
        return true;
    }

    @ConfigItem(
            keyName = "executeGearSwaps",
            name = "Execute Gear Swaps",
            description = "Allow equip-item steps.",
            section = executionSection,
            position = 46
    )
    default boolean executeGearSwaps() {
        return true;
    }

    @ConfigItem(
            keyName = "executeInventoryActions",
            name = "Execute Inventory Actions",
            description = "Allow inventory interaction steps.",
            section = executionSection,
            position = 47
    )
    default boolean executeInventoryActions() {
        return true;
    }

    @ConfigItem(
            keyName = "executeSpells",
            name = "Execute Spell Actions",
            description = "Allow spell cast steps.",
            section = executionSection,
            position = 48
    )
    default boolean executeSpells() {
        return true;
    }

    @ConfigSection(
            name = "Overlay",
            description = "Visualization options for simulation state and decisions.",
            position = 50
    )
    String overlaySection = "overlaySection";

    @ConfigItem(
            keyName = "showSceneOverlay",
            name = "Show Scene Overlay",
            description = "Show simulation visuals in the 3D scene.",
            section = overlaySection,
            position = 51
    )
    default boolean showSceneOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "showInfoOverlay",
            name = "Show Info Overlay",
            description = "Show simulation summary panel.",
            section = overlaySection,
            position = 52
    )
    default boolean showInfoOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "showBestMoveTile",
            name = "Show Best Move Tile",
            description = "Highlight the best action destination.",
            section = overlaySection,
            position = 53
    )
    default boolean showBestMoveTile() {
        return true;
    }

    @ConfigItem(
            keyName = "showNpcPaths",
            name = "Show NPC Paths",
            description = "Render predicted npc paths.",
            section = overlaySection,
            position = 54
    )
    default boolean showNpcPaths() {
        return true;
    }

    @ConfigItem(
            keyName = "showNpcLosTiles",
            name = "Show NPC LoS Tiles",
            description = "Render visible line-of-sight tiles for visualized npcs.",
            section = overlaySection,
            position = 55
    )
    default boolean showNpcLosTiles() {
        return false;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "maxVisualizedNpcs",
            name = "Max Visualized NPCs",
            description = "Maximum npc overlays rendered each frame.",
            section = overlaySection,
            position = 56
    )
    default int maxVisualizedNpcs() {
        return 8;
    }

    @Range(min = 1, max = 30)
    @ConfigItem(
            keyName = "maxNpcPathLength",
            name = "Max NPC Path Length",
            description = "Maximum predicted path tiles rendered per npc.",
            section = overlaySection,
            position = 57
    )
    default int maxNpcPathLength() {
        return 12;
    }

    @Range(min = 1, max = 30)
    @ConfigItem(
            keyName = "npcLosRangeCap",
            name = "NPC LoS Range Cap",
            description = "Maximum range used while rendering npc LoS tiles.",
            section = overlaySection,
            position = 58
    )
    default int npcLosRangeCap() {
        return 12;
    }

    @ConfigItem(
            keyName = "showNpcDebugLabels",
            name = "Show NPC Debug Labels",
            description = "Render npc debug labels in scene.",
            section = overlaySection,
            position = 59
    )
    default boolean showNpcDebugLabels() {
        return false;
    }

    @Alpha
    @ConfigItem(
            keyName = "bestMoveColor",
            name = "Best Move Color",
            description = "Color for the best move tile overlay.",
            section = overlaySection,
            position = 60
    )
    default Color bestMoveColor() {
        return new Color(32, 220, 120, 180);
    }

    @Alpha
    @ConfigItem(
            keyName = "npcPathColor",
            name = "NPC Path Base Color",
            description = "Base color for npc path overlays.",
            section = overlaySection,
            position = 61
    )
    default Color npcPathColor() {
        return new Color(255, 170, 30, 170);
    }

    @Alpha
    @ConfigItem(
            keyName = "npcLosColor",
            name = "NPC LoS Base Color",
            description = "Base color for npc LoS overlays.",
            section = overlaySection,
            position = 62
    )
    default Color npcLosColor() {
        return new Color(40, 170, 255, 100);
    }
}
