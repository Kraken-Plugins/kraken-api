package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.DisplayInfo;
import com.kraken.api.service.walker.transport.HubResumePause;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Crosses a hub whose destinations are selected with resume-pause on a dedicated list widget.
 *
 * <p>Spirit trees and minecarts open that list after {@code Travel}. Waiting for chat options never
 * sees it, which is why a Stronghold tree to the Grand Exchange used to time out after the click.</p>
 */
@Slf4j
public class HubResumePauseHandler implements TransportHandler {

    /** How long to wait for the destination list to appear. */
    private static final long LIST_TIMEOUT_MS = 6_000;

    /** How long to wait for the journey to finish. */
    private static final long TRAVEL_TIMEOUT_MS = 15_000;

    /** How far the player must move for the journey to count as having happened. */
    private static final int TRAVEL_DISTANCE = 16;

    @Override
    public boolean execute(TransportContext context) {
        DisplayInfo displayInfo = DisplayInfo.parse(context.getDisplayInfo());
        int index = HubResumePause.optionIndex(displayInfo);
        if (index == HubResumePause.NO_INDEX) {
            log.debug("Hub transport carries no destination index: {}", context.getDisplayInfo());
            return false;
        }

        WorldPoint before = context.playerLocation();
        if (!TransportEntityResolver.interact(context)) {
            return false;
        }

        if (!SleepService.sleepUntil(() -> HubResumePause.isOpen(context.getCtx()), LIST_TIMEOUT_MS)) {
            log.debug("Destination list did not appear for {}", context.getType());
            return false;
        }

        if (!context.getCtx().getInteractionManager().selectDialogueOption(HubResumePause.LIST, index)) {
            log.debug("Could not select index {} for '{}'", index,
                    displayInfo != null ? displayInfo.getLabel() : context.getDisplayInfo());
            return false;
        }

        return awaitTravel(context, before);
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
