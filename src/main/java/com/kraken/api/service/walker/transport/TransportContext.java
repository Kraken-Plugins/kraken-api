package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import lombok.Builder;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportType;

/**
 * Everything a {@link TransportHandler} needs to fire one transport.
 *
 * <p>Handlers are stateless and shared, so all per-crossing state travels here.</p>
 */
@Getter
@Builder
public final class TransportContext {

    /** The API context, used to reach queries and services. */
    private final Context ctx;

    /** The transport edge being crossed, carrying its requirements. */
    private final Transport transport;

    /** Where the transport is entered, or null for a teleport usable anywhere. */
    private final WorldPoint origin;

    /** Where the transport is expected to leave the player. */
    private final WorldPoint destination;

    /** The parsed form of the transport's object info, or null when it carries none. */
    private final ObjectInfo objectInfo;

    /** The transport's display info, which names the destination for teleports and hubs. */
    private final String displayInfo;

    /**
     * Returns the kind of transport being crossed.
     *
     * @return the transport type
     */
    public TransportType getType() {
        return transport != null ? transport.getType() : null;
    }

    /**
     * Reports whether this transport can be used from anywhere rather than from a fixed tile.
     *
     * @return true for teleports
     */
    public boolean isTeleport() {
        TransportType type = getType();
        return type != null && type.isTeleport();
    }

    /**
     * Returns the player's current tile.
     *
     * @return the local player's location, or null when it could not be read
     */
    public WorldPoint playerLocation() {
        return ctx.players().local().location();
    }
}
