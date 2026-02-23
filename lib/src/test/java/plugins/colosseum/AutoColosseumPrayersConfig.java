package plugins.colosseum;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.ModifierlessKeybind;
import net.runelite.client.config.Range;

import java.awt.event.KeyEvent;

@ConfigGroup("autocoloprayers")
public interface AutoColosseumPrayersConfig extends Config {
    @ConfigSection(
            name = "General",
            description = "Main automation and hotkey settings.",
            position = 0
    )
    String generalSection = "generalSection";

    /**
     * Enables or disables the full auto-prayer engine.
     *
     * @return {@code true} when automation is allowed.
     */
    @ConfigItem(
            keyName = "enabled",
            name = "Enable Plugin",
            description = "Enable automatic Colosseum protection prayers.",
            section = generalSection,
            position = 1
    )
    default boolean enabled() {
        return true;
    }

    /**
     * Sets the initial runtime state controlled by the hotkey toggle.
     *
     * @return {@code true} to start with runtime auto-prayers enabled.
     */
    @ConfigItem(
            keyName = "startEnabled",
            name = "Start Enabled",
            description = "Initial hotkey state when the plugin starts.",
            section = generalSection,
            position = 2
    )
    default boolean startEnabled() {
        return true;
    }

    /**
     * Hotkey used to toggle runtime auto-prayers on or off.
     *
     * @return Configured toggle keybind.
     */
    @ConfigItem(
            keyName = "toggleHotkey",
            name = "Toggle Hotkey",
            description = "Press to toggle runtime auto-prayers.",
            section = generalSection,
            position = 3
    )
    default ModifierlessKeybind toggleHotkey() {
        return new ModifierlessKeybind(KeyEvent.VK_F6, 0);
    }

    @ConfigSection(
            name = "Queue Logic",
            description = "Attack queue prediction and cancellation behavior.",
            position = 10
    )
    String queueSection = "queueSection";

    /**
     * Controls how far ahead future attacks are queued.
     *
     * @return Prayer queue horizon in ticks.
     */
    @Range(min = 1, max = 50)
    @ConfigItem(
            keyName = "queueLookaheadTicks",
            name = "Queue Lookahead Ticks",
            description = "How many ticks into the future to queue predicted attacks.",
            section = queueSection,
            position = 11
    )
    default int queueLookaheadTicks() {
        return 20;
    }

    /**
     * Cancels queued future attacks for a mob after line of sight is broken.
     *
     * @return {@code true} to clear queued attacks on LOS break.
     */
    @ConfigItem(
            keyName = "cancelQueuedOnLosBreak",
            name = "Cancel On LOS Break",
            description = "Clear queued attacks when line of sight is broken from a mob.",
            section = queueSection,
            position = 12
    )
    default boolean cancelQueuedOnLosBreak() {
        return true;
    }

    /**
     * Removes queued future attacks when a mob dies or despawns.
     *
     * @return {@code true} to clear queue entries on death.
     */
    @ConfigItem(
            keyName = "removeQueuedOnNpcDeath",
            name = "Remove Queue On Death",
            description = "Remove queued attacks for a mob when it dies.",
            section = queueSection,
            position = 13
    )
    default boolean removeQueuedOnNpcDeath() {
        return true;
    }

    /**
     * Enables automatic first manticore volley only when LOS remains unbroken while charging.
     *
     * @return {@code true} to auto-pray first manticore volley on clean charge.
     */
    @ConfigItem(
            keyName = "autoFirstManticoreVolley",
            name = "Auto First Manticore Volley",
            description = "Auto-pray manticore first volley only when LOS remains unbroken while charging.",
            section = queueSection,
            position = 14
    )
    default boolean autoFirstManticoreVolley() {
        return true;
    }

