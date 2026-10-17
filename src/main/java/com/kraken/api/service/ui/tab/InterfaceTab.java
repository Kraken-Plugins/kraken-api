package com.kraken.api.service.ui.tab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;

/**
 * Tabs of the side panel, with the name of each tab, the varbit that holds its hotkey and its index.
 * The index is the value that {@link TabService#switchTo(InterfaceTab)} passes to client script 915 to open the tab.
 */
@AllArgsConstructor
public enum InterfaceTab {
    /** The combat options tab. */
    COMBAT("Combat Options", VarbitID.STONE_COMBAT_KEY, 0),
    /** The skills tab. */
    SKILLS("Skills", VarbitID.STONE_STATS_KEY, 1),
    /** The quest list tab. */
    QUESTS("Quest List", VarbitID.STONE_JOURNAL_KEY, 2),
    /** The inventory tab. */
    INVENTORY("Inventory", VarbitID.STONE_INV_KEY, 3),
    /** The worn equipment tab. */
    EQUIPMENT("Worn Equipment", VarbitID.STONE_WORN_KEY, 4),
    /** The prayer tab. */
    PRAYER("Prayer", VarbitID.STONE_PRAYER_KEY, 5),
    /** The magic spellbook tab. */
    MAGIC("Magic", VarbitID.STONE_MAGIC_KEY, 6),
    /** The friends list tab. */
    FRIENDS("Friends List", VarbitID.STONE_FRIENDS_KEY, 9),
    /** The settings tab. */
    SETTINGS("Settings", VarbitID.STONE_OPTIONS1_KEY, 11),
    /** The music player tab. */
    MUSIC("Music Player", VarbitID.STONE_MUSIC_KEY, 13),
    /** The logout tab. */
    LOGOUT("Logout", VarbitID.STONE_LOGOUT_KEY, 10),
    /** The chat channel tab. */
    CHAT("Chat Channel", VarbitID.STONE_CLANCHAT_KEY, 7),
    /** The account management tab. */
    ACC_MAN("Account Management", VarbitID.STONE_ACCOUNT_KEY, 8),
    /** The emotes tab. */
    EMOTES("Emotes", VarbitID.STONE_OPTIONS2_KEY, 12),
    /** No side panel tab is open, or the tab value is not recognized. */
    NOTHING_SELECTED("NothingSelected", -1, -1);

    @Getter
    private final String name;

    @Getter
    private final int hotkeyVarbit;

    @Getter
    private final int index;
}
