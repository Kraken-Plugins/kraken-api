package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.service.dialogue.DialogueService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.DisplayInfo;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Locale;

/**
 * Crosses a hub transport whose destinations are offered as numbered chat options.
 *
 * <p>Wilderness obelisks read like {@code "1: Level 13 Wilderness"} and really are chat options, so
 * they reuse {@link DialogueService}. Spirit trees and minecarts use the same numbered display info
 * in the dataset but open a dedicated list widget; those go through {@link HubResumePauseHandler}.</p>
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
        String option = chooseOption(dialogue.getDialogueOptions(), displayInfo);
        return option != null && dialogue.selectOption(option);
    }

    /**
     * Chooses the chat option that selects a hub destination.
     *
     * <p>The dataset's label is preferred because it is what the interface shows. Position is a
     * fallback against the same title-stripped list {@link DialogueService#getDialogueOptions()}
     * returns, so {@code "6: Prifddinas"} is the sixth real option rather than widget child index 6.
     * The chosen text is what gets clicked; a raw index is never sent.</p>
     *
     * @param options the visible chat options, title already stripped; may be null
     * @param displayInfo the parsed destination, may be null
     * @return the option text to select, or null when none matches
     */
    public static String chooseOption(List<String> options, DisplayInfo displayInfo) {
        if (options == null || options.isEmpty() || displayInfo == null) {
            return null;
        }

        String label = displayInfo.getLabel();
        if (label != null && !label.isEmpty()) {
            String wanted = label.toLowerCase(Locale.ROOT);
            for (String option : options) {
                if (option != null && option.toLowerCase(Locale.ROOT).contains(wanted)) {
                    return option;
                }
            }
        }

        if (displayInfo.hasPosition()) {
            int index = displayInfo.getPosition() - 1;
            if (index >= 0 && index < options.size()) {
                String option = options.get(index);
                if (option != null && !option.isEmpty()) {
                    return option;
                }
            }
        }

        return null;
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
