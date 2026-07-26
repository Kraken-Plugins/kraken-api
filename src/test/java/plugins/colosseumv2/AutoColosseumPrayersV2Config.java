package plugins.colosseumv2;

import net.runelite.client.config.*;

import java.awt.event.KeyEvent;

@ConfigGroup("autocoloprayersv2")
public interface AutoColosseumPrayersV2Config extends Config {

    @ConfigItem(
            keyName = "licenseKey",
            name = "License Key",
            description = "License key required to enable the plugin.",
            position = -5,
            secret = true
    )
    default String licenseKey() {
        return "";
    }

    // ===========================
    // Prayer Section
    // ===========================
    @ConfigSection(
            name = "Prayer",
            description = "Settings for praying in the Colosseum.",
            position = 0
    )
    String prayerSection = "prayerSection";

    /**
     * Enables or disables the full auto-prayer engine.
     *
     * @return {@code true} when automation is allowed.
     */
    @ConfigItem(
            keyName = "enabled",
            name = "Enable Defensive Prayers",
            description = "Enable automatic Colosseum defensive protection prayers.",
            section = prayerSection,
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
            section = prayerSection,
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
            name = "Toggle Auto Prayers Hotkey",
            description = "Press to toggle runtime auto-prayers.",
            section = prayerSection,
            position = 3
    )
    default ModifierlessKeybind toggleHotkey() {
        return new ModifierlessKeybind(KeyEvent.VK_F6, 0);
    }

    /**
     * Hotkey used to toggle one-tick flick mode at runtime.
     *
     * @return Configured one-tick flick toggle keybind.
     */
    @ConfigItem(
            keyName = "oneTickFlickHotkey",
            name = "Toggle 1-Tick Flick Hotkey",
            description = "Press to toggle one-tick flick mode on/off.",
            section = prayerSection,
            position = 4
    )
    default ModifierlessKeybind oneTickFlickHotkey() {
        return new ModifierlessKeybind(KeyEvent.VK_F7, 0);
    }

    /**
     * Enables wave start pre-prayer.
     *
     * @return {@code true} to allow generic wave pre-prayer.
     */
    @ConfigItem(
            keyName = "enableWavePrePray",
            name = "Enable Wave Pre-Pray",
            description = "Use a generic protection prayer for early wave-start attacks.",
            section = prayerSection,
            position = 5
    )
    default boolean enableWavePrePray() {
        return true;
    }

    /**
     * How long the generic wave pre-prayer is held after a wave starts, unless a real attack
     * is observed first.
     *
     * @return Pre-pray window length in ticks.
     */
    @Range(min = 1, max = 30)
    @ConfigItem(
            keyName = "prePrayWindowTicks",
            name = "Pre-Pray Window Ticks",
            description = "Ticks after wave start to hold the generic pre-prayer if no attack has been seen.",
            section = prayerSection,
            position = 6
    )
    default int prePrayWindowTicks() {
        return 12;
    }

    /**
     * Turns the plugin-activated protection prayer off when nothing is queued for the next
     * tick. Only prayers activated by the plugin are deactivated, never manually activated
     * ones. One-tick flick mode always deactivates when idle regardless of this setting.
     *
     * @return {@code true} to deactivate the plugin's prayer between predicted attacks.
     */
    @ConfigItem(
            keyName = "deactivateOnIdle",
            name = "Deactivate When Idle",
            description = "Turn the plugin's protection prayer off between predicted attacks to save prayer points.",
            section = prayerSection,
            position = 7
    )
    default boolean deactivateOnIdle() {
        return false;
    }

    // ===========================
    // QUEUE Config
    // ===========================
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


    // ===========================
    // Overlays Section
    // ===========================
    @ConfigSection(
            name = "Overlays",
            description = "Status and queue debugging overlays.",
            position = 30
    )
    String overlaySection = "overlaySection";

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
            name = "Descending Boxes",
            description = "Render descending prayer boxes above protection prayers.",
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

    // =======================
    // Debug Section
    // =======================
    @ConfigSection(
            name = "Debug",
            description = "Debug controls for detailed state and NPC visualization.",
            position = 40
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
}

