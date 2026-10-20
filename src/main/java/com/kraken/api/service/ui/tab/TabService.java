package com.kraken.api.service.ui.tab;

import com.kraken.api.Context;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Reads the side panel tab that is open and opens tabs. */
@Slf4j
@Singleton
public class TabService {

    @Inject
    private Context ctx;

    /**
     * Returns the side panel tab that is currently open, read from the inventory tab varc int.
     *
     * @return the open tab, or {@link InterfaceTab#NOTHING_SELECTED} when no tab is open or the varc value is not recognized
     */
    public InterfaceTab getCurrentTab() {
        final int varcIntValue = ctx.getClient().getVarcIntValue(171); // Inventory tab varc int
        // valueOf returns null for an unmapped varc value; switching on a null enum would NPE before
        // reaching the default branch, so guard it explicitly.
        VarcIntValues tab = VarcIntValues.valueOf(varcIntValue);
        if (tab == null) {
            return InterfaceTab.NOTHING_SELECTED;
        }
        switch (tab) {
            case TAB_COMBAT_OPTIONS:
                return InterfaceTab.COMBAT;
            case TAB_SKILLS:
                return InterfaceTab.SKILLS;
            case TAB_QUEST_LIST:
                return InterfaceTab.QUESTS;
            case TAB_INVENTORY:
                return InterfaceTab.INVENTORY;
            case TAB_WORN_EQUIPMENT:
                return InterfaceTab.EQUIPMENT;
            case TAB_PRAYER:
                return InterfaceTab.PRAYER;
            case TAB_SPELLBOOK:
                return InterfaceTab.MAGIC;
            case TAB_FRIEND_LIST:
                return InterfaceTab.FRIENDS;
            case TAB_LOGOUT:
                return InterfaceTab.LOGOUT;
            case TAB_SETTINGS:
                return InterfaceTab.SETTINGS;
            case TAB_MUSIC:
                return InterfaceTab.MUSIC;
            case TAB_CHAT_CHANNEL:
                return InterfaceTab.CHAT;
            case TAB_ACC_MANAGEMENT:
                return InterfaceTab.ACC_MAN;
            case TAB_EMOTES:
                return InterfaceTab.EMOTES;
            case TAB_NOT_SELECTED:
                return InterfaceTab.NOTHING_SELECTED;
            default:
                throw new IllegalStateException("Unexpected value: " + tab);
        }
    }


    /**
     * Opens the given side panel tab and waits for it to become the current tab. Returns immediately with {@code true} if the tab is already open.
     * Returns {@code false} without switching when the tab is {@link InterfaceTab#NOTHING_SELECTED} and the resizable stone arrangement varbit is 0.
     *
     * @param tab the tab to open
     * @return {@code true} if the tab is the current tab when the wait ends, {@code false} otherwise
     */
    public boolean switchTo(InterfaceTab tab) {
        if (isCurrentTab(tab)) return true;

        if (tab == InterfaceTab.NOTHING_SELECTED && ctx.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 0)
            return false;

        ctx.getClientThread().invokeLater(() -> ctx.getClient().runScript(915, tab.getIndex()));
        return SleepService.sleepUntil(() -> isCurrentTab(tab));
    }

    /**
     * Returns whether the given tab is the side panel tab that is currently open.
     *
     * @param tab the tab to compare with the open tab
     * @return {@code true} if the open tab is {@code tab}
     */
    public boolean isCurrentTab(InterfaceTab tab) {
        return getCurrentTab() == tab;
    }
}
