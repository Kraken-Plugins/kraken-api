package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.Context;
import com.kraken.api.core.interaction.resolver.ActionResolver;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.service.dialogue.DialogueService;
import com.kraken.api.service.ui.tab.InterfaceTab;
import com.kraken.api.service.ui.tab.TabService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import shortestpath.transport.requirement.ItemRequirement;
import shortestpath.transport.requirement.TransportItems;

/**
 * Teleports using an item, whether it is worn or carried.
 *
 * <p>These transports carry no object info at all. What identifies them is the item requirement — a
 * list of interchangeable item ids such as every charge level of an Ardougne cloak — plus display info
 * naming the destination. Two shapes appear. Jewellery is {@code "Amulet of glory: Al Kharid"}: the
 * destination is a sub-option behind Rub, and clicking Rub alone would land on a default stop. Worn
 * jewellery lists that same stop on the equipment widget, so the handler opens that tab and clicks
 * the live action instead of inventing one from the item definition.
 * Tablets are a bare name such as {@code "Lumbridge tablet"}: Break is the teleport, and there is no
 * destination list to pick from.</p>
 */
@Slf4j
public class ItemTeleportHandler implements TransportHandler {

    /** Menu actions that commonly hide a destination list behind them. */
    private static final String[] CANDIDATE_MENUS = {"Rub", "Teleport", "Operate", "Break", "Invoke", "Play"};

    /** How long to wait for the inventory or equipment tab to show the item's menu. */
    private static final long TAB_TIMEOUT_MS = 2_000;

    /** How long to wait for the teleport to land. */
    private static final long TELEPORT_TIMEOUT_MS = 10_000;

    /** How far the player must move for a teleport to count as having happened. */
    private static final int TELEPORT_DISTANCE = 10;

    @Override
    public boolean execute(TransportContext context) {
        String displayInfo = context.getDisplayInfo();
        String destination = destinationLabel(displayInfo);
        if (destination == null) {
            log.debug("Teleport item transport has no usable display info: {}", displayInfo);
            return false;
        }

        ContainerItem item = revealItem(context);
        if (item == null || item.getWidget() == null) {
            log.debug("Holding none of the items for '{}'", displayInfo);
            return false;
        }

        WorldPoint before = context.playerLocation();
        if (!use(context.getCtx(), item, destination, hasSubDestination(displayInfo))) {
            return false;
        }

        awaitTeleport(context, before);
        return true;
    }

    /**
     * Splits display info into the destination it names.
     *
     * <p>Display info reads {@code "<item>: <destination>"}. The item half is already known from the
     * requirement list, so only the destination is needed, and it is what the menu shows. A tablet row
     * has no colon, so the whole string is returned — it is not a menu option.</p>
     *
     * @param displayInfo the transport's display info
     * @return the destination text, or null when there is none
     */
    public static String destinationLabel(String displayInfo) {
        if (displayInfo == null || displayInfo.trim().isEmpty()) {
            return null;
        }

        String trimmed = displayInfo.trim();
        int separator = trimmed.indexOf(':');
        if (separator < 0) {
            return trimmed;
        }

        String destination = trimmed.substring(separator + 1).trim();
        return destination.isEmpty() ? null : destination;
    }

    /**
     * Reports whether display info names a stop that must be selected after the first click.
     *
     * <p>{@code "Amulet of glory: Al Kharid"} does; {@code "Lumbridge tablet"} does not. Treating the
     * tablet name as a sub-option made Break succeed and then fail the walk in Lumbridge.</p>
     *
     * @param displayInfo the transport's display info, may be null
     * @return true when a destination must be chosen from a submenu or chat list
     */
    public static boolean hasSubDestination(String displayInfo) {
        if (displayInfo == null) {
            return false;
        }

        String trimmed = displayInfo.trim();
        int separator = trimmed.indexOf(':');
        return separator > 0 && separator < trimmed.length() - 1
                && !trimmed.substring(separator + 1).trim().isEmpty();
    }

    /**
     * Finds the first of the transport's interchangeable items the player holds.
     *
     * <p>Worn items are looked up on the equipment interface only. The default equipment query also
     * returns wearable inventory copies, and those copies list worn destinations such as {@code Karamja}
     * that the inventory widget does not actually offer.</p>
     *
     * @param context the transport being operated
     * @return the first matching worn or carried item, or null when none is held
     */
    private ContainerItem findItem(TransportContext context) {
        TransportItems requirements = context.getTransport() != null
                ? context.getTransport().getItemRequirements() : null;
        if (requirements == null || requirements.getRequirements() == null) {
            return null;
        }

        Context ctx = context.getCtx();
        for (ItemRequirement requirement : requirements.getRequirements()) {
            int[] ids = requirement.getItemIds();
            if (ids == null) {
                continue;
            }

            for (int id : ids) {
                EquipmentEntity worn = ctx.equipment().inInterface().withId(id).first();
                if (worn != null && worn.isPresent()) {
                    return worn.raw();
                }

                InventoryEntity carried = ctx.inventory().withId(id).first();
                if (carried != null && carried.isPresent()) {
                    return carried.raw();
                }
            }
        }

        return null;
    }

