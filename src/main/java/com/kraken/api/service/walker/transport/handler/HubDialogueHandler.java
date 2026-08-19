package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.service.dialogue.DialogueService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.DisplayInfo;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Crosses a hub transport whose destinations are offered as a numbered list.
 *
 * <p>Spirit trees and the other hubs that read like {@code "6: Prifddinas"} present their stops as
 * chat options rather than a dedicated interface. The dataset supplies both halves of that entry, so
 * the destination is chosen by name where possible and by position where the name does not match —
 * the two disagree occasionally, and the position is the more literal of the pair.</p>
 */
@Slf4j
public class HubDialogueHandler implements TransportHandler {

    /** How long to wait for the destination list to appear. */
    private static final long DIALOGUE_TIMEOUT_MS = 6_000;

    /** How long to wait for the journey to finish. */
    private static final long TRAVEL_TIMEOUT_MS = 15_000;

    /** How far the player must move for the journey to count as having happened. */
    private static final int TRAVEL_DISTANCE = 16;

    @Override
    public boolean execute(TransportContext context) {
        DisplayInfo displayInfo = DisplayInfo.parse(context.getDisplayInfo());
        if (displayInfo == null) {
            log.debug("Hub transport carries no destination: {}", context.getDisplayInfo());
            return false;
        }

        WorldPoint before = context.playerLocation();
        if (!TransportEntityResolver.interact(context)) {
            return false;
        }

        DialogueService dialogue = context.getCtx().getService(DialogueService.class);
        if (!SleepService.sleepUntil(dialogue::isDialoguePresent, DIALOGUE_TIMEOUT_MS)) {
            log.debug("No destination list appeared for {}", context.getType());
            return false;
        }

        if (!choose(dialogue, displayInfo)) {
            log.debug("Could not select '{}' from the destination list", displayInfo.getLabel());
            return false;
        }

        return awaitTravel(context, before);
    }

    /**
     * Picks the destination, by name first and by position as a fallback.
     */
    private boolean choose(DialogueService dialogue, DisplayInfo displayInfo) {
        String label = displayInfo.getLabel();
        if (label != null && !label.isEmpty() && dialogue.isOptionPresent(label)) {
            return dialogue.selectOption(label);
        }

        if (displayInfo.hasPosition()) {
            dialogue.selectOption(displayInfo.getPosition());
            return true;
        }

        return false;
    }

    private boolean awaitTravel(TransportContext context, WorldPoint before) {
        if (before == null) {
            SleepService.sleepFor(3);
            return true;
        }

        return SleepService.sleepUntil(() -> {
            WorldPoint now = context.playerLocation();
            return now != null && now.distanceTo(before) > TRAVEL_DISTANCE;
        }, TRAVEL_TIMEOUT_MS);
    }
}
