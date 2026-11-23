package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.Context;
import com.kraken.api.service.dialogue.DialogueService;
import com.kraken.api.service.ui.tab.InterfaceTab;
import com.kraken.api.service.ui.tab.TabService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.HomeTeleportPlan;
import com.kraken.api.service.walker.transport.GroupingTeleport;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportHandler;
import com.kraken.api.service.walker.transport.WidgetClicks;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Teleports to a minigame from the grouping tab.
 *
 * <p>The grouping tab is the fourth sub tab of the chat channel side panel. Its teleport button
 * goes to whichever activity is selected in the list above it, and the game tells the activities
 * apart by an id. The handler looks that id up by name in the game's own table of minigame
 * teleports, opens the sub tab, selects the activity the way picking it from the list does, and
 * clicks the button. The button is one child per activity id, so the click itself carries the
 * activity and cannot land on whatever was selected before.</p>
 *
 * <p>An activity with several landings, the rat pits, asks which one in a chat menu after the
 * click. The transport's display info names it, and it is answered while waiting for the teleport
 * to land. The teleport channels for about as long as a home teleport does.</p>
 *
 * <p>A minigame teleport names its activity in its display info, which is all this works from. The
 * twenty minute cooldown and what each activity asks of the player are requirements of the
 * transport, checked before this runs.</p>
 */
@Slf4j
public class GroupingTeleportHandler implements TransportHandler {

    /** The client script the activity list runs when an entry is picked. Its argument is the activity id. */
    private static final int SELECT_ACTIVITY_SCRIPT = 124;

    /** The sub tab's menu action, which is the name the chat channel side panel gives the tab. */
    private static final String GROUPING_TAB_ACTION = "Grouping";

    /** The layers that hold the sub tab buttons. Group ironman accounts get the larger panel. */
    private static final int[] SUB_TAB_LAYERS = {
            InterfaceID.SideChannels.TABS,
            InterfaceID.SideChannelsLarge.TABS
    };

    /** How long to wait for the side panel to show its sub tabs, and then for the grouping tab to show. */
    private static final long TAB_TIMEOUT_MS = 4_000;

    /** How long to wait for the teleport button to name the selected activity. */
    private static final long BUTTON_TIMEOUT_MS = 3_000;

    /** How far the player must move for the teleport to count as having happened. */
    private static final int TELEPORT_DISTANCE = 10;

    @Override
    public boolean execute(TransportContext context) {
        GroupingTeleport teleport = GroupingTeleport.parse(context.getDisplayInfo());
        if (teleport == null) {
            return context.fail("the minigame teleport names no activity");
        }

        Context ctx = context.getCtx();
        int activityId = GroupingTeleport.activityId(readActivities(ctx), teleport.getActivity());
        if (activityId == GroupingTeleport.NO_ACTIVITY) {
            return context.fail("the game lists no minigame teleport called " + teleport.getActivity());
        }

        if (!openGroupingTab(ctx)) {
            return context.fail("the grouping tab did not open");
        }

        WorldPoint before = context.playerLocation();
        if (!clickTeleport(ctx, activityId)) {
            return context.fail("the grouping tab offered no teleport to " + teleport.getActivity());
        }

        String problem = awaitTeleport(context, teleport, before);
        return problem == null || context.fail(problem);
    }