    /**
     * Opens the tab that holds the teleport item and waits until its live menu is readable.
     *
     * <p>A worn glory's destinations are on the equipment widget, which has no actions until that tab
     * is selected. Equipment slots with a null action list used to be skipped entirely, so an equipped
     * glory was invisible while the inventory was open and the walk failed before any click.</p>
     *
     * @param context the transport being operated
     * @return the item once its widget lists actions, or null when it cannot be shown
     */
    private ContainerItem revealItem(TransportContext context) {
        ContainerItem item = findItem(context);
        if (item == null) {
            if (!switchTab(context.getCtx(), InterfaceTab.EQUIPMENT)) {
                return null;
            }

            item = waitForItemMenu(context);
            if (item != null) {
                return item;
            }

            if (!switchTab(context.getCtx(), InterfaceTab.INVENTORY)) {
                return null;
            }

            return waitForItemMenu(context);
        }

        InterfaceTab tab = item.getOrigin() == ContainerItem.ItemOrigin.EQUIPMENT
                ? InterfaceTab.EQUIPMENT
                : InterfaceTab.INVENTORY;
        if (!switchTab(context.getCtx(), tab)) {
            return null;
        }

        ContainerItem shown = waitForItemMenu(context);
        return shown != null ? shown : item;
    }

    private boolean switchTab(Context ctx, InterfaceTab tab) {
        TabService tabs = ctx.getService(TabService.class);
        return tabs != null && tabs.switchTo(tab);
    }

    private ContainerItem waitForItemMenu(TransportContext context) {
        final ContainerItem[] found = new ContainerItem[1];
        SleepService.sleepUntil(() -> {
            found[0] = findItem(context);
            Widget widget = found[0] != null ? found[0].getWidget() : null;
            return widget != null && widget.getActions() != null;
        }, TAB_TIMEOUT_MS);
        return found[0];
    }

    /**
     * Works the item's live widget menu.
     *
     * <p>Only the live widget menu is used. The item definition lists worn destinations such as
     * {@code Karamja} even when the amulet is in the inventory, whose widget only offers Wear / Rub.
     * Clicking {@code Karamja} as a top-level action is what failed before Rub could open the submenu.
     * A worn glory lists the stop on the equipment widget, so that click is used when it is really
     * there.</p>
     *
     * @param ctx the API context
     * @param item the item to use
     * @param destination the destination label, or the whole display info for a tablet
     * @param namedStop true when a submenu or chat option must be chosen
     * @return true when the interaction was dispatched
     */
    private boolean use(Context ctx, ContainerItem item, String destination, boolean namedStop) {
        Widget widget = item.getWidget();
        if (widget == null) {
            return false;
        }

        String[] live = widget.getActions();
        if (hasLiveAction(live, destination)) {
            return ctx.getInteractionManager().interact(widget, destination);
        }

        for (String menu : CANDIDATE_MENUS) {
            if (!hasLiveAction(live, menu)) {
                continue;
            }

            if (!namedStop) {
                return ctx.getInteractionManager().interact(widget, menu);
            }

            if (ctx.getInteractionManager().interact(widget, menu, destination)) {
                return true;
            }

            // Some items answer a menu action with a conversation rather than a sub-menu.
            if (ctx.getInteractionManager().interact(widget, menu)) {
                return chooseFromDialogue(ctx, destination);
            }
        }

        return false;
    }

    /**
     * Reports whether a live widget menu offers an action.
     *
     * @param actions the widget's current actions, may be null
     * @param wanted the action to look for, may be null
     * @return true when the widget lists that action
     */
    public static boolean hasLiveAction(String[] actions, String wanted) {
        if (actions == null || wanted == null || wanted.isEmpty()) {
            return false;
        }

        for (String candidate : actions) {
            if (ActionResolver.matches(wanted, candidate)) {
                return true;
            }
        }

        return false;
    }

    private boolean chooseFromDialogue(Context ctx, String destination) {
        DialogueService dialogue = ctx.getService(DialogueService.class);
        SleepService.sleepUntil(dialogue::isDialoguePresent, 3_000);

        if (dialogue.isOptionPresent(destination)) {
            return dialogue.selectOption(destination);
        }

        return false;
    }

    private void awaitTeleport(TransportContext context, WorldPoint before) {
        if (before == null) {
            SleepService.sleepFor(3);
            return;
        }

        SleepService.sleepUntil(() -> {
            WorldPoint now = context.playerLocation();
            return now != null && now.distanceTo(before) > TELEPORT_DISTANCE;
        }, TELEPORT_TIMEOUT_MS);
    }
}
