package com.kraken.api.core.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.interaction.model.MenuOption;
import com.kraken.api.core.interaction.model.ResolvedMenuAction;
import com.kraken.api.core.packet.entity.MousePackets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;

/**
 * Handles the low-level sending of a resolved menu interaction: queuing the mouse
 * click packet and invoking the menu action via {@link DoActionInvoker}.
 */
@Slf4j
@Singleton
public class InteractionDispatcher {

    @Inject
    private MousePackets mousePackets;

    @Inject
    private DoActionInvoker doActionInvoker;

    /**
     * Dispatches a resolved menu action at the given canvas point.
     *
     * @param point          Canvas coordinates to click
     * @param action         Action label (e.g. "Attack") — used for logging and the engine call
     * @param resolvedAction The fully resolved menu option and target string
     * @return true if the action reached the client's engine, false if it could not be dispatched
     */
    public boolean dispatch(Point point, String action, ResolvedMenuAction resolvedAction) {
        if (point == null || resolvedAction == null) {
            log.warn("Refusing to dispatch action '{}': point={}, resolvedAction={}", action, point, resolvedAction);
            return false;
        }

        MenuOption option = resolvedAction.getOption();
        mousePackets.queueClickPacket(point.getX(), point.getY());

        log.debug("doAction(param0={}, param1={}, MenuAction={}, identifier={}, itemId={}, wv={}, action={}, target={}, x={}, y={})",
                option.getParam0(), option.getParam1(), option.getType().name(),
                option.getIdentifier(), option.getItemId(), option.getWorldView(),
                action, resolvedAction.getTarget(), point.getX(), point.getY());

        return doActionInvoker.invoke(
                option.getParam0(), option.getParam1(), option.getType().getId(),
                option.getIdentifier(), option.getItemId(), option.getWorldView(),
                action, resolvedAction.getTarget(), point.getX(), point.getY()
        );
    }
}