    /**
     * Number of ticks manticores spend charging before they start a volley.
     *
     * @return Manticore charge duration in ticks.
     */
    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "manticoreChargeTicks",
            name = "Manticore Charge Ticks",
            description = "Ticks from charge start to first manticore volley.",
            section = queueSection,
            position = 15
    )
    default int manticoreChargeTicks() {
        return 10;
    }

    /**
     * Gives jaguar melee attacks explicit priority when the pathing indicates an imminent hit.
     *
     * @return {@code true} to prioritize jaguar melee attack ticks.
     */
    @ConfigItem(
            keyName = "prayJaguarOnPath",
            name = "Jaguar Path Priority",
            description = "Pray melee on the tick a jaguar path predicts an imminent hit.",
            section = queueSection,
            position = 16
    )
    default boolean prayJaguarOnPath() {
        return true;
    }

    @ConfigSection(
            name = "Wave Pre-Pray",
            description = "Generic wave-start prayer to catch immediate spawn attacks.",
            position = 20
    )
    String prePraySection = "prePraySection";

    /**
     * Enables wave start pre-prayer.
     *
     * @return {@code true} to allow generic wave pre-prayer.
     */
    @ConfigItem(
            keyName = "enableWavePrePray",
            name = "Enable Wave Pre-Pray",
            description = "Use a generic protection prayer for early wave-start attacks.",
            section = prePraySection,
            position = 21
    )
    default boolean enableWavePrePray() {
        return true;
    }

    /**
     * Number of ticks to hold wave pre-prayer after a wave start is detected.
     *
     * @return Pre-prayer duration in ticks.
     */
    @Range(min = 1, max = 10)
    @ConfigItem(
            keyName = "prePrayDurationTicks",
            name = "Pre-Pray Duration",
            description = "How many ticks to hold the wave-start pre-prayer.",
            section = prePraySection,
            position = 22
    )
    default int prePrayDurationTicks() {
        return 3;
    }

    @ConfigSection(
            name = "Overlays",
            description = "Status and queue debugging overlays.",
            position = 40
    )
    String overlaySection = "overlaySection";

    @ConfigSection(
            name = "Debug",
            description = "Debug controls for detailed state and NPC visualization.",
            position = 30
    )
    String debugSection = "debugSection";

    /**
     * Expands the status overlay with detailed Colosseum state diagnostics.
     *
     * @return {@code true} to add detailed debug rows to the status panel.
     */
    @ConfigItem(
            keyName = "expandDebugOverlay",
            name = "Expand Status Debug",
            description = "Adds detailed wave/state/tick information to the status overlay.",
            section = debugSection,
            position = 31
    )
    default boolean expandDebugOverlay() {
        return false;
    }

    /**
     * Renders current line-of-sight tiles for tracked Colosseum NPCs.
     *
     * @return {@code true} to render NPC LoS debug tiles in scene.
     */
    @ConfigItem(
            keyName = "showNpcLineOfSightDebug",
            name = "Show NPC LoS Debug",
            description = "Render line-of-sight tiles for Colosseum NPCs in scene.",
            section = debugSection,
            position = 32
    )
    default boolean showNpcLineOfSightDebug() {
        return false;
    }

    /**
     * Renders pathing predictions from NPCs toward the local player.
     *
     * @return {@code true} to render NPC pathing debug tiles in scene.
     */
    @ConfigItem(
            keyName = "showNpcPathingDebug",
            name = "Show NPC Path Debug",
            description = "Render pathing predictions from Colosseum NPCs to the local player.",
            section = debugSection,
            position = 33
    )
    default boolean showNpcPathingDebug() {
        return false;
    }

    /**
     * Maximum number of NPCs visualized by scene debug overlays.
     *
     * @return Max debug-rendered NPC count.
     */
    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "npcDebugMaxNpcs",
            name = "NPC Debug Count",
            description = "Maximum number of Colosseum NPCs shown by LoS/path debug overlays.",
            section = debugSection,
            position = 34
    )
    default int npcDebugMaxNpcs() {
        return 8;
    }

    /**
     * Maximum path length rendered for NPC path debug.
     *
     * @return Max path tiles rendered for each NPC.
     */
    @Range(min = 1, max = 40)
    @ConfigItem(
            keyName = "npcDebugPathLength",
            name = "NPC Path Length",
            description = "Maximum path length shown for each NPC in path debug mode.",
            section = debugSection,
            position = 35
    )
    default int npcDebugPathLength() {
        return 12;
    }

    /**
     * Uses line-of-sight termination when rendering NPC path debug.
     *
     * @return {@code true} to stop path debug at first LoS tile.
     */
    @ConfigItem(
            keyName = "npcPathStopOnLosDebug",
            name = "Path Stop On LoS",
            description = "For path debug, stop rendering where NPC first gains line of sight.",
            section = debugSection,
            position = 36
    )
    default boolean npcPathStopOnLosDebug() {
        return true;
    }

    /**
     * Renders NPC label text with debug metadata.
     *
     * @return {@code true} to draw NPC debug labels.
     */
    @ConfigItem(
            keyName = "showNpcDebugLabels",
            name = "Show NPC Labels",
            description = "Render labels with NPC debug metadata.",
            section = debugSection,
            position = 37
    )
    default boolean showNpcDebugLabels() {
        return false;
    }

    /**
     * Shows the status panel overlay.
     *
     * @return {@code true} to display status overlay.
     */
    @ConfigItem(
            keyName = "showStatusOverlay",
            name = "Show Status Overlay",
            description = "Display plugin runtime state and next queued prayers.",
            section = overlaySection,
            position = 41
    )
    default boolean showStatusOverlay() {
        return true;
    }

    /**
     * Shows queue rows in the status overlay.
     *
     * @return {@code true} to include queue rows in the panel.
     */
    @ConfigItem(
            keyName = "showQueueOverlay",
            name = "Show Queue In Panel",
            description = "Display queue rows in the status panel overlay.",
            section = overlaySection,
            position = 42
    )
    default boolean showQueueOverlay() {
        return true;
    }

    /**
     * Shows descending queue boxes on prayer widgets.
     *
     * @return {@code true} to render prayer-tab queue boxes.
     */
    @ConfigItem(
            keyName = "showPrayerQueueOnPrayerTab",
            name = "Show Prayer Tab Queue",
            description = "Render descending queue boxes above protection prayers.",
            section = overlaySection,
            position = 43
    )
    default boolean showPrayerQueueOnPrayerTab() {
        return true;
    }

    /**
     * Maximum queue rows rendered in the panel.
     *
     * @return Max queue rows in status panel.
     */
    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "queueOverlayLines",
            name = "Panel Queue Rows",
            description = "Maximum number of queued entries shown in the status panel.",
            section = overlaySection,
            position = 44
    )
    default int queueOverlayLines() {
        return 8;
    }

    /**
     * Maximum tick offset rendered on prayer widgets.
     *
     * @return Tick lookahead for prayer-tab queue boxes.
     */
    @Range(min = 1, max = 20)
    @ConfigItem(
            keyName = "prayerTabLookaheadTicks",
            name = "Prayer Tab Lookahead",
            description = "Max tick offset shown by descending prayer-tab queue boxes.",
            section = overlaySection,
            position = 45
    )
    default int prayerTabLookaheadTicks() {
        return 8;
    }

}
