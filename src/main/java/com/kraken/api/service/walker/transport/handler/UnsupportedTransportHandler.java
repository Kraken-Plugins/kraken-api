package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Refuses a transport whose destination chooser has not been built yet.
 *
 * <p>Two kinds remain. A canoe has to be felled and shaped before it will float, which is a build
 * sequence gated on a woodcutting level and an axe rather than a destination choice. A minigame
 * teleport is driven from the grouping tab, which {@code InterfaceTab} does not currently name.</p>
 *
 * <p>Both fail immediately and say why, so a route through one ends with a clear reason instead of a
 * walker standing at a canoe station doing nothing. Turning the matching option off in
 * {@code GlobalPathfinderConfig} keeps them out of routes altogether.</p>
 */
@Slf4j
public class UnsupportedTransportHandler implements TransportHandler {

    @Override
    public boolean execute(TransportContext context) {
        log.warn("No handler implemented for {} ({} -> {})",
                context.getType(), context.getOrigin(), context.getDestination());
        return false;
    }
}
