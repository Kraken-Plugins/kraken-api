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
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Teleports using an item, whether it is worn or carried.
 *
 * <p>These transports click nothing in the scene. What identifies them is a list of interchangeable
 * item ids, such as every charge level of an Ardougne cloak, plus the stop to choose when the item
 * offers several. Two kinds appear. Jewellery names a stop, such as {@code Al Kharid} on an amulet
 * of glory: the stop is a sub-option behind Rub, and clicking Rub alone would land on a default
 * stop. Worn jewellery lists that same stop on the equipment widget, so the handler opens that tab
 * and clicks the live action instead of inventing one from the item definition. A tablet names no
 * stop: Break is the teleport, and there is no destination list to pick from.</p>
 *
 * <p>A definition says all of that in fields of its own: the item ids, the action on the item when
 * it is an unusual one, and the stop to choose when there is one.</p>
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
        TransportDefinition definition = context.getDefinition();
        if (definition == null) {
            return context.fail("the teleport carries no definition to say which item to use");
        }

        String destination = blankToNull(definition.getDisplayInfo());
        boolean namedStop = destination != null;
        String preferredMenu = blankToNull(definition.getMenuOption());

        ContainerItem item = revealItem(context);
        if (item == null || item.getWidget() == null) {
            log.debug("Holding none of the items for '{}'", definition.getId());
            return false;
        }

        WorldPoint before = context.playerLocation();
        if (!use(context.getCtx(), item, destination, namedStop, menusToTry(preferredMenu))) {
            return false;
        }

        awaitTeleport(context, before);
        return true;
    }

    /**
     * Orders the menu actions to try on a teleport item, with the one a definition names first.
     *
     * @param preferred the action a registered teleport names, or null when it names none
     * @return the actions to try, in order, without repeats
     */
    public static List<String> menusToTry(String preferred) {
        List<String> menus = new ArrayList<>(CANDIDATE_MENUS.length + 1);
        if (preferred != null && !preferred.trim().isEmpty()) {
            menus.add(preferred.trim());
        }
        for (String candidate : CANDIDATE_MENUS) {
            if (menus.stream().noneMatch(candidate::equalsIgnoreCase)) {
                menus.add(candidate);
            }
        }
        return menus;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
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
        Context ctx = context.getCtx();
        for (int id : candidateItemIds(context)) {
            Optional<EquipmentEntity> worn = ctx.equipment().inInterface().withId(id).first();
            if (worn.isPresent()) {
                return worn.get().raw();
            }

            Optional<InventoryEntity> carried = ctx.inventory().withId(id).first();
            if (carried.isPresent()) {
                return carried.get().raw();
            }
        }

        return null;
    }

    /**
     * Lists the item ids that carry the teleport, in the order they should be looked for.
     *
     * @param context the transport being operated
     * @return the candidate ids, empty when the transport names no item
     */
    public static List<Integer> candidateItemIds(TransportContext context) {
        TransportDefinition definition = context.getDefinition();
        return definition != null ? definition.getItemIds() : Collections.emptyList();
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
     * @param destination the stop to choose, or null when the teleport names none
     * @param namedStop true when a submenu or chat option must be chosen
     * @param menus the menu actions to try on the item, in order
     * @return true when the interaction was dispatched
     */
    private boolean use(Context ctx, ContainerItem item, String destination, boolean namedStop, List<String> menus) {
        Widget widget = item.getWidget();
        if (widget == null) {
            return false;
        }

        String[] live = widget.getActions();
        if (hasLiveAction(live, destination)) {
            return ctx.getInteractionManager().interact(widget, destination);
        }

        for (String menu : menus) {
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