    /**
     * Reads the activities that have a teleport from the game's table of them.
     *
     * @param ctx the API context
     * @return the activity names by id, empty when the table could not be read
     */
    private static Map<Integer, String> readActivities(Context ctx) {
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            Map<Integer, String> activities = new LinkedHashMap<>();
            for (int row : client.getDBTableRows(DBTableID.MinigameTeleport.ID)) {
                Object id = firstValue(client.getDBTableField(row, DBTableID.MinigameTeleport.COL_ID, 0));
                Object name = firstValue(client.getDBTableField(row, DBTableID.MinigameTeleport.COL_NAME, 0));
                if (id instanceof Integer && name instanceof String) {
                    activities.put((Integer) id, (String) name);
                }
            }
            return activities;
        }, Collections.emptyMap());
    }

    private static Object firstValue(Object[] field) {
        return field != null && field.length > 0 ? field[0] : null;
    }

    /**
     * Opens the chat channel side panel and its grouping sub tab.
     *
     * <p>The side panel builds its sub tab buttons a moment after it becomes the open panel, so
     * the button is waited for and not read once.</p>
     *
     * @param ctx the API context
     * @return true when the grouping tab is showing
     */
    private boolean openGroupingTab(Context ctx) {
        TabService tabs = ctx.getService(TabService.class);
        if (tabs == null || !tabs.switchTo(InterfaceTab.CHAT)) {
            return false;
        }

        Widget[] subTab = new Widget[1];
        boolean ready = SleepService.sleepUntil(() -> {
            if (isGroupingTabShowing(ctx)) {
                return true;
            }
            subTab[0] = ctx.runOnClientThread(() -> groupingSubTab(ctx.getClient()), null);
            return subTab[0] != null;
        }, TAB_TIMEOUT_MS);
        if (!ready) {
            return false;
        }
        if (isGroupingTabShowing(ctx)) {
            return true;
        }

        return ctx.getInteractionManager().interact(subTab[0], GROUPING_TAB_ACTION)
                && SleepService.sleepUntil(() -> isGroupingTabShowing(ctx), TAB_TIMEOUT_MS);
    }

    private static boolean isGroupingTabShowing(Context ctx) {
        return Boolean.TRUE.equals(ctx.runOnClientThread(() -> {
            Widget teleport = ctx.getClient().getWidget(InterfaceID.Grouping.TELEPORT);
            return teleport != null && !teleport.isHidden();
        }, Boolean.FALSE));
    }

    /**
     * Finds the button of the grouping sub tab by the action it offers. Client thread only.
     *
     * @param client the game client
     * @return the button, or null when the chat channel side panel shows no such sub tab
     */
    private static Widget groupingSubTab(Client client) {
        for (int layerId : SUB_TAB_LAYERS) {
            Widget layer = client.getWidget(layerId);
            if (layer == null || layer.isHidden()) {
                continue;
            }

            for (Widget subTab : layer.getStaticChildren()) {
                if (subTab != null && ItemTeleportHandler.hasLiveAction(subTab.getActions(), GROUPING_TAB_ACTION)) {
                    return subTab;
                }
            }
        }
        return null;
    }

    /**
     * Selects an activity and clicks the teleport button once it names one.
     *
     * @param ctx the API context
     * @param activityId the id of the activity to teleport to
     * @return true when the click was dispatched
     */
    private boolean clickTeleport(Context ctx, int activityId) {
        ctx.runOnClientThread(() -> {
            ctx.getClient().runScript(SELECT_ACTIVITY_SCRIPT, activityId);
            return Boolean.TRUE;
        }, Boolean.FALSE);

        Widget[] found = new Widget[1];
        boolean offered = SleepService.sleepUntil(() -> {
            found[0] = ctx.runOnClientThread(() -> teleportButton(ctx.getClient(), activityId), null);
            return found[0] != null;
        }, BUTTON_TIMEOUT_MS);
        if (!offered) {
            return false;
        }

        String action = GroupingTeleport.teleportAction(found[0].getActions());
        return action != null && ctx.getInteractionManager().interact(found[0], action);
    }

    /**
     * Finds the teleport button for an activity. Client thread only.
     *
     * <p>The grouping tab rebuilds the button every time the selection changes: it is the child of
     * the teleport layer whose index is the id of the selected activity, and the only child that
     * offers an action. Asking for the child at the wanted id therefore finds nothing until that
     * activity is the selected one.</p>
     *
     * @param client the game client
     * @param activityId the id of the activity to teleport to
     * @return the button, or null when the grouping tab is closed or another activity is selected
     */
    private static Widget teleportButton(Client client, int activityId) {
        Widget layer = client.getWidget(InterfaceID.Grouping.TELEPORT);
        if (layer == null || layer.isHidden()) {
            return null;
        }

        Widget button = WidgetClicks.childAt(layer, activityId);
        return button != null && GroupingTeleport.teleportAction(button.getActions()) != null ? button : null;
    }

    /**
     * Waits for the teleport to land, answering the chat menu an activity with several landings
     * opens first.
     *
     * @param context the teleport being operated
     * @param teleport the parsed display info, which carries the landing to choose
     * @param before where the player stood before the click, may be null
     * @return null when the teleport landed, otherwise why it is taken not to have
     */
    private String awaitTeleport(TransportContext context, GroupingTeleport teleport, WorldPoint before) {
        if (before == null) {
            SleepService.sleepFor(3);
            return null;
        }

        DialogueService dialogue = context.getCtx().getService(DialogueService.class);
        boolean[] chosen = new boolean[1];
        boolean landed = SleepService.sleepUntil(() -> {
            if (teleport.getChoice() != null && !chosen[0]) {
                String option = HubDialogueHandler.chooseOption(dialogue.getDialogueOptions(), teleport.getChoice());
                chosen[0] = option != null && dialogue.selectOption(option);
            }

            WorldPoint now = context.playerLocation();
            return now != null && now.distanceTo(before) > TELEPORT_DISTANCE;
        }, HomeTeleportPlan.CAST_TIMEOUT_MS);

        if (landed) {
            return null;
        }
        return teleport.getChoice() != null && !chosen[0]
                ? "the teleport to " + teleport.getActivity() + " never offered " + teleport.getChoice().getLabel()
                : "clicked the teleport to " + teleport.getActivity() + " but it did not land";
    }
}
