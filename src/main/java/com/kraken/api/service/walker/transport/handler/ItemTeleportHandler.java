package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.Context;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.service.dialogue.DialogueService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import shortestpath.transport.requirement.ItemRequirement;
import shortestpath.transport.requirement.TransportItems;

import java.util.Locale;

/**
 * Teleports using an item, whether it is worn or carried.
 *
 * <p>These transports carry no object info at all. What identifies them is the item requirement — a
 * list of interchangeable item ids such as every charge level of an Ardougne cloak — plus display info
 * naming the destination, as in {@code "Ardougne cloak: Kandarin Monastery"}. The destination is
 * usually a sub-option behind a menu action like "Rub", and occasionally a top-level action on a worn
 * item, so both are tried.</p>
 */
@Slf4j
public class ItemTeleportHandler implements TransportHandler {

    /** Menu actions that commonly hide a destination list behind them. */
    private static final String[] CANDIDATE_MENUS = {"Rub", "Teleport", "Operate", "Break", "Invoke", "Play"};

    /** How long to wait for the teleport to land. */
    private static final long TELEPORT_TIMEOUT_MS = 10_000;

    /** How far the player must move for a teleport to count as having happened. */
    private static final int TELEPORT_DISTANCE = 10;

    @Override
    public boolean execute(TransportContext context) {
        String destination = destinationLabel(context.getDisplayInfo());
        if (destination == null) {
            log.debug("Teleport item transport has no usable display info: {}", context.getDisplayInfo());
            return false;
        }

        ContainerItem item = findItem(context);
        if (item == null) {
            log.debug("Holding none of the items for '{}'", context.getDisplayInfo());
            return false;
        }

        WorldPoint before = context.playerLocation();
        if (!use(context.getCtx(), item, destination)) {
            return false;
        }

        awaitTeleport(context, before);
        return true;
    }

    /**
     * Splits display info into the destination it names.
     *
     * <p>Display info reads {@code "<item>: <destination>"}. The item half is already known from the
     * requirement list, so only the destination is needed, and it is what the menu shows.</p>
     *
     * @param displayInfo the transport's display info
     * @return the destination text, or null when there is none
     */
    static String destinationLabel(String displayInfo) {
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
     * Finds the first of the transport's interchangeable items the player holds.
     *
     * <p>Equipment is searched before the inventory: a worn item's destinations are reachable without
     * taking it off, and jewellery is more often worn than carried.</p>
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
                EquipmentEntity worn = ctx.equipment().withId(id).first();
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
     * Works the item's menu, preferring a destination that is offered directly.
     */
    private boolean use(Context ctx, ContainerItem item, String destination) {
        Widget widget = item.getWidget();
        if (widget == null) {
            return false;
        }

        if (hasAction(item, destination)) {
            return ctx.getInteractionManager().interact(widget, destination);
        }

        for (String menu : CANDIDATE_MENUS) {
            if (!hasAction(item, menu)) {
                continue;
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

    private boolean chooseFromDialogue(Context ctx, String destination) {
        DialogueService dialogue = ctx.getService(DialogueService.class);
        SleepService.sleepUntil(dialogue::isDialoguePresent, 3_000);

        if (dialogue.isOptionPresent(destination)) {
            return dialogue.selectOption(destination);
        }

        return dialogue.isDialoguePresent() && dialogue.continueDialogue();
    }

    private boolean hasAction(ContainerItem item, String action) {
        String wanted = action.toLowerCase(Locale.ROOT);

        String[] inventoryActions = item.getInventoryActions();
        if (inventoryActions != null) {
            for (String candidate : inventoryActions) {
                if (candidate != null && candidate.toLowerCase(Locale.ROOT).equals(wanted)) {
                    return true;
                }
            }
        }

        if (item.getEquipmentActions() != null) {
            for (String candidate : item.getEquipmentActions()) {
                if (candidate != null && candidate.toLowerCase(Locale.ROOT).equals(wanted)) {
                    return true;
                }
            }
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
