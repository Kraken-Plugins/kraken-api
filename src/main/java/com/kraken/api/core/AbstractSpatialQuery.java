package com.kraken.api.core;

import com.kraken.api.Context;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.util.WorldAreaUtils;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import java.util.Comparator;
import java.util.Optional;

/**
 * Base class for queries over entities that occupy a tile in the loaded scene (NPCs, players,
 * scenery, ground items, projectiles, graphics objects).
 *
 * <p>This is the single owner of the spatial vocabulary. Every spatial query shares these filters and
 * terminals with identical names and semantics, so a new spatial query cannot drift from the others.</p>
 *
 * <h3>Coordinate space and planes</h3>
 * <p>All distances are measured between {@link Locatable#getWorldLocation()} values, which are in the
 * same coordinate space as the local player's own location — including inside instanced regions — so
 * these filters remain valid in raids and other instances. Distance is Chebyshev tile distance via
 * {@link WorldPoint#distanceTo(WorldPoint)}, which treats entities on a different plane as infinitely
 * far away: {@code within} excludes them, {@code nearest} orders them last. {@code at} requires an
 * exact match including the plane.</p>
 *
 * <h3>No local player</h3>
 * <p>Filters anchored on the local player (the no-argument {@code within}, {@code sortByDistance},
 * {@code nearest}) degrade to empty results when there is no local player — at the login screen or
 * mid world-hop — rather than throwing.</p>
 *
 * @param <T> The type of entity being queried, which must expose a world location
 * @param <Q> The concrete query class
 * @param <R> The raw RuneLite type
 */
public abstract class AbstractSpatialQuery<T extends Interactable<R> & Locatable, Q extends AbstractSpatialQuery<T, Q, R>, R> extends AbstractQuery<T, Q, R> {

    public AbstractSpatialQuery(Context ctx) {
        super(ctx);
    }

    /**
     * Resolves the local player's current world location on the client thread.
     * @return The local player's location, or {@code null} when there is no local player.
     */
    protected final WorldPoint localPlayerLocation() {
        return ctx.runOnClientThread(() -> {
            Player player = ctx.getClient().getLocalPlayer();
            return player != null ? player.getWorldLocation() : null;
        }, null);
    }

    /**
     * Chebyshev tile distance between two points, treating a missing point or a plane mismatch as
     * unreachable so despawned entities sort last and never pass a distance filter.
     */
    private static int distanceOrMax(WorldPoint location, WorldPoint anchor) {
        if (location == null || anchor == null) {
            return Integer.MAX_VALUE;
        }
        return location.distanceTo(anchor);
    }

    /**
     * Filters for entities within the given tile distance of the local player, on the same plane.
     * Yields nothing when there is no local player.
     * @param distance The maximum distance in tiles, inclusive.
     * @return Q entities within range of the local player.
     */
    public Q within(int distance) {
        return within(localPlayerLocation(), distance);
    }

    /**
     * Filters for entities within the given tile distance of an anchor point, on the same plane.
     * @param anchor The point to measure from. {@code null} yields nothing.
     * @param distance The maximum distance in tiles, inclusive.
     * @return Q entities within range of the anchor.
     */
    public Q within(WorldPoint anchor, int distance) {
        if (anchor == null) {
            return empty();
        }
        return filter(t -> distanceOrMax(t.getWorldLocation(), anchor) <= distance);
    }

    /**
     * Filters for entities inside the axis-aligned rectangle spanned by two corner points. The corners
     * may be given in any order; the plane component is ignored.
     * @param min One corner of the area, conventionally the south-west tile.
     * @param max The opposite corner, conventionally the north-east tile.
     * @return Q entities inside the area.
     */
    public Q withinArea(WorldPoint min, WorldPoint max) {
        return filter(t -> {
            WorldPoint location = t.getWorldLocation();
            return location != null && WorldAreaUtils.contains(location, min, max);
        });
    }

    /**
     * Filters for entities standing on an exact tile, plane included.
     * @param point The tile to match.
     * @return Q entities at the tile.
     */
    public Q at(WorldPoint point) {
        return filter(t -> {
            WorldPoint location = t.getWorldLocation();
            return location != null && location.equals(point);
        });
    }

    /**
     * Filters for entities the player can currently reach on foot.
     * @return Q reachable entities.
     */
    public Q reachable() {
        return filter(t -> {
            WorldPoint location = t.getWorldLocation();
            return location != null && ctx.getService(TileService.class).isTileReachable(location);
        });
    }

    /**
     * Sorts the matched entities by distance from the local player, closest first. With no local
     * player the order is unchanged.
     * @return Q sorted by proximity to the local player.
     */
    public Q sortByDistance() {
        return sortByDistanceTo(localPlayerLocation());
    }

    /**
     * Sorts the matched entities by distance from an anchor point, closest first.
     * @param anchor The point to measure from.
     * @return Q sorted by proximity to the anchor.
     */
    public Q sortByDistanceTo(WorldPoint anchor) {
        return sorted(Comparator.comparingInt(t -> distanceOrMax(t.getWorldLocation(), anchor)));
    }

    /**
     * Returns the matched entity closest to the local player.
     * @return The nearest entity, or {@link Optional#empty()} when nothing matched or there is no
     *         local player.
     */
    public Optional<T> nearest() {
        WorldPoint anchor = localPlayerLocation();
        if (anchor == null) {
            return Optional.empty();
        }
        return nearestTo(anchor);
    }

    /**
     * Returns the matched entity closest to an anchor point.
     * @param anchor The point to measure from.
     * @return The nearest entity, or {@link Optional#empty()} when nothing matched.
     */
    public Optional<T> nearestTo(WorldPoint anchor) {
        return sortByDistanceTo(anchor).first();
    }
}
