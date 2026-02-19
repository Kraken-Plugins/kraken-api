package com.kraken.api.service.actor;

import com.kraken.api.Context;
import com.kraken.api.service.tile.TileService;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility service for calculating line of sight (LoS), collision, and reachability
 * for actors and tiles within the game world.
 */
public class ActorService {

    private static final Context ctx = RuneLite.getInjector().getInstance(Context.class);
    private static final TileService tileService = RuneLite.getInjector().getInstance(TileService.class);

    /**
     * Checks if there is a clear line of sight between two world points.
     *
     * @param source The starting {@link WorldPoint}.
     * @param other  The target {@link WorldPoint}.
     * @return True if there is an unobstructed line of sight, false otherwise.
     */
    public static boolean hasLineOfSightTo(WorldPoint source, WorldPoint other) {
        Tile sourceTile = tileService.getTile(source.getX(), source.getY());
        Tile otherTile = tileService.getTile(other.getX(), other.getY());

        if(sourceTile == null || otherTile == null) return false;

        return hasLineOfSightTo(sourceTile, otherTile);
    }

    /**
     * Checks if there is a clear line of sight between two scene tiles.
     * Automatically dispatches to the client thread to safely access collision maps.
     *
     * @param source The starting {@link Tile}.
     * @param other  The target {@link Tile}.
     * @return True if there is an unobstructed line of sight, false otherwise.
     */
    public static boolean hasLineOfSightTo(Tile source, Tile other) {
        return ctx.runOnClientThread(() -> {
            if(source == null || other == null) {
                return false;
            }

            if (source.getPlane() != other.getPlane()) {
                return false;
            }

            Client client = ctx.getClient();

            CollisionData[] collisionData = client.getTopLevelWorldView().getCollisionMaps();
            if (collisionData == null) {
                return false;
            }

            int z = source.getPlane();
            int[][] collisionDataFlags = collisionData[z].getFlags();
            return hasLineOfSightToInternal(source, other, collisionDataFlags);
        });
    }

    /**
     * Retrieves a list of all tiles within a specified radius that an NPC currently has line of sight to.
     * <br>
     *
     *
     * @param npc   The source {@link NPC}.
     * @param range The radius to check for visible tiles.
     * @return A list of {@link WorldPoint}s representing visible tiles. Returns an empty list if none are found.
     */
    public static List<WorldPoint> getLineOfSightTiles(NPC npc, int range) {
        return ctx.runOnClientThread(() -> {
            if (npc == null) {
                return new ArrayList<>();
            }

            WorldPoint source = npc.getWorldLocation();
            if (source == null) {
                return new ArrayList<>();
            }

            NPCComposition composition = npc.getComposition();
            int sourceSize = composition != null ? composition.getSize() : 1;
            return getLineOfSightTilesInternal(source, sourceSize, range, true);
        });
    }

