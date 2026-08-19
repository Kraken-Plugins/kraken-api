package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.TransportHandler;
import com.kraken.api.service.walker.transport.WarningWidgets;
import lombok.extern.slf4j.Slf4j;

/**
 * Crosses a transport that is operated by a single click: doors, gates, stairs, ladders, agility
 * shortcuts, levers and portals.
 *
 * <p>This is the base shape. Transports that need a follow-up step build on it rather than repeating
 * the click. Arrival is judged by the executor, not here — a door often opens without moving the
 * player, so waiting for a step would stall every gating door.</p>
 */
@Slf4j
public class ObjectTransportHandler implements TransportHandler {

    @Override
    public boolean execute(TransportContext context) {
        if (!TransportEntityResolver.interact(context)) {
            return false;
        }

        WarningWidgets.dismiss(context.getCtx());
        return true;
    }
}
