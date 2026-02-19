package com.kraken.api.service.actor;

import com.kraken.api.Context;
import com.kraken.api.service.tile.TileService;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;

import java.util.ArrayList;
import java.util.List;

public class ActorService {

    private static final Context ctx = RuneLite.getInjector().getInstance(Context.class);
    private static final TileService tileService = RuneLite.getInjector().getInstance(TileService.class);

    public static boolean hasLineOfSightTo(WorldPoint source, WorldPoint other) {
        Tile sourceTile = tileService.getTile(source.getX(), source.getY());
        Tile otherTile = tileService.getTile(other.getX(), other.getY());

        if(sourceTile == null || otherTile == null) return false;

        return hasLineOfSightTo(sourceTile, otherTile);
    }

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

    public static List<WorldPoint> getLineOfSightTiles(
            WorldPoint source,
            int sourceSize,
            int range,
            boolean sourceIsNpc
    ) {
        return ctx.runOnClientThread(() -> getLineOfSightTilesInternal(source, sourceSize, range, sourceIsNpc));
    }

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

    private static WorldPoint getClosestNpcTileToTarget(WorldPoint source, int sourceSize, WorldPoint target) {
        int tx = Math.max(source.getX(), Math.min(source.getX() + sourceSize - 1, target.getX()));
        int ty = Math.max(source.getY(), Math.min(source.getY() + sourceSize - 1, target.getY()));
        return new WorldPoint(tx, ty, source.getPlane());
    }

    private static boolean isMeleeReachable(WorldPoint source, int sourceSize, WorldPoint target) {
        int dx = target.getX() - source.getX();
        int dy = target.getY() - source.getY();

        return (dx < sourceSize && dx >= 0 && (dy == sourceSize || dy == -1))
                || (dy < sourceSize && dy >= 0 && (dx == -1 || dx == sourceSize));
    }
}
