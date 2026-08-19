package com.kraken.api.service.walker.transport;

/**
 * Executes one kind of transport.
 *
 * <p>A handler is responsible only for operating the transport — clicking the door, boarding the boat,
 * casting the spell — and for waiting until that interaction has resolved. Deciding whether the player
 * actually arrived is the walker's job, because arrival is checked the same way for every type.</p>
 *
 * <p>Implementations must be stateless and safe to share; per-crossing state lives on
 * {@link TransportContext}. They block, so they run off the client thread.</p>
 */
public interface TransportHandler {

    /**
     * Operates the transport.
     *
     * @param context the transport being crossed and the services needed to cross it
     * @return true when the interaction was dispatched and appeared to take effect; false when the
     *         handler could not operate the transport at all, which tells the walker to re-plan
     */
    boolean execute(TransportContext context);
}
