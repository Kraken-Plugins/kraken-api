package com.kraken.api.query.tileobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.util.WorldAreaUtils;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Queries every kind of scenery in the loaded scene: game, wall, decorative and ground objects.
 *
 * <p>{@link com.kraken.api.query.gameobject.GameObjectQuery} reads only {@code tile.getGameObjects()},
 * which leaves wall objects unreachable — and a large share of the game's doors and gates are wall
 * objects. This query exists so that anything which has to open a door can find it.</p>
 *
 * <p>Prefer {@code GameObjectQuery} when a game object is what you want; this query returns a wider
 * result set and therefore needs more specific filters to be useful.</p>
 */
public class TileObjectQuery extends AbstractQuery<TileObjectEntity, TileObjectQuery, TileObject> {

    public TileObjectQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<TileObjectEntity>> source() {
        return () -> {
            List<TileObjectEntity> objects = new ArrayList<>();
            int plane = ctx.getClient().getTopLevelWorldView().getPlane();
            Tile[][] planeTiles = ctx.getClient().getTopLevelWorldView().getScene().getTiles()[plane];

            for (Tile[] tiles : planeTiles) {
                if (tiles == null) {
                    continue;
                }

                for (Tile tile : tiles) {
                    if (tile == null) {
                        continue;
                    }

                    collect(objects, tile);
                }
            }

            return objects.stream();
        };
    }

    /**
     * Adds every non-null object standing on a tile, de-duplicating multi-tile game objects.
     *
     * <p>A game object that spans several tiles is reported by each tile it covers, so it is only
     * taken from the tile matching its south west corner. The other three kinds occupy a single tile
     * and need no such guard.</p>
     */
    private void collect(List<TileObjectEntity> objects, Tile tile) {
        for (GameObject gameObject : tile.getGameObjects()) {
            if (gameObject == null || gameObject.getId() == -1) {
                continue;
            }
            if (gameObject.getSceneMinLocation().equals(tile.getSceneLocation())) {
                objects.add(new TileObjectEntity(ctx, gameObject));
            }
        }

        if (tile.getWallObject() != null) {
            objects.add(new TileObjectEntity(ctx, tile.getWallObject()));
        }

        if (tile.getDecorativeObject() != null) {
            objects.add(new TileObjectEntity(ctx, tile.getDecorativeObject()));
        }

        if (tile.getGroundObject() != null) {
            objects.add(new TileObjectEntity(ctx, tile.getGroundObject()));
        }
    }

    /**
     * Filters for objects whose composition name matches exactly, ignoring case.
     *
     * @param name the name to match
     * @return this query
     */
    @Override
    public TileObjectQuery withName(String name) {
        return filter(object -> {
            ObjectComposition comp = object.getObjectComposition();
            return comp != null && comp.getName() != null && comp.getName().equalsIgnoreCase(name);
        });
    }

    /**
     * Filters for objects whose composition name contains the given substring, ignoring case.
     *
     * @param name the substring to match
     * @return this query
     */
    @Override
    public TileObjectQuery nameContains(String name) {
        return filter(object -> {
            ObjectComposition comp = object.getObjectComposition();
            return comp != null
                    && comp.getName() != null
                    && comp.getName().toLowerCase().contains(name.toLowerCase());
        });
    }

    /**
     * Filters for objects offering a specific menu action.
     *
     * @param action the action to look for, for example "Open"
     * @return this query
     */
    public TileObjectQuery withAction(String action) {
        return filter(object -> {
            ObjectComposition comp = object.getObjectComposition();
            if (comp == null || comp.getActions() == null) {
                return false;
            }
            return Arrays.stream(comp.getActions())
                    .filter(Objects::nonNull)
                    .anyMatch(a -> a.equalsIgnoreCase(action));
        });
    }

    /**
     * Filters for objects standing on an exact tile.
     *
     * @param point the tile to match
     * @return this query
     */
    public TileObjectQuery at(WorldPoint point) {
        return filter(object -> {
            WorldPoint location = object.getWorldLocation();
            return location != null && location.equals(point);
        });
    }

    /**
     * Filters for objects within the given tile distance of a point.
     *
     * @param anchor the point to measure from
     * @param distance the maximum distance in tiles
     * @return this query
     */
    public TileObjectQuery near(WorldPoint anchor, int distance) {
        return filter(object -> {
            WorldPoint location = object.getWorldLocation();
            return location != null && location.distanceTo(anchor) <= distance;
        });
    }

    /**
     * Filters for objects within the given tile distance of the local player.
     *
     * @param distance the maximum distance in tiles
     * @return this query
     */
    public TileObjectQuery within(int distance) {
        return near(ctx.players().local().location(), distance);
    }

    /**
     * Filters for objects inside a rectangular area.
     *
     * @param min the south west corner
     * @param max the north east corner
     * @return this query
     */
    public TileObjectQuery withinArea(WorldPoint min, WorldPoint max) {
        return filter(object -> {
            WorldPoint location = object.getWorldLocation();
            return location != null && WorldAreaUtils.contains(location, min, max);
        });
    }

    /**
     * Filters for objects the player can currently reach on foot.
     *
     * @return this query
     */
    public TileObjectQuery reachable() {
        return filter(object -> {
            WorldPoint location = object.getWorldLocation();
            return location != null && ctx.getService(TileService.class).isTileReachable(location);
        });
    }

    /**
     * Sorts by distance from the local player.
     *
     * @return this query
     */
    public TileObjectQuery sortByDistance() {
        final WorldPoint playerLocation = ctx.players().local().location();
        return sorted(Comparator.comparingInt(object -> distanceOrMax(object, playerLocation)));
    }

    /**
     * Returns the object closest to the local player.
     *
     * @return the nearest object, which may wrap null when nothing matched
     */
    public TileObjectEntity nearest() {
        return sortByDistance().first();
    }

    /**
     * Returns the object closest to a given point.
     *
     * @param anchor the point to measure from
     * @return the nearest object, which may wrap null when nothing matched
     */
    public TileObjectEntity nearestTo(WorldPoint anchor) {
        return sorted(Comparator.comparingInt(object -> distanceOrMax(object, anchor))).first();
    }

    private static int distanceOrMax(TileObjectEntity object, WorldPoint anchor) {
        WorldPoint location = object.getWorldLocation();
        if (location == null || anchor == null) {
            return Integer.MAX_VALUE;
        }
        return location.distanceTo(anchor);
    }
}
