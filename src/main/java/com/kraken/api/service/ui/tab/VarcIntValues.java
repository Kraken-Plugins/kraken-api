package com.kraken.api.service.ui.tab;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/** Values of varc ints that hold UI state. {@link TabService} reads the {@code TAB_} values to find the open side panel tab. */
@Getter
public enum VarcIntValues {
    // Tooltip State
    /** Tooltip state value for a shown tooltip. */
    TOOLTIP_SHOWN(0),
    /** Tooltip state value for a hidden tooltip. */
    TOOLTIP_HIDDEN(1),
    // Chatbox Input Type
    /** Chatbox input type for the RuneLite chatbox panel. */
    RUNELITE_CHATBOX_PANEL(-3),
    /** Chatbox input type for a RuneLite input. */
    RUNELITE(-2),
    /** Chatbox input type when no input is open. */
    NONE(0),
    /** Chatbox input type for a private message. */
    PRIVATE_MESSAGE(6),
    /** Chatbox input type for a search. */
    SEARCH(11),
    // Chatbox Scrollbar Position
    /** Lowest chatbox scrollbar position. */
    CHATBOX_SCROLLBAR_POSITION_MIN(0),
    /** Highest chatbox scrollbar position. */
    CHATBOX_SCROLLBAR_POSITION_MAX(1302),
    // Report Window State
    /** Report window state value for an open report window. */
    REPORT_WINDOW_OPENED(1),
    /** Report window state value for a closed report window. */
    REPORT_WINDOW_CLOSED(0),
    // Selected Chat Tab
    /** Selected chat tab value for the All tab. */
    SELECTED_CHAT_TAB_ALL(0),
    /** Selected chat tab value for the Game tab. */
    SELECTED_CHAT_TAB_GAME(1),
    /** Selected chat tab value for the Public tab. */
    SELECTED_CHAT_TAB_PUBLIC(2),
    /** Selected chat tab value for the Private tab. */
    SELECTED_CHAT_TAB_PRIVATE(3),
    /** Selected chat tab value for the Channel tab. */
    SELECTED_CHAT_TAB_CHANNEL(4),
    /** Selected chat tab value for the Clan tab. */
    SELECTED_CHAT_TAB_CLAN(5),
    /** Selected chat tab value for the Trade tab. */
    SELECTED_CHAT_TAB_TRADE(6),
    /** Selected chat tab value when the chat frame is closed. */
    SELECTED_CHAT_TAB_FRAME_CLOSED(1337),
    // Highlighted Chat Tab
    /** Highlighted chat tab value when no chat tab is highlighted. */
    HIGHLIGHTED_CHAT_TAB_NONE(-1),
    /** Highlighted chat tab value for the All tab. */
    HIGHLIGHTED_CHAT_TAB_ALL(0),
    /** Highlighted chat tab value for the Game tab. */
    HIGHLIGHTED_CHAT_TAB_GAME(1),
    /** Highlighted chat tab value for the Public tab. */
    HIGHLIGHTED_CHAT_TAB_PUBLIC(2),
    /** Highlighted chat tab value for the Private tab. */
    HIGHLIGHTED_CHAT_TAB_PRIVATE(3),
    /** Highlighted chat tab value for the Channel tab. */
    HIGHLIGHTED_CHAT_TAB_CHANNEL(4),
    /** Highlighted chat tab value for the Clan tab. */
    HIGHLIGHTED_CHAT_TAB_CLAN(5),
    /** Highlighted chat tab value for the Trade tab. */
    HIGHLIGHTED_CHAT_TAB_TRADE(6),
    // Bank Scrollbar Position
    /** Lowest bank scrollbar position. */
    BANK_SCROLLBAR_POSITION_MIN(0),
    /** Highest bank scrollbar position for a bank of 400 items. */
    BANK_SCROLLBAR_POSITION_400_ITEMS_MAX(1511),
    // Report Ignore Player State
    /** Report ignore player state value for a checked box. */
    REPORT_IGNORE_PLAYER_CHECKED(1),
    /** Report ignore player state value for an unchecked box. */
    REPORT_IGNORE_PLAYER_UNCHECKED(0),
    // Bank Worn Items
    /** Bank worn items state value when worn items are shown. */
    BANK_WORN_ITEMS_SHOWN(1),
    /** Bank worn items state value when worn items are hidden. */
    BANK_WORN_ITEMS_HIDDEN(0),
    // Interface Tabs
    /** Varc value for the combat options tab, mapped to {@link InterfaceTab#COMBAT}. */
    TAB_COMBAT_OPTIONS(0),
    /** Varc value for the skills tab, mapped to {@link InterfaceTab#SKILLS}. */
    TAB_SKILLS(1),
    /** Varc value for the quest list tab, mapped to {@link InterfaceTab#QUESTS}. */
    TAB_QUEST_LIST(2),
    /** Varc value for the inventory tab, mapped to {@link InterfaceTab#INVENTORY}. */
    TAB_INVENTORY(3),
    /** Varc value for the worn equipment tab, mapped to {@link InterfaceTab#EQUIPMENT}. */
    TAB_WORN_EQUIPMENT(4),
    /** Varc value for the prayer tab, mapped to {@link InterfaceTab#PRAYER}. */
    TAB_PRAYER(5),
    /** Varc value for the spellbook tab, mapped to {@link InterfaceTab#MAGIC}. */
    TAB_SPELLBOOK(6),
    /** Varc value for the chat channel tab, mapped to {@link InterfaceTab#CHAT}. */
    TAB_CHAT_CHANNEL(7),
    /** Varc value for the account management tab, mapped to {@link InterfaceTab#ACC_MAN}. */
    TAB_ACC_MANAGEMENT(8),
    /** Varc value for the friends list tab, mapped to {@link InterfaceTab#FRIENDS}. */
    TAB_FRIEND_LIST(9),
    /** Varc value for the logout tab, mapped to {@link InterfaceTab#LOGOUT}. */
    TAB_LOGOUT(10),
    /** Varc value for the settings tab, mapped to {@link InterfaceTab#SETTINGS}. */
    TAB_SETTINGS(11),
    /** Varc value for the emotes tab, mapped to {@link InterfaceTab#EMOTES}. */
    TAB_EMOTES(12),
    /** Varc value for the music player tab, mapped to {@link InterfaceTab#MUSIC}. */
    TAB_MUSIC(13),
    /** Varc value when no tab is selected, mapped to {@link InterfaceTab#NOTHING_SELECTED}. */
    TAB_NOT_SELECTED(-1)
    ;

    // cache values on load
    private static final Map<Object, Object> hashMap = new HashMap<>();
    static {
        for (VarcIntValues varcEnum : VarcIntValues.values()) {
            hashMap.put(varcEnum.value, varcEnum);
        }
    }

    private final int value;

    VarcIntValues(int value) {
        this.value = value;
    }

    /**
     * Returns the constant with the given value, or {@code null} if no constant has that value.
     * When several constants share a value, the one declared last is returned.
     *
     * @param varcEnum the varc int value to look up
     * @return the matching constant, or {@code null} if none matches
     */
    public static VarcIntValues valueOf(int varcEnum) {
        return (VarcIntValues) hashMap.get(varcEnum);
    }

}

