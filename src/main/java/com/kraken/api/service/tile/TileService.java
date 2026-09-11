package com.kraken.api.service.tile;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.*;


@Slf4j
@Singleton
public class TileService {

    // Client-thread owned. Keep an exact collision copy: flags can change within one game tick.
    private Reachability cachedReachability;

    private static final class Reachability {
        private WorldView worldView;
        private Scene scene;
        private int tick, baseX, baseY, plane, originX, originY;
        private int[][] flags;
        private boolean[][] visited;
    }

    @Inject
    private Provider<Context> ctxProvider;

    /**
     * Returns the object composition for a given TileObject.
     * @param tileObject The tile object to retrieve the composition for
     * @return The object composition for a given tile object
     */
    public ObjectComposition getObjectComposition(TileObject tileObject) {
        ObjectComposition def = ctxProvider.get().runOnClientThread(() -> ctxProvider.get().getClient().getObjectDefinition(tileObject.getId()));
        if(def.getImpostorIds() != null && def.getImpostor() != null) {
            return ctxProvider.get().runOnClientThread(def::getImpostor);
        }

        return def;
    }

    /**
     * This method calculates the distances to a specified tile in the game world
     * using a breadth-first search (BFS) algorithm, considering movement restrictions
     * and collision data. The distances are stored in a HashMap where the key is a
     * WorldPoint (representing a tile location), and the value is the distance
     * from the starting tile. The method accounts for movement flags that block
     * movement in specific directions (east, west, north, south) and removes
     * unreachable tiles based on collision data.
     * <p>
     * The method iterates over a range of distances, progressively updating
     * reachable tiles and adding them to the tileDistances map. It checks if a
     * tile can be reached by verifying its collision flags and whether it’s blocked
     * for movement in any direction.
     *
     * @param tile The starting tile for the distance calculation.
     * @param distance The maximum distance to calculate to neighboring tiles.
     * @param ignoreCollision If true, ignores collision data during the calculation.
     * @return A HashMap containing WorldPoints and their corresponding distances from the start tile.
     */
    public HashMap<WorldPoint, Integer> getReachableTilesFromTile(WorldPoint tile, int distance, boolean ignoreCollision) {
        final HashMap<WorldPoint, Integer> tileDistances = new HashMap<>();
        tileDistances.put(tile, 0);

        // Expand ring by ring from an explicit frontier of the tiles just discovered, rather than
        // re-scanning the whole distance map on every ring. Each tile is visited once.
        List<WorldPoint> frontier = new ArrayList<>();
        frontier.add(tile);

        for (int i = 0; i < distance + 1 && !frontier.isEmpty(); i++) {
            int dist = i;
            List<WorldPoint> nextFrontier = new ArrayList<>();

            for (WorldPoint point : frontier) {
                LocalPoint localPoint;
                if (ctxProvider.get().getClient().getTopLevelWorldView().isInstance()) {
                    WorldPoint worldPoint = WorldPoint.toLocalInstance(ctxProvider.get().getClient().getTopLevelWorldView(), point).stream().findFirst().orElse(null);
                    if (worldPoint == null) break;
                    localPoint = LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), worldPoint);
                } else {
                    localPoint = LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), point);
                }

                CollisionData[] collisionMap = ctxProvider.get().getClient().getTopLevelWorldView().getCollisionMaps();
                if (collisionMap != null && localPoint != null) {
                    CollisionData collisionData = collisionMap[ctxProvider.get().getClient().getTopLevelWorldView().getPlane()];
                    int[][] flags = collisionData.getFlags();
                    int data = flags[localPoint.getSceneX()][localPoint.getSceneY()];

                    Set<MovementFlag> movementFlags = MovementFlag.getSetFlags(data);

                    if (!ignoreCollision && !tile.equals(point)) {
                        if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_FULL) || movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_FLOOR)) {
                            tileDistances.remove(point);
                            continue;
                        }
                    }

                    if (dist >= distance)
                        continue;

                    if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_EAST))
                        addNeighbour(point.dx(1), dist + 1, tileDistances, nextFrontier);
                    if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_WEST))
                        addNeighbour(point.dx(-1), dist + 1, tileDistances, nextFrontier);
                    if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH))
                        addNeighbour(point.dy(1), dist + 1, tileDistances, nextFrontier);
                    if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH))
                        addNeighbour(point.dy(-1), dist + 1, tileDistances, nextFrontier);
                }
            }

            frontier = nextFrontier;
        }

        return tileDistances;
    }

    /**
     * Records a neighbouring tile at the given distance if it has not been seen yet, adding it to the
     * next frontier so it is expanded exactly once.
     *
     * @param neighbour     The neighbouring world point.
     * @param neighbourDist The distance to assign to the neighbour.
     * @param tileDistances The accumulated tile-to-distance map.
     * @param nextFrontier  The frontier for the next ring, appended to when the neighbour is new.
     */
    private void addNeighbour(WorldPoint neighbour, int neighbourDist, HashMap<WorldPoint, Integer> tileDistances, List<WorldPoint> nextFrontier) {
        if (tileDistances.putIfAbsent(neighbour, neighbourDist) == null) {
            nextFrontier.add(neighbour);
        }
    }

    /**
     * Checks whether the player can reach the live footprint or a cardinal boundary tile with no
     * separating movement wall. Scene bounds already include placement rotation and even sizes.
     * This geometric approach check does not prove object-specific access rules or server acceptance.
     * @param obj object in the player's world view
     * @return true when a footprint tile or unobstructed cardinal approach is reachable
     */
    public boolean isObjectReachable(GameObject obj) {
        if (obj == null) return false;
        return Boolean.TRUE.equals(ctxProvider.get().runOnClientThread(() -> {
            WorldView worldView = obj.getWorldView();
            if (worldView == null || obj.getPlane() != worldView.getPlane()) return false;
            Point min = obj.getSceneMinLocation();
            Point max = obj.getSceneMaxLocation();
            Reachability reachable = getReachability(worldView);
            if (min == null || max == null || reachable == null
                    || !isWithinBounds(min.getX(), min.getY(), reachable.flags.length, reachable.flags[0].length)
                    || !isWithinBounds(max.getX(), max.getY(), reachable.flags.length, reachable.flags[0].length)
                    || min.getX() > max.getX() || min.getY() > max.getY()) return false;
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    if (reachable.visited[x][y]) return true;
                }
                if (approach(reachable, x, min.getY() - 1, x, min.getY())
                        || approach(reachable, x, max.getY() + 1, x, max.getY())) return true;
            }
            for (int y = min.getY(); y <= max.getY(); y++) {
                if (approach(reachable, min.getX() - 1, y, min.getX(), y)
                        || approach(reachable, max.getX() + 1, y, max.getX(), y)) return true;
            }
            return false;
        }));
    }

    private boolean approach(Reachability reachable, int x, int y, int targetX, int targetY) {
        return isWithinBounds(x, y, reachable.flags.length, reachable.flags[0].length) && reachable.visited[x][y]
                && openEdge(reachable.flags, x, y, targetX, targetY);
    }

    /** Captures all cache inputs and computes a flood while still on the client thread. */
    private Reachability getReachability(WorldView worldView) {
        Client client = ctxProvider.get().getClient();
        Player player = client.getLocalPlayer();
        if (worldView == null || player == null || player.getWorldView() != worldView) return null;
        LocalPoint origin = player.getLocalLocation();
        if (origin == null) return null;
        int plane = worldView.getPlane();
        CollisionData[] maps = worldView.getCollisionMaps();
        if (maps == null || plane < 0 || plane >= maps.length || maps[plane] == null) return null;
        int[][] flags = maps[plane].getFlags();
        if (flags == null || flags.length == 0 || flags[0] == null || flags[0].length == 0) return null;
        for (int[] row : flags) {
            if (row == null || row.length != flags[0].length) return null;
        }
        if (!isWithinBounds(origin.getSceneX(), origin.getSceneY(), flags.length, flags[0].length)) return null;
        Reachability cached = cachedReachability;
        if (cached != null && cached.worldView == worldView && cached.scene == worldView.getScene()
                && cached.tick == client.getTickCount() && cached.baseX == worldView.getBaseX()
                && cached.baseY == worldView.getBaseY() && cached.plane == plane
                && cached.originX == origin.getSceneX() && cached.originY == origin.getSceneY()
                && Arrays.deepEquals(cached.flags, flags)) return cached;

        Reachability captured = new Reachability();
        captured.worldView = worldView;
        captured.scene = worldView.getScene();
        captured.tick = client.getTickCount();
        captured.baseX = worldView.getBaseX();
        captured.baseY = worldView.getBaseY();
        captured.plane = plane;
        captured.originX = origin.getSceneX();
        captured.originY = origin.getSceneY();
        captured.flags = Arrays.stream(flags).map(row -> row == null ? null : row.clone()).toArray(int[][]::new);
        captured.visited = floodReachableTiles(captured.originX, captured.originY, captured.flags);
        cachedReachability = captured;
        return captured;
    }

    /** Checks both sides of a cardinal edge; full object occupancy is checked separately. */
    private boolean openEdge(int[][] flags, int x, int y, int targetX, int targetY) {
        int leaving, entering;
        if (targetX < x) {
            leaving = CollisionDataFlag.BLOCK_MOVEMENT_WEST;
            entering = CollisionDataFlag.BLOCK_MOVEMENT_EAST;
        } else if (targetX > x) {
            leaving = CollisionDataFlag.BLOCK_MOVEMENT_EAST;
            entering = CollisionDataFlag.BLOCK_MOVEMENT_WEST;
        } else if (targetY < y) {
            leaving = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
            entering = CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
        } else {
            leaving = CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
            entering = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
        }
        return (flags[x][y] & leaving) == 0 && (flags[targetX][targetY] & entering) == 0;
    }

    /**
     * Flood-fills the reachable tiles from a scene-coordinate start using a 4-cardinal BFS over the
     * collision flags. A neighbour is entered only when the current tile permits leaving in that
     * direction and the neighbour is not fully blocked.
     *
     * @param startX The start tile's scene x-coordinate (0-103).
     * @param startY The start tile's scene y-coordinate (0-103).
     * @param flags  The current plane's collision flags.
     * @return a matrix matching the collision dimensions, where true marks a reachable tile.
     */
    private boolean[][] floodReachableTiles(int startX, int startY, int[][] flags) {
        boolean[][] visited = new boolean[flags.length][flags[0].length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        queue.add((startX << 16) | startY);
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int point = queue.poll();
            int x = point >> 16;
            int y = point & 0xFFFF;

            checkNeighbour(queue, visited, flags, x, y, -1, 0);
            checkNeighbour(queue, visited, flags, x, y, 1, 0);
            checkNeighbour(queue, visited, flags, x, y, 0, -1);
            checkNeighbour(queue, visited, flags, x, y, 0, 1);
        }

        return visited;
    }

    /**
     * Enters a neighbouring tile into the BFS when movement into it is permitted and it is unvisited.
     * Movement requires the current tile not to block travel in the given direction and the
     * destination not to be fully blocked.
     *
     * @param queue             The BFS work queue of packed scene coordinates.
     * @param visited           The visited matrix, updated in place.
     * @param flags             The current plane's collision flags.
     * @param x                 The current tile's scene x-coordinate.
     * @param y                 The current tile's scene y-coordinate.
     * @param dx                The x-offset of the neighbour (-1, 0 or 1).
     * @param dy                The y-offset of the neighbour (-1, 0 or 1).
     */
    private void checkNeighbour(ArrayDeque<Integer> queue, boolean[][] visited, int[][] flags, int x, int y, int dx, int dy) {
        int nx = x + dx;
        int ny = y + dy;

        if (isWithinBounds(nx, ny, flags.length, flags[0].length) && !visited[nx][ny]
                && openEdge(flags, x, y, nx, ny)
                && (flags[nx][ny] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0) {
            queue.add((nx << 16) | ny);
            visited[nx][ny] = true;
        }
    }


    /**
     * This method checks if a given target tile (WorldPoint) is reachable from the
     * player's current location, considering collision data and the plane of the
     * world. The method uses a breadth-first search (BFS) algorithm to traverse
     * neighboring tiles while checking for movement blocks in the four cardinal
     * directions (north, south, east, west). It ensures the target tile is within
     * the same plane as the player and that movement between tiles is not blocked.
     * <p>
     * The method initializes a queue to explore the world grid, marking visited
     * tiles to avoid revisiting. It checks the flags for collision data to determine
     * whether movement is allowed in each direction, and only adds neighboring tiles
     * to the queue if they are not blocked. Finally, it verifies if the target point
     * has been visited during the traversal and returns true if reachable, false otherwise.
     *
     * @param targetPoint The WorldPoint representing the target tile to check for
     *                    reachability.
     * @return True if the target tile is reachable from the player's location,
     *         otherwise false.
     */
    public boolean isTileReachable(WorldPoint targetPoint) {
        if (targetPoint == null) return false;
        return Boolean.TRUE.equals(ctxProvider.get().runOnClientThread(() -> {
            WorldView worldView = ctxProvider.get().getClient().getTopLevelWorldView();
            Reachability reachable = getReachability(worldView);
            return reachable != null && isVisited(worldView, targetPoint, reachable.visited);
        }));
    }

    /**
     * This method checks whether a given WorldPoint has been visited during the
     * traversal of the game world. It calculates the tile’s local coordinates relative
     * to the base coordinates, considering whether the client is in an instanced region
     * or not. The method then checks if the calculated coordinates are within bounds
     * and if the tile has been marked as visited in the provided visited array.
     * <p>
     * The method ensures that the given WorldPoint corresponds to a valid tile on
     * the game map by verifying if its coordinates fall within the bounds of the
     * world grid, and if so, it checks whether that tile has already been visited
     * during the search or traversal process.
     *
     * @param wv the world view owning the collision snapshot
     * @param worldPoint The WorldPoint representing the tile to check for visit status.
     * @param visited A 2D boolean array tracking visited tiles during world traversal.
     * @return True if the tile has been visited and is within bounds, otherwise false.
     */
    private boolean isVisited(WorldView wv, WorldPoint worldPoint, boolean[][] visited) {
        if (wv.isInstance()) {
            // In an instance the target world point maps to one or more instanced scene positions;
            // the tile is reachable if any of them was visited. Convert the target itself here — not
            // the player's location — otherwise every target reads as reachable.
            for (WorldPoint instancePoint : WorldPoint.toLocalInstance(wv, worldPoint)) {
                if (instancePoint.getPlane() != wv.getPlane()) continue;
                LocalPoint localPoint = LocalPoint.fromWorld(wv, instancePoint);
                if (localPoint != null && isWithinBounds(localPoint.getSceneX(), localPoint.getSceneY(), visited.length, visited[0].length)
                        && visited[localPoint.getSceneX()][localPoint.getSceneY()]) {
                    return true;
                }
            }
            return false;
        }

        if (worldPoint.getPlane() != wv.getPlane()) return false;
        int x = worldPoint.getX() - wv.getBaseX();
        int y = worldPoint.getY() - wv.getBaseY();
        return isWithinBounds(x, y, visited.length, visited[0].length) && visited[x][y];
    }

    /** Checks scene coordinates against the captured view's dimensions. */
    private static boolean isWithinBounds(int x, int y, int width, int height) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }


    /**
     * This method retrieves the tile at the specified coordinates (x, y) on the current plane.
     * It first creates a WorldPoint for the given coordinates and checks if the point is within
     * the scene using the `isInScene` method. If the WorldPoint is valid and within the scene,
     * it converts the WorldPoint to a LocalPoint, then retrieves and returns the corresponding
     * Tile from the game scene.
     * <p>
     * If the WorldPoint is out of bounds or the LocalPoint is null, the method returns null
     * to indicate that no valid tile is found at the given coordinates.
     *
     * @param x The x-coordinate of the tile.
     * @param y The y-coordinate of the tile.
     * @return The Tile at the specified coordinates, or null if the tile is invalid or not in the scene.
     */
    public Tile getTile(int x, int y) {
        WorldPoint worldPoint = new WorldPoint(x, y, ctxProvider.get().getClient().getTopLevelWorldView().getPlane());
        LocalPoint localPoint;

        if (ctxProvider.get().getClient().getTopLevelWorldView().getScene().isInstance()) {
            localPoint = fromWorldInstance(worldPoint);
        } else {
            localPoint = LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), worldPoint);
        }

        if (localPoint == null) return null;
        return ctxProvider.get().getClient().getTopLevelWorldView().getScene().getTiles()[worldPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
    }

    /**
     * Used to convert a WorldPoint in an instance to a LocalPoint
     * @param worldPoint The world point to convert
     * @return the first matching occurrence on the active scene plane, or null if absent
     */
    public LocalPoint fromWorldInstance(WorldPoint worldPoint) {
        if (worldPoint == null) return null;
        return ctxProvider.get().runOnClientThread(() -> {
            WorldView worldView = ctxProvider.get().getClient().getTopLevelWorldView();
            if (worldView == null) return null;
            for (WorldPoint instance : WorldPoint.toLocalInstance(worldView, worldPoint)) {
                if (instance.getPlane() == worldView.getPlane()) {
                    LocalPoint local = LocalPoint.fromWorld(worldView, instance);
                    if (local != null) return local;
                }
            }
            return null;
        });
    }

    /**
     * Gets the coordinate of the tile that contains the passed world point,
     * accounting for instances.
     *
     * @param worldPoint the instance worldpoint
     * @return the tile coordinate containing the local point
     */
    public WorldPoint fromInstance(WorldPoint worldPoint) {
        if (worldPoint == null) return null;
        return ctxProvider.get().runOnClientThread(() -> {
            WorldView worldView = ctxProvider.get().getClient().getTopLevelWorldView();
            if (worldView == null || !worldView.isInstance()) return worldPoint;
            LocalPoint localPoint = LocalPoint.fromWorld(worldView, worldPoint.getX(), worldPoint.getY());
            return localPoint == null ? worldPoint
                    : WorldPoint.fromLocalInstance(worldView.getScene(), localPoint, worldPoint.getPlane());
        });
    }

    /**
     * Converts a world point into a list of instanced world points
     * @param worldPoint World point to convert
     * @return all matching instance occurrences, or an empty list if the template is absent.
     */
    public ArrayList<WorldPoint> toInstance(WorldPoint worldPoint) {
        if (worldPoint == null) return new ArrayList<>();
        return ctxProvider.get().runOnClientThread(() -> {
            WorldView worldView = ctxProvider.get().getClient().getTopLevelWorldView();
            return worldView == null ? new ArrayList<>()
                    : new ArrayList<>(WorldPoint.toLocalInstance(worldView, worldPoint));
        });
    }

    /**
     * Returns the distance from a world point to another world point in local point distance.
     * @param distance Distance to convert
     * @return The distance in local points between a world point and another world point
     */
    public static Integer worldToLocalDistance(int distance) {
        return distance * Perspective.LOCAL_TILE_SIZE;
    }

    /**
     * Returns the distance from a local point to another local point in world point distance.
     * @param distance Distance to convert
     * @return The distance in world points between a local point and another local point
     */
    public static Integer localToWorldDistance(int distance) {
        return distance / Perspective.LOCAL_TILE_SIZE;
    }


    /**
     * Returns the Tile for a given {@link WorldPoint}.
     * @param point WorldPoint to get the tile for
     * @return The tile for a given WorldPoint.
     */
    public Tile getTile(WorldPoint point) {
        Client client = ctxProvider.get().getClient();
        WorldView worldView = client.getTopLevelWorldView();

        LocalPoint lp = LocalPoint.fromWorld(worldView, point.getX(), point.getY());
        Tile[][][] tiles = worldView.getScene().getTiles();

        if(lp == null || tiles == null) return null;

        try {
            return tiles[point.getPlane()][lp.getSceneX()][lp.getSceneY()];
        } catch (Exception e) {
            log.error("Failed to get tile for world point: x={}, y={}, z={}", point.getX(), point.getY(), point.getPlane(), e);
            return null;
        }
    }
}
