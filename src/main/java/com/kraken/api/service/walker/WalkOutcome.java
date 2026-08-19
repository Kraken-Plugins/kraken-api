package com.kraken.api.service.walker;

/**
 * How a walk ended.
 */
public enum WalkOutcome {

    /** The player reached the destination. */
    SUCCESS,

    /** The pathfinder found no route at all. */
    NO_ROUTE,

    /** The walker was called on the client thread, where it cannot wait for anything. */
    CALLED_ON_CLIENT_THREAD,

    /** The player is inside an instance, whose coordinates do not match the static collision map. */
    IN_INSTANCE,

    /** The player stopped making progress. */
    STALLED,

    /** The walk ran out of its time or round budget. */
    TIMED_OUT,

    /** A transport on the route needs something the player does not have. */
    TRANSPORT_REQUIREMENTS_UNMET,

    /** A transport could not be operated, for example because its object was not in the scene. */
    TRANSPORT_FAILED,

    /** A transport on the route is of a kind the walker cannot yet operate. */
    TRANSPORT_UNSUPPORTED,

    /** The player's position could not be read. */
    UNKNOWN_POSITION
}
