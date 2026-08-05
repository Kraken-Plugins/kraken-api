
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
            name = "Bank PIN",
            keyName = "bankPin",
            description = "Your four digit bank pin, used to get past the keypad during automated setup. <br>" +
                    "Leave blank if you have no pin, or if you prefer to enter it yourself before starting a run. <br>" +
                    "Without this, every test that needs the bank is skipped rather than hanging on the keypad.",
            position = -997,
            section = general,
            secret = true
    )
    default String bankPin() {
        return "";
    }

    @ConfigItem(
            name = "Establish Requirements",
            keyName = "establishPreconditions",
            description = "Let the runner set up the world before each test: travel, bank, withdraw. <br>" +
                    "Turn this off to run a test exactly as it stands, which is the quickest way to tell <br>" +
                    "whether a failure is a real regression or the setup putting the world in the wrong state.",
            position = -996,
            section = general
    )
    default boolean establishPreconditions() {
        return true;
    }

    @ConfigItem(
            name = "Include Destructive Tests",
            keyName = "includeDestructive",
            description = "Destructive tests are tests which have side effects mutating the world. i.e." +
                    "G.E. Test is destructive because it costs gold and fire runes to execute. <br>" +
                    "Movement tests are considered non-destructive because it costs nothing to move to a tile.",
            position = -995,
            section = general
    )
    default boolean includeDestructive() {
        return true;
    }

    @Units(Units.SECONDS)
    @ConfigItem(
            name = "Test Timeout",
            keyName = "testTimeout",
            description = "The timeout in seconds for a test. If a test exceeds this value <br>" +
                    "it will be cancelled and considered failed.",
            position = -994,
            section = general
    )
    default int timeout() {
        return 50;
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
