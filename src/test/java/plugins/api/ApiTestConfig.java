
package plugins.api;

import com.kraken.api.input.mouse.strategy.MouseMovementStrategy;
import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("testapi")
public interface ApiTestConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "General options for configuring tests.",
            position = -1000
    )
    String general = "general";

    @ConfigItem(
            name = "Clear Tests",
            keyName = "clearTests",
            description = "clear the execution of the configured tests. <br>" +
                    "This produces a clean slate for running or re-running tests.",
            position = -999,
            section = general
    )
    default boolean clearTests() {
        return false;
    }

    @ConfigItem(
            keyName = "showDebugInfo",
            name = "Show Debug Info",
            description = "Display additional debug information in overlays and within the logs.",
            section = general,
            position = -6
    )
    default boolean showDebugInfo() {
        return false;
    }

    @ConfigItem(
            name = "Pause Script",
            keyName = "pauseScript",
            description = "Pauses the example script loop which runs in the background.",
            position = -5,
            section = general
    )
    default boolean pauseScript() {
        return false;
    }

    @ConfigSection(
            name = "Game State",
            description = "Tests for game state actions like logging in and out",
            position = -1
    )
    String gameState = "Game State";

    @ConfigItem(
            name = "Login",
            keyName = "login",
            description = "Logs into the client using the preloaded jagex account.",
            position = -2,
            section = gameState
    )
    default boolean login() {
        return false;
    }

    @ConfigItem(
            name = "Logout",
            keyName = "logout",
            description = "Logs out of the client.",
            section = gameState
    )
    default boolean logout() {
        return false;
    }

    // =========== Widget Actions Section ================
    @ConfigSection(
            name = "Widget Actions",
            description = "Tests for using widgets on various game entities. <br>" +
                    "I.e. spell on npc, chisel on gemstone, bucket on fountain",
            position = 98
    )
    String widgetActions = "Widget Actions";

    @ConfigItem(
            name = "Widget Target NPC",
            keyName = "widgetTargetOnNpc",
            description = "Targets a Guard in Varrock with fire strike.",
            position = 1,
            section = widgetActions
    )
    default boolean widgetTargetOnNpc() {
        return false;
    }

    @ConfigItem(
            name = "Widget Target Game Object",
            keyName = "widgetTargetOnGameObject",
            description = "Targets an empty bucket to be used on a fountain in Varrock",
            position = 2,
            section = widgetActions
    )
    default boolean widgetTargetOnGameObject() {
        return false;
    }

    @ConfigItem(
            name = "Widget Target Widget",
            keyName = "widgetTargetOnWidget",
            description = "Targets a chisel to be used on a sapphire. <br>" +
                    "Have a chisel and uncut sapphire in your inventory",
            position = 3,
            section = widgetActions
    )
    default boolean widgetTargetOnWidget() {
        return false;
    }

    @ConfigItem(
            name = "Widget Sub Action",
            keyName = "widgetSubAction",
            description = "Targets a sub action of the ring of dueling teleport.",
            position = 4,
            section = widgetActions
    )
    default boolean widgetSubAction() {
        return false;
    }

    @ConfigItem(
            name = "Widget Action",
            keyName = "widgetAction",
            description = "Uses a standard cc op widget action (clicking on spec orb).",
            position = 5,
            section = widgetActions
    )
    default boolean widgetAction() {
        return false;
    }

    // =========== Pathfinding Section ================
    @ConfigSection(
            name = "Pathfinding Tests",
            description = "Settings for enabling and testing pathfinding across the world",
            position = 99
    )
    String pathfinding = "pathfinding";

    /**
     * Defines the pathfinder test target in world coordinates.
     *
     * @return Target coordinates in the format x,y,z. Leave blank to use a selected tile.
     */
    @ConfigItem(
            name = "Local Pathfinder Target",
            keyName = "pathfinderTestTarget",
            description = "Target coordinates for the pathfinder service test in the format: x,y,z.",
            position = 2,
            section = pathfinding
    )
    default String pathfinderTestTarget() {
        return "";
    }

    @ConfigItem(
            keyName = "enablePathfinder",
            name = "Start Local Pathfinder",
            description = "Enable pathfinder service tests",
            section = pathfinding,
            position = 3
    )
    default boolean enablePathfinder() {
        return true;
    }

    @ConfigItem(
            name = "Global Pathfinder Target",
            keyName = "globalPathfinderTarget",
            description = "Target coordinates for the global pathfinder test in the format: x,y,z. Leave blank to use the selected tile.",
            position = 4,
            section = pathfinding
    )
    default String globalPathfinderTarget() {
        return "";
    }

    @ConfigItem(
            keyName = "enableGlobalPathfinder",
            name = "Start Global Pathfinder",
            description = "Run the global pathfinder service test using the shortest-path transport graph.",
            section = pathfinding,
            position = 5
    )
    default boolean enableGlobalPathfinder() {
        return false;
    }

    @ConfigItem(
            keyName = "showGlobalPathfinderOverlay",
            name = "Show Global Path Overlay",
            description = "Render the most recently computed global path and any transport hops on the scene and world map.",
            section = pathfinding,
            position = 6
    )
    default boolean showGlobalPathfinderOverlay() {
        return false;
    }

    // =========== Tests Section ================
    @ConfigSection(
            name = "Query Tests",
            description = "Settings for enabling specific API query tests.",
            position = 100
    )
    String tests = "Query Tests";

    @ConfigItem(
            keyName = "enableBankQuery",
            name = "Start Bank Tests",
            description = "Enable Bank Query Tests",
            section = tests,
            position = 2
    )
    default boolean enableBankQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableInventoryQuery",
            name = "Start Inventory Tests",
            description = "Enable Inventory Query Tests",
            section = tests,
            position = 3
    )
    default boolean enableInventoryQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableBankInventoryQuery",
            name = "Start Bank Inventory Tests",
            description = "Enable Bank inventory Query Tests",
            section = tests,
            position = 4
    )
    default boolean enableBankInventoryQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableEquipmentQuery",
            name = "Start Equipment Tests",
            description = "Enable Equipment Query Tests",
            section = tests,
            position = 5
    )
    default boolean enableEquipmentQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableGameObjectQuery",
            name = "Start Game Object Tests",
            description = "Enable game object query tests",
            section = tests,
            position = 6
    )
    default boolean enableGameObjectQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableGroundObjectQuery",
            name = "Start Ground Object Tests",
            description = "Enable Ground object query tests",
            section = tests,
            position = 6
    )
    default boolean enableGroundObjectQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableNpcQuery",
            name = "Start Npc Tests",
            description = "Enable Npc object query tests",
            section = tests,
            position = 7
    )
    default boolean enableNpcQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enablePlayerQuery",
            name = "Start Player Tests",
            description = "Enable Player object query tests",
            section = tests,
            position = 8
    )
    default boolean enablePlayerQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableWidgetQuery",
            name = "Start Widget Tests",
            description = "Enable Widget object query tests",
            section = tests,
            position = 9
    )
    default boolean enableWidgetQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableWorldQuery",
            name = "Start World Tests",
            description = "Enable World object query tests",
            section = tests,
            position = 10
    )
    default boolean enableWorldQuery() {
        return true;
    }


    @ConfigItem(
            keyName = "enableDepositBoxQuery",
            name = "Start Deposit Box Query Tests",
            description = "Enable Deposit Box query tests",
            section = tests,
            position = 11
    )
    default boolean enableDepositBoxQuery() {
        return true;
    }


    // ==============================================
    // ========== SERVICE TEST SETTINGS ==========
    // ==============================================
    @ConfigSection(
            name = "Service Tests",
            description = "Options for configuring service class tests",
            position = 101
    )
    String serviceTests = "Service Tests";

    @ConfigItem(
            keyName = "enablePrayer",
            name = "Start Prayer Service Tests",
            description = "Enable Prayer tests",
            section = serviceTests,
            position = 1
    )
    default boolean enablePrayerTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enableMovement",
            name = "Start Movement Service Tests",
            description = "Enable movement service tests",
            section = serviceTests,
            position = 2
    )
    default boolean enableMovementTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enableSpell",
            name = "Start Spell Service Tests",
            description = "Enable spell service tests",
            section = serviceTests,
            position = 3
    )
    default boolean enableSpellTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enableCamera",
            name = "Start Camera Service Tests",
            description = "Enable camera service tests",
            section = serviceTests,
            position = 4
    )
    default boolean enableCameraTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enableTaskChain",
            name = "Start Task Chain Tests",
            description = "Enable task chain tests",
            section = serviceTests,
            position = 6
    )
    default boolean enableTaskChain() {
        return true;
    }

    @ConfigItem(
            keyName = "enableDialogueService",
            name = "Start Dialogue Service Tests",
            description = "Enable dialogue service tests",
            section = serviceTests,
            position = 7
    )
    default boolean enableDialogueService() {
        return true;
    }

    @ConfigItem(
            keyName = "enableProcessingService",
            name = "Start Process Service Tests",
            description = "Enable process service tests",
            section = serviceTests,
            position = 8
    )
    default boolean enableProcessService() {
        return true;
    }

    @ConfigItem(
            keyName = "enableAreaService",
            name = "Start Area Service Tests",
            description = "Enable area service tests",
            section = serviceTests,
            position = 9
    )
    default boolean enableAreaService() {
        return true;
    }

    @ConfigItem(
            keyName = "enableGrandExchangeService",
            name = "Start GE Tests",
            description = "Enable grand exchange tests",
            section = serviceTests,
            position = 10
    )
    default boolean enableGrandExchangeService() {
        return true;
    }

    @ConfigItem(
            keyName = "enableBankServiceTests",
            name = "Start Bank Service Tests",
            description = "Enable Bank service tests",
            section = serviceTests,
            position = 11
    )
    default boolean enableBankServiceTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enableDepositBoxService",
            name = "Start Deposit Box Service Tests",
            description = "Enable Deposit Box service tests",
            section = serviceTests,
            position = 12
    )
    default boolean enableDepositBoxService() {
        return true;
    }

    // ==============================================
    // ========== MOUSE SETTINGS ==========
    // ==============================================
    @ConfigSection(
            name = "Mouse Settings",
            description = "Settings for testing mouse movement, pathing, and recording.",
            position = 102
    )
    String mouseSettings = "Mouse Settings";

    @ConfigItem(
            keyName = "mouseRecord",
            name = "Start Recording",
            description = "Starts or stops the mouse recording. When checked mouse recording " +
                    "is happening. When un-checked mouse recording stops.",
            section = mouseSettings,
            position = 1
    )
    default boolean mouseRecord() {
        return true;
    }

    @ConfigItem(
            keyName = "mouseStrategy",
            name = "Mouse Movement Strategy",
            description = "Determines the strategy to use for moving the mouse.",
            section = mouseSettings,
            position = 2
    )
    default MouseMovementStrategy mouseStrategy() {
        return MouseMovementStrategy.NO_MOVEMENT;
    }

    @ConfigItem(
            keyName = "enableMouseTest",
            name = "Start Mouse Test",
            description = "Starts the mouse movement test, using the \"test\" recording data",
            section = mouseSettings,
            position = 3
    )
    default boolean enableMouseTest() {
        return true;
    }


    // ==============================================
    // ========== OVERLAY SETTINGS ==========
    // ==============================================
    @ConfigSection(
            name = "Overlay Settings",
            description = "General overlay configuration for tests, debugging, and sim visualization",
            position = 103
    )
    String overlaySettings = "Overlay Settings";

    @ConfigItem(
            keyName = "showGameObjects",
            name = "Show Game Objects",
            description = "Display game objects in the scene.",
            section = overlaySettings,
            position = 1
    )
    default boolean showGameObjects() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            keyName = "gameObjectRange",
            name = "Game Object Range",
            description = "The range at which game objects are highlighted.",
            section = overlaySettings,
            position = 2
    )
    default int gameObjectRange() {
        return 3;
    }

    @ConfigItem(
            keyName = "showGroundObjects",
            name = "Show Ground Objects",
            description = "Display Ground objects in the scene.",
            section = overlaySettings,
            position = 4
    )
    default boolean showGroundObjects() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            keyName = "groundObjectRange",
            name = "Ground Object Range",
            description = "The range at which ground objects are highlighted.",
            section = overlaySettings,
            position = 5
    )
    default int groundObjectRange() {
        return 3;
    }

    @ConfigItem(
            keyName = "showNpcObjects",
            name = "Show Npc Objects",
            description = "Display Npc's within the scene.",
            section = overlaySettings,
            position = 6
    )
    default boolean showNpcs() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            keyName = "npcRange",
            name = "Npc Range",
            description = "The range at which Npcs are highlighted.",
            section = overlaySettings,
            position = 7
    )
    default int npcRange() {
        return 3;
    }

    @ConfigItem(
            keyName = "showPlayerObjects",
            name = "Show Players",
            description = "Display Player's within the scene.",
            section = overlaySettings,
            position = 8
    )
    default boolean showPlayers() {
        return false;
    }

    @ConfigItem(
            keyName = "showLocalPlayer",
            name = "Show Local Player",
            description = "Display information about the local player in the scene.",
            section = overlaySettings,
            position = 9
    )
    default boolean showSelf() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            keyName = "playerRange",
            name = "Player Range",
            description = "The range at which Players are highlighted.",
            section = overlaySettings,
            position = 10
    )
    default int playerRange() {
        return 3;
    }

    @ConfigItem(
            keyName = "showaAreaService",
            name = "Show Game Areas",
            description = "Show game areas rendered from the Area service tests.",
            section = overlaySettings,
            position = 11
    )
    default boolean showAreaService() {
        return true;
    }

    @ConfigItem(
            name = "Show Mouse Overlay",
            keyName = "showMouse",
            description = "Shows an overlay of the mouse position and trail.",
            position = 12,
            section = overlaySettings
    )
    default boolean showMouse() {
        return false;
    }

    @ConfigItem(
            keyName = "showWidgetDebug",
            name = "Show Widget Debug",
            description = "Display additional debug information about widgets when hovered with the mouse.",
            section = overlaySettings,
            position = 14
    )
    default boolean showWidgetDebug() {
        return false;
    }

    @ConfigItem(
            keyName = "renderCurrentPath",
            name = "Show Current Path",
            description = "Displays the current path calculated by the local pathfinder.",
            section = overlaySettings,
            position = 15
    )
    default boolean renderCurrentPath() {
        return false;
    }

    // ==============================================
    // ========== LoS SETTINGS ==========
    // ==============================================
    @ConfigSection(
            name = "Line of Sight Settings",
            description = "General configurations for NPC line of sight mechanics.",
            position = 104
    )
    String los = "NPC LoS & Pathing";

    @ConfigItem(
            name = "Show NPC Line of Sight",
            keyName = "showLos",
            description = "Shows the line of sight for NPC's around the player",
            position = 1,
            section = los
    )
    default boolean showNpcLoS() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            name = "NPC LoS Scan Range",
            keyName = "npcLoSScanRange",
            description = "How far from the player to include NPCs for LoS rendering.",
            position = 2,
            section = los
    )
    default int npcLoSScanRange() {
        return 15;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            name = "NPC LoS Default Range",
            keyName = "npcLoSDefaultRange",
            description = "Fallback range used when a range can't be detected and no manual override exists.",
            position = 3,
            section = los
    )
    default int npcLoSDefaultRange() {
        return 1;
    }

    @ConfigItem(
            name = "Use Detected NPC Ranges",
            keyName = "npcLoSUseDetectedRanges",
            description = "Use composition-based detected NPC ranges when available.",
            position = 4,
            section = los
    )
    default boolean npcLoSUseDetectedRanges() {
        return true;
    }

    @ConfigItem(
            name = "Show LoS Range Editor",
            keyName = "showNpcLoSRangeEditor",
            description = "Shows a small table for manual per-NPC range overrides while LoS is enabled.",
            position = 5,
            section = los
    )
    default boolean showNpcLoSRangeEditor() {
        return false;
    }

    @ConfigItem(
            name = "Use Per-NPC LoS Colors",
            keyName = "npcLoSUsePerNpcColors",
            description = "Color each NPC's LoS tiles uniquely based on NPC id.",
            position = 6,
            section = los
    )
    default boolean npcLoSUsePerNpcColors() {
        return true;
    }

    @Alpha
    @ConfigItem(
            name = "NPC LoS Base Color",
            keyName = "npcLoSColor",
            description = "Base LoS color used when per-NPC colors are disabled.",
            position = 7,
            section = los
    )
    default Color npcLoSColor() {
        return new Color(0, 255, 255, 120);
    }

    @Range(max = 255)
    @ConfigItem(
            name = "NPC LoS Fill Alpha",
            keyName = "npcLoSFillAlpha",
            description = "Tile fill alpha for NPC LoS overlays.",
            position = 8,
            section = los
    )
    default int npcLoSFillAlpha() {
        return 40;
    }

    @Range(max = 255)
    @ConfigItem(
            name = "NPC LoS Border Alpha",
            keyName = "npcLoSBorderAlpha",
            description = "Tile border alpha for NPC LoS overlays.",
            position = 9,
            section = los
    )
    default int npcLoSBorderAlpha() {
        return 150;
    }

    @ConfigItem(
            name = "Show NPC Pathing",
            keyName = "showNpcPathing",
            description = "Shows predicted NPC movement pathing towards the player.",
            position = 10,
            section = los
    )
    default boolean showNpcPathing() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            name = "NPC Path Scan Range",
            keyName = "npcPathScanRange",
            description = "How far from the player to include NPCs for path rendering.",
            position = 11,
            section = los
    )
    default int npcPathScanRange() {
        return 15;
    }

    @ConfigItem(
            name = "Stop Path On LoS",
            keyName = "npcPathStopOnLos",
            description = "Stops each rendered NPC path at the first tile where line of sight to the player is gained.",
            position = 12,
            section = los
    )
    default boolean npcPathStopOnLos() {
        return true;
    }

    @ConfigItem(
            name = "Show Path Termination Tile",
            keyName = "npcPathShowTerminationTile",
            description = "Highlights the tile where the rendered path ends.",
            position = 13,
            section = los
    )
    default boolean npcPathShowTerminationTile() {
        return true;
    }

    @ConfigItem(
            name = "Use Per-NPC Path Colors",
            keyName = "npcPathUsePerNpcColors",
            description = "Color each NPC path uniquely based on NPC id.",
            position = 14,
            section = los
    )
    default boolean npcPathUsePerNpcColors() {
        return true;
    }

    @Alpha
    @ConfigItem(
            name = "NPC Path Base Color",
            keyName = "npcPathColor",
            description = "Base path color used when per-NPC path colors are disabled.",
            position = 15,
            section = los
    )
    default Color npcPathColor() {
        return new Color(255, 165, 0, 160);
    }

    @Range(max = 255)
    @ConfigItem(
            name = "NPC Path Fill Alpha",
            keyName = "npcPathFillAlpha",
            description = "Tile fill alpha for NPC path overlays.",
            position = 16,
            section = los
    )
    default int npcPathFillAlpha() {
        return 30;
    }

    @Range(max = 255)
    @ConfigItem(
            name = "NPC Path Border Alpha",
            keyName = "npcPathBorderAlpha",
            description = "Tile border alpha for NPC path overlays.",
            position = 17,
            section = los
    )
    default int npcPathBorderAlpha() {
        return 190;
    }
}
