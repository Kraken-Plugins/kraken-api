package com.kraken.api.query.gameobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.service.tile.TileService;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import com.kraken.api.util.WorldAreaUtils;
import net.runelite.api.coords.WorldPoint;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GameObjectQuery extends AbstractQuery<GameObjectEntity, GameObjectQuery, GameObject> {

    // A blacklist of actions for game objects (these are actions that are generally on NPC's).
    HashSet<String> ACTION_BLACKLIST = new HashSet<>();

    public GameObjectQuery(Context ctx) {
        super(ctx);
        ACTION_BLACKLIST.add("examine");
        ACTION_BLACKLIST.add("attack");
        ACTION_BLACKLIST.add("pickpocket");
        ACTION_BLACKLIST.add("talk-to");
    }

    @Override
    protected Supplier<Stream<GameObjectEntity>> source() {
        return () -> {
            List<GameObjectEntity> gameObjects = new ArrayList<>();
            for (Tile[] tiles : ctx.getClient().getTopLevelWorldView().getScene().getTiles()[ctx.getClient().getTopLevelWorldView().getPlane()]) {
                if (tiles == null) {
                    continue;
                }

                for (Tile tile : tiles) {
                    if (tile == null) {
                        continue;
                    }

                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject == null || gameObject.getId() == -1) continue;
                        if (gameObject.getSceneMinLocation().equals(tile.getSceneLocation())) {
                            gameObjects.add(new GameObjectEntity(ctx, gameObject));
                        }
                    }
                }
            }

            return gameObjects.stream();
        };
    }

    /**
     * Filters the stream of game objects for objects with a specific name
     * @param name The name of the object to filter for
     * @return GameObjectQuery
     */
    @Override
    public GameObjectQuery withName(String name) {
        return filter(t -> {
            ObjectComposition comp = t.getObjectComposition();
            if(comp == null) return false;
            return comp.getName() != null && comp.getName().equalsIgnoreCase(name);
        });
    }

    /**
     * Filters the stream of game objects for objects which match a specific substring of a name. For example:
     * {@code ctx.gameObjects().nameContains("Oak")} will find Oak tree game objects in the scene.
     * @param name The name to match against
     * @return GameObjectQuery
     */
    @Override
    public GameObjectQuery nameContains(String name) {
        return filter(t -> {
            ObjectComposition comp = t.getObjectComposition();
            if(comp == null) return false;
            return comp.getName() != null && comp.getName().toLowerCase().contains(name.toLowerCase());
        });
    }

    /**
     * Filters game objects to only contain objects which can be interacted with (non Examine) actions. i.e.
     * Trees, Ore, Doors, Stairs etc...
     * @return GameObjectQuery
     */
    public GameObjectQuery interactable() {
        return filter(gameObject ->  {
            String[] rawActions = gameObject.getObjectComposition().getActions();
            if(rawActions == null || rawActions.length == 0) return false;
            return Arrays.stream(rawActions)
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .anyMatch(s -> !ACTION_BLACKLIST.contains(s));
        });
    }

    /**
     * Filters for objects that have a specific action available.
     * Usage: ctx.objects().withAction("Mine").nearest().first();
     * @param action The action to check for i.e "Mine", "Chop", "Examine".
     * @return GameObjectQuery
     */
    public GameObjectQuery withAction(String action) {
        return filter(obj -> {
            if (obj.getObjectComposition() == null) return false;
            String[] actions = obj.getObjectComposition().getActions();
            if (actions == null) return false;
            return Arrays.stream(actions).filter(Objects::nonNull).anyMatch(a -> a.equalsIgnoreCase(action));
        });
    }

    /**
     * Filters the game objects to include only those with at least one action containing the specified substring.
     * <p>
     * This method is case-insensitive and matches the specified substring against all non-null action strings
     * associated with the game object.
     * </p>
     *
     * @param actionSubstring The substring to search for within the actions of the game objects.
     *                        Must not be {@code null}.
     * @return A {@code GameObjectQuery} with the applied filter to include only objects with actions matching
     *         the specified substring.
     */
    public GameObjectQuery withPartialAction(String actionSubstring) {
        return filter(obj -> {
            if (obj.getObjectComposition() == null) return false;
            String[] actions = obj.getObjectComposition().getActions();
            if (actions == null) return false;
            return Arrays.stream(actions).filter(Objects::nonNull).anyMatch(a -> a.toLowerCase().contains(actionSubstring.toLowerCase()));
        });
    }

    /**
     * Finds game objects within a specified area. The min WorldPoint should be the southwest tile of the area
     * and the max WorldPoint should be the northeast tile of the area.
     * @param min The southwest minimum world point of the area to check
     * @param max The northeast maximum world point of the area to check
     * @return GameObjectQuery
     */
    public GameObjectQuery withinArea(WorldPoint min, WorldPoint max) {
        return filter(obj -> WorldAreaUtils.contains(obj.raw().getWorldLocation(), min, max));
    }

    /**
     * Filters for only game objects which are reachable from the players current tile.
     * @return GroundObjectQuery
     */
    public GameObjectQuery reachable() {
        return filter(gameObject -> gameObject.raw() != null && ctx.getService(TileService.class).isObjectReachable(gameObject.raw()));
    }

    /**
     * Sorts the stream of game objects to order them by manhattan distance to the local player.
     * @return GameObjectQuery
     */
    public GameObjectEntity nearest() {
        final WorldPoint playerLoc = ctx.players().local().location();
        return sorted(Comparator.comparingInt(obj -> obj.raw().getWorldLocation().distanceTo(playerLoc))).first();
    }

    /**
     * Sorts the game object stream by distance from the local players current location.
     * @return GameObjectQuery
     */
    public GameObjectQuery sortByDistance() {
        final WorldPoint playerLoc = ctx.players().local().location();
        return sorted(Comparator.comparingInt(obj -> obj.raw().getWorldLocation().distanceTo(playerLoc)));
    }

    /**
     * Filters for only objects whose location is within the specified distance from the anchor point.
     * @param anchor The anchor local point.
     * @param distance The maximum distance from the anchor point (in local units).
     * @return True if the object is within the specified distance from the anchor point, false otherwise.
     */
    public GameObjectQuery within(LocalPoint anchor, int distance) {
        int range = distance * Perspective.LOCAL_TILE_SIZE;
        return filter(obj -> obj.raw().getLocalLocation().distanceTo(anchor) <= range);
    }

    /**
     * Filters for only objects within the specified distance of the local player, measured in world
     * tiles using Chebyshev distance. Use {@link #within(LocalPoint, int)} for sub-tile local-space filtering.
     * @param distance The maximum distance from the player, in world tiles.
     * @return A filtered {@code GameObjectQuery}.
     */
    public GameObjectQuery within(int distance) {
        WorldPoint anchor = ctx.players().local().location();
        return filter(obj -> obj.raw().getWorldLocation().distanceTo(anchor) <= distance);
    }

    /**
     * Filters by exact WorldPoint.
     * @param point The world point to filter for entities on
     * @return GameObjectQuery
     */
    public GameObjectQuery at(WorldPoint point) {
        return filter(obj -> obj.raw().getWorldLocation().equals(point));
    }
}