    /**
     * Core line of sight algorithm utilizing a grid-based raycasting approach.
     * Full credit to Vitalite's {@code SceneAPI} class for this implementation.
     * <a href="https://github.com/Tonic-Box/VitaLite/blob/main/api/src/main/java/com/tonic/api/game/SceneAPI.java">Source</a>
     * <br>
     *
     *
     * @param source             The starting {@link Tile}.
     * @param other              The target {@link Tile}.
     * @param collisionDataFlags 2D array of collision flags for the current plane.
     * @return True if the path is unobstructed by collision flags.
     */
    private static boolean hasLineOfSightToInternal(Tile source, Tile other, int[][] collisionDataFlags) {
        Point p1 = source.getSceneLocation();
        Point p2 = other.getSceneLocation();
        if (p1.getX() == p2.getX() && p1.getY() == p2.getY()) {
            return true;
        }

        int dx = p2.getX() - p1.getX();
        int dy = p2.getY() - p1.getY();
        int dxAbs = Math.abs(dx);
        int dyAbs = Math.abs(dy);

        int xFlags = CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL;
        int yFlags = CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL;
        if (dx < 0) {
            xFlags |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_EAST;
        } else {
            xFlags |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_WEST;
        }
        if (dy < 0) {
            yFlags |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_NORTH;
        } else {
            yFlags |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_SOUTH;
        }

        if (dxAbs > dyAbs) {
            int x = p1.getX();
            int yBig = p1.getY() << 16;
            int slope = (dy << 16) / dxAbs;
            yBig += 0x8000;
            if (dy < 0) {
                yBig--;
            }
            int direction = dx < 0 ? -1 : 1;

            while (x != p2.getX()) {
                x += direction;
                int y = yBig >>> 16;
                if ((collisionDataFlags[x][y] & xFlags) != 0) {
                    return false;
                }
                yBig += slope;
                int nextY = yBig >>> 16;
                if (nextY != y && (collisionDataFlags[x][nextY] & yFlags) != 0) {
                    return false;
                }
            }
        } else {
            int y = p1.getY();
            int xBig = p1.getX() << 16;
            int slope = (dx << 16) / dyAbs;
            xBig += 0x8000;
            if (dx < 0) {
                xBig--;
            }
            int direction = dy < 0 ? -1 : 1;

            while (y != p2.getY()) {
                y += direction;
                int x = xBig >>> 16;
                if ((collisionDataFlags[x][y] & yFlags) != 0) {
                    return false;
                }
                xBig += slope;
                int nextX = xBig >>> 16;
                if (nextX != x && (collisionDataFlags[nextX][y] & xFlags) != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Scans a square area around a source point to find all tiles within line of sight.
     * Takes multi-tile entity footprints into account.
     *
     * @param source      The starting {@link WorldPoint} (south-west tile of the entity).
     * @param sourceSize  The tile size of the source entity (e.g., 1x1, 2x2).
     * @param range       The maximum distance in tiles to check.
     * @param sourceIsNpc True if the source is an NPC, altering how distances from the footprint are calculated.
     * @return A list of visible {@link WorldPoint}s.
     */
    private static List<WorldPoint> getLineOfSightTilesInternal(
            WorldPoint source,
            int sourceSize,
            int range,
            boolean sourceIsNpc
    ) {
        List<WorldPoint> visibleTiles = new ArrayList<>();
        if (source == null || sourceSize <= 0 || range <= 0) {
            return visibleTiles;
        }

        Client client = ctx.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null || source.getPlane() != worldView.getPlane()) {
            return visibleTiles;
        }

        CollisionData[] collisionData = worldView.getCollisionMaps();
        if (collisionData == null || source.getPlane() < 0 || source.getPlane() >= collisionData.length) {
            return visibleTiles;
        }

        int[][] collisionDataFlags = collisionData[source.getPlane()].getFlags();
        Tile[][][] tiles = worldView.getScene().getTiles();
        if (tiles == null || source.getPlane() < 0 || source.getPlane() >= tiles.length) {
            return visibleTiles;
        }

        for (int x = source.getX() - range; x <= source.getX() + range; x++) {
            for (int y = source.getY() - range; y <= source.getY() + range; y++) {
                WorldPoint target = new WorldPoint(x, y, source.getPlane());
                Tile targetTile = getTileAtWorldPoint(worldView, tiles, target);
                if (targetTile == null) {
                    continue;
                }

                if (collidesSouthWest(source, sourceSize, target, 1)) {
                    continue;
                }

                WorldPoint sourceTilePoint = sourceIsNpc
                        ? getClosestNpcTileToTarget(source, sourceSize, target)
                        : source;

                int dxAbs = Math.abs(target.getX() - sourceTilePoint.getX());
                int dyAbs = Math.abs(target.getY() - sourceTilePoint.getY());
                if (dxAbs > range || dyAbs > range) {
                    continue;
                }

                if (range == 1) {
                    if (isMeleeReachable(source, sourceSize, target)) {
                        visibleTiles.add(target);
                    }
                    continue;
                }

                Tile sourceTile = getTileAtWorldPoint(worldView, tiles, sourceTilePoint);
                if (sourceTile == null) {
                    continue;
                }

                if (hasLineOfSightToInternal(sourceTile, targetTile, collisionDataFlags)) {
                    visibleTiles.add(target);
                }
            }
        }

        return visibleTiles;
    }

    /**
     * Resolves a {@link Tile} object from the scene based on a {@link WorldPoint}.
     *
     * @param worldView The current top-level WorldView.
     * @param tiles     3D array representing all currently loaded scene tiles.
     * @param point     The target {@link WorldPoint} to resolve.
     * @return The {@link Tile} at the specified location, or null if out of bounds/unloaded.
     */
    private static Tile getTileAtWorldPoint(WorldView worldView, Tile[][][] tiles, WorldPoint point) {
        if (point == null) {
            return null;
        }

        LocalPoint localPoint = LocalPoint.fromWorld(worldView, point);
        if (localPoint == null) {
            return null;
        }

        int sceneX = localPoint.getSceneX();
        int sceneY = localPoint.getSceneY();
        int plane = point.getPlane();
        if (sceneX < 0 || sceneY < 0 || plane < 0 || plane >= tiles.length) {
            return null;
        }

        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null || sceneX >= planeTiles.length || planeTiles[sceneX] == null
                || sceneY >= planeTiles[sceneX].length) {
            return null;
        }

        return planeTiles[sceneX][sceneY];
    }

    /**
     * Checks for Axis-Aligned Bounding Box (AABB) intersection between two entities based on their
     * south-west coordinate anchors and footprint sizes.
     * <br>
     *
     *
     * @param first      The south-west {@link WorldPoint} of the first entity.
     * @param firstSize  The square footprint size of the first entity.
     * @param second     The south-west {@link WorldPoint} of the second entity.
     * @param secondSize The square footprint size of the second entity.
     * @return True if the two bounding boxes overlap, false otherwise.
     */
    private static boolean collidesSouthWest(WorldPoint first, int firstSize, WorldPoint second, int secondSize) {
        if (first.getPlane() != second.getPlane()) {
            return false;
        }

        int firstMinX = first.getX();
        int firstMaxX = first.getX() + firstSize - 1;
        int firstMinY = first.getY();
        int firstMaxY = first.getY() + firstSize - 1;

        int secondMinX = second.getX();
        int secondMaxX = second.getX() + secondSize - 1;
        int secondMinY = second.getY();
        int secondMaxY = second.getY() + secondSize - 1;

        return !(firstMaxX < secondMinX
                || secondMaxX < firstMinX
                || firstMaxY < secondMinY
                || secondMaxY < firstMinY);
    }

    /**
     * Calculates the coordinate within a source entity's bounding box that is physically closest to a target point.
     * Used to establish the origin point for line of sight calculations on multi-tile NPCs.
     *
     * @param source     The south-west {@link WorldPoint} of the entity.
     * @param sourceSize The square footprint size of the entity.
     * @param target     The destination {@link WorldPoint}.
     * @return The closest {@link WorldPoint} within the source's footprint to the target.
     */
    private static WorldPoint getClosestNpcTileToTarget(WorldPoint source, int sourceSize, WorldPoint target) {
        int tx = Math.max(source.getX(), Math.min(source.getX() + sourceSize - 1, target.getX()));
        int ty = Math.max(source.getY(), Math.min(source.getY() + sourceSize - 1, target.getY()));
        return new WorldPoint(tx, ty, source.getPlane());
    }

    /**
     * Determines if a target tile is reachable via melee from a source entity.
     * Requires orthogonal adjacency (no diagonals) to the outer edge of the source's bounding box.
     * <br>
     *
     *
     * @param source     The south-west {@link WorldPoint} of the attacker.
     * @param sourceSize The square footprint size of the attacker.
     * @param target     The {@link WorldPoint} of the target.
     * @return True if the target is orthogonally adjacent to the source footprint.
     */
    private static boolean isMeleeReachable(WorldPoint source, int sourceSize, WorldPoint target) {
        int dx = target.getX() - source.getX();
        int dy = target.getY() - source.getY();

        return (dx < sourceSize && dx >= 0 && (dy == sourceSize || dy == -1))
                || (dy < sourceSize && dy >= 0 && (dx == -1 || dx == sourceSize));
    }
}