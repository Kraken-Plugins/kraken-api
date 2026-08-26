package com.kraken.api.service.walker.transport;

import net.runelite.api.coords.WorldPoint;

/**
 * Decides whether a transport crossing has happened, from the player's tiles alone.
 *
 * <p>Kept pure so it can be tested without a client. Live-scene reachability of the destination is
 * a separate check the executor applies while it waits, for doors that open underfoot without
 * moving the player, and before it clicks, for doors another player already opened.</p>
 */
public final class TransportArrival {

    /** How close to the recorded destination counts as having arrived, in tiles. */
    public static final int NEAR_DESTINATION_TILES = 3;

    /** How far the player must move for a teleport or hub ride to count. */
    public static final int TELEPORT_DISTANCE = 16;

    private TransportArrival() {
    }

    /**
     * Reports whether the player's new tile is evidence that a transport was crossed.
     *
     * <p>Standing still on the origin is not arrival, even when the destination is the next tile.
     * Dataset doors are often one tile apart, and treating that proximity as success is what made a
     * closed door look like a crossing.</p>
     *
     * @param before where the player stood before the transport was operated, may be null
     * @param now where the player stands now, may be null
     * @param destination where the transport is expected to leave them, may be null
     * @return true when the tiles show a crossing
     */
    public static boolean crossed(WorldPoint before, WorldPoint now, WorldPoint destination) {
        if (now == null) {
            return false;
        }

        if (before != null && now.getPlane() != before.getPlane()) {
            return true;
        }

        if (before != null && now.distanceTo(before) > TELEPORT_DISTANCE) {
            return true;
        }

        if (destination == null || now.getPlane() != destination.getPlane()) {
            return false;
        }

        if (now.distanceTo(destination) > NEAR_DESTINATION_TILES) {
            return false;
        }

        return before == null || !now.equals(before);
    }

    /**
     * Reports whether a transport's destination is the next tile and already walkable.
     *
     * <p>A dataset door another player left open has a different object id and no {@code Open} action.
     * Clicking it fails. The far tile of a closed door is not in the live-scene flood, so this stays
     * false until the door is actually open. Stairs stay on another plane, so they are still
     * clicked.</p>
     *
     * <p>Reachable on this plane is not enough. The scene flood covers a hundred tiles: the far side
     * of the Varrock underwall is reachable by walking around, and treating that as an open door is
     * what skipped Climb-into and stalled on the tunnel origin. Only the next tile counts — that is
     * an open door, not a shortcut through a wall.</p>
     *
     * @param here where the player is standing, may be null
     * @param destination where the transport leads, may be null
     * @param destinationReachable whether {@code destination} is reachable in the live scene
     * @return true when the destination is a walking neighbour and already walkable
     */
    public static boolean alreadyOpen(WorldPoint here, WorldPoint destination, boolean destinationReachable) {
        if (!destinationReachable || here == null || destination == null) {
            return false;
        }

        if (here.getPlane() != destination.getPlane()) {
            return false;
        }

        return here.distanceTo(destination) <= 1;
    }

    /**
     * Whether a teleport landing is already walkable on this plane, so the item should not be used.
     *
     * <p>Jewellery has no origin tile, so the planner will pick a glory stop that is already on this
     * island. Walking there is correct; skipping the teleport as an open door is what stalled a
     * twelve-tile walk on Musa Point.</p>
     *
     * @param here where the player is standing, may be null
     * @param landing where the teleport leads, may be null
     * @param landingReachable whether {@code landing} is reachable in the live scene
     * @return true when the landing is walkable without teleporting
     */
    public static boolean landingWalkable(WorldPoint here, WorldPoint landing, boolean landingReachable) {
        if (!landingReachable || here == null || landing == null) {
            return false;
        }

        return here.getPlane() == landing.getPlane();
    }

    /**
     * Whether the executor should click nothing because the crossing is already possible.
     *
     * <p>An adjacent open door yes. Teleports no, and neither is an agility tunnel whose far side is
     * reachable only by walking around the wall.</p>
     *
     * @param here where the player is standing, may be null
     * @param destination where the transport leads, may be null
     * @param destinationReachable whether {@code destination} is reachable in the live scene
     * @param teleport true when this edge is a teleport rather than a door or shortcut
     * @return true when the click should be skipped and counted as already crossed
     */
    public static boolean skipOperating(
            WorldPoint here,
            WorldPoint destination,
            boolean destinationReachable,
            boolean teleport) {
        return !teleport && alreadyOpen(here, destination, destinationReachable);
    }

    /**
     * Whether waiting after a click should stop: the tiles show a crossing, or a door opened
     * underfoot.
     *
     * <p>Reachable somewhere in the scene is not arrival. The Varrock underwall's far side is in the
     * flood by walking around, and treating that as done is what clicked Climb-into again while the
     * first climb was still playing.</p>
     *
     * <p>Near the destination after a short step is not enough either. The wilderness ditch dest is
     * three tiles north of the origin — the same radius as {@link #NEAR_DESTINATION_TILES} — so
     * walking the south bank counted as a crossing and Cross fired again before the jump.</p>
     *
     * @param before where the player stood before the click, may be null
     * @param now where they stand now, may be null
     * @param destination where the transport leads, may be null
     * @param destinationReachable whether {@code destination} is reachable in the live scene
     * @return true when the wait should end
     */
    public static boolean arrived(
            WorldPoint before,
            WorldPoint now,
            WorldPoint destination,
            boolean destinationReachable) {
        if (now == null) {
            return false;
        }

        if (before != null && now.getPlane() != before.getPlane()) {
            return true;
        }

        if (before != null && now.distanceTo(before) > TELEPORT_DISTANCE) {
            return true;
        }

        if (alreadyOpen(now, destination, destinationReachable)) {
            return true;
        }

        return destinationReachable && crossed(before, now, destination);
    }
}
