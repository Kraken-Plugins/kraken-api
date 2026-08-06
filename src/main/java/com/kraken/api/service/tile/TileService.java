package com.kraken.api.service.tile;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.player.LocalPlayerEntity;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

import static net.runelite.api.Constants.CHUNK_SIZE;
import static net.runelite.api.Perspective.SCENE_SIZE;

@Slf4j
@Singleton
public class TileService {

    private static final int FLAG_DATA_SIZE = 104;

    // The player-reachability flood depends only on the player's tile and the plane's collision
    // flags, neither of which changes within a game tick, so it is computed at most once per tick.
    // Without this, reachable() over N entities ran one full 104x104 BFS per entity in a single frame.
    private int reachabilityMatrixTick = -1;
    private boolean[][] cachedReachabilityMatrix;

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
     * Checks if a GameObject is reachable.
     * This considers the object's size and checks if the player can reach
     * any tile touching the object's boundary (the "Interactable Halo").
     * @param obj The game object to determine reachability for
     * @return true if the game object is reachable and false otherwise
     */
    public boolean isObjectReachable(GameObject obj) {
        if (obj == null) return false;

        // 1. Get the boundary of the object in Scene Coordinates
        // We use Scene Coordinates (0-103) because that matches the CollisionData flags.
        LocalPoint lp = obj.getLocalLocation(); // Center of object
        if (lp == null) return false;

        Client client = ctxProvider.get().getClient();
        int sceneX = lp.getSceneX();
        int sceneY = lp.getSceneY();

        // Object composition gives us width/height (for 1x1, 2x2 objects etc)
        ObjectComposition comp = getObjectComposition(obj);
        int sizeX = 1;
        int sizeY = 1;

        if (comp != null) {
            // Adjust for rotation if necessary (swaps width/height)
            if (obj.getOrientation() == 1 || obj.getOrientation() == 3) {
                sizeX = comp.getSizeY();
                sizeY = comp.getSizeX();
            } else {
                sizeX = comp.getSizeX();
                sizeY = comp.getSizeY();
            }
        }

        // Calculate the bottom-left corner of the object in Scene coords
        // LocalPoint is center, so we shift back to corner
        int minX = sceneX - (sizeX - 1) / 2;
        int minY = sceneY - (sizeY - 1) / 2;
        int maxX = minX + sizeX - 1;
        int maxY = minY + sizeY - 1;

        // 2. Run the BFS to find all reachable tiles from player
        boolean[][] visited = getReachableTilesMatrix();
        if (visited == null) return false;

        // 3. Check if any tile occupying the object OR adjacent to the object is reachable
        // We search from minX-1 to maxX+1 to cover the "halo" around the object.
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int y = minY - 1; y <= maxY + 1; y++) {
                if (x >= 0 && y >= 0 && x < SCENE_SIZE && y < SCENE_SIZE) {
                    if (visited[x][y]) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Standard BFS to map all reachable tiles from the current player position.
     * @return A 104x104 boolean array where true = walkable from player, or null if client state is unavailable.
     */
    private boolean[][] getReachableTilesMatrix() {
        Client client = ctxProvider.get().getClient();
        int tick = client.getTickCount();
        if (tick == reachabilityMatrixTick && cachedReachabilityMatrix != null) {
            return cachedReachabilityMatrix;
        }

        Player localPlayer = ctxProvider.get().runOnClientThread(client::getLocalPlayer);
        if (localPlayer == null) return null;

        WorldView wv = client.getTopLevelWorldView();
        if (wv == null) return null;

        LocalPoint playerLp = localPlayer.getLocalLocation();
        if (playerLp == null) return null;

        CollisionData[] collisionData = wv.getCollisionMaps();
        if (collisionData == null) return null;
        int[][] flags = collisionData[wv.getPlane()].getFlags();

        boolean[][] matrix = floodReachableTiles(playerLp.getSceneX(), playerLp.getSceneY(), flags);
        cachedReachabilityMatrix = matrix;
        reachabilityMatrixTick = tick;
        return matrix;
    }

    /**
     * Flood-fills the reachable tiles from a scene-coordinate start using a 4-cardinal BFS over the
     * collision flags. A neighbour is entered only when the current tile permits leaving in that
     * direction and the neighbour is not fully blocked.
     *
     * @param startX The start tile's scene x-coordinate (0-103).
     * @param startY The start tile's scene y-coordinate (0-103).
     * @param flags  The current plane's collision flags.
     * @return A {@code FLAG_DATA_SIZE}×{@code FLAG_DATA_SIZE} matrix where true marks a reachable tile.
     */
    private boolean[][] floodReachableTiles(int startX, int startY, int[][] flags) {
        boolean[][] visited = new boolean[FLAG_DATA_SIZE][FLAG_DATA_SIZE];
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        queue.add((startX << 16) | startY);
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int point = queue.poll();
            int x = point >> 16;
            int y = point & 0xFFFF;

            checkNeighbour(queue, visited, flags, x, y, -1, 0, CollisionDataFlag.BLOCK_MOVEMENT_WEST);
            checkNeighbour(queue, visited, flags, x, y, 1, 0, CollisionDataFlag.BLOCK_MOVEMENT_EAST);
            checkNeighbour(queue, visited, flags, x, y, 0, -1, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH);
            checkNeighbour(queue, visited, flags, x, y, 0, 1, CollisionDataFlag.BLOCK_MOVEMENT_NORTH);
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
     * @param blockMovementFlag The flag on the current tile that blocks travel toward the neighbour.
     */
    private void checkNeighbour(ArrayDeque<Integer> queue, boolean[][] visited, int[][] flags, int x, int y, int dx, int dy, int blockMovementFlag) {
        int nx = x + dx;
        int ny = y + dy;

        if (isWithinBounds(nx, ny) && !visited[nx][ny]
                && (flags[x][y] & blockMovementFlag) == 0
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

        LocalPlayerEntity player = ctxProvider.get().players().local();
        final WorldPoint playerLoc = ctxProvider.get().runOnClientThread(() -> player.raw().getWorldLocation());
        if (playerLoc == null) return false;

        if (targetPoint.getPlane() != playerLoc.getPlane()) return false;

        // Shares the tick-cached player-reachability flood with isObjectReachable; the flood always
        // starts from the player's scene tile, and isVisited handles the target-side (instanced)
        // coordinate conversion.
        boolean[][] visited = getReachableTilesMatrix();
        if (visited == null) return false;

        return isVisited(targetPoint, visited);
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
     * @param worldPoint The WorldPoint representing the tile to check for visit status.
     * @param visited A 2D boolean array tracking visited tiles during world traversal.
     * @return True if the tile has been visited and is within bounds, otherwise false.
     */
    private boolean isVisited(WorldPoint worldPoint, boolean[][] visited) {
        WorldView wv = ctxProvider.get().getClient().getTopLevelWorldView();
        if (wv.getScene().isInstance()) {
            // In an instance the target world point maps to one or more instanced scene positions;
            // the tile is reachable if any of them was visited. Convert the target itself here — not
            // the player's location — otherwise every target reads as reachable.
            for (WorldPoint instancePoint : WorldPoint.toLocalInstance(wv, worldPoint)) {
                LocalPoint localPoint = LocalPoint.fromWorld(wv, instancePoint);
                if (localPoint != null && isWithinBounds(localPoint.getSceneX(), localPoint.getSceneY())
                        && visited[localPoint.getSceneX()][localPoint.getSceneY()]) {
                    return true;
                }
            }
            return false;
        }

        int x = worldPoint.getX() - wv.getBaseX();
        int y = worldPoint.getY() - wv.getBaseY();
        return isWithinBounds(x, y) && visited[x][y];
    }

    /**
     * This method checks if the given coordinates (x, y) are within the valid bounds
     * of the game world grid. It ensures that the coordinates are non-negative and
     * within the range of the grid dimensions (0 to 103 for both x and y).
     * <p>
     * The method is used to prevent out-of-bounds errors when accessing world tiles
     * by ensuring that the coordinates provided for the tile are within the valid
     * range before performing further operations.
     *
     * @param x The x-coordinate of the tile to check.
     * @param y The y-coordinate of the tile to check.
     * @return True if the coordinates are within bounds (0 <= x, y < {@code FLAG_DATA_SIZE}), otherwise false.
     */
    private static boolean isWithinBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < FLAG_DATA_SIZE && y < FLAG_DATA_SIZE;
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
     * @return A local point representing the same global world point
     */
    public LocalPoint fromWorldInstance(WorldPoint worldPoint) {
        int[][][] instanceTemplateChunks = ctxProvider.get().getClient().getTopLevelWorldView().getInstanceTemplateChunks();
        // Extract the coordinates from the WorldPoint
        int worldX = worldPoint.getX();
        int worldY = worldPoint.getY();
        int worldPlane = ctxProvider.get().getClient().getTopLevelWorldView().getPlane();

        // Loop through all chunks to find which one contains the world point
        for (int chunkX = 0; chunkX < instanceTemplateChunks[worldPlane].length; chunkX++) {
            for (int chunkY = 0; chunkY < instanceTemplateChunks[worldPlane][chunkX].length; chunkY++) {
                // Get the template chunk at this chunk position
                int templateChunk = instanceTemplateChunks[worldPlane][chunkX][chunkY];

                // Extract rotation, template chunk coordinates, and plane
                int rotation = (templateChunk >> 1) & 0x3;
                int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
                int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;
                int templateChunkPlane = (templateChunk >> 24) & 0x3;

                // Check if the WorldPoint matches this chunk (after reversing rotation)
                WorldPoint rotatedWorldPoint = rotate(new WorldPoint(worldX, worldY, templateChunkPlane), rotation);

                if (rotatedWorldPoint.getX() >= templateChunkX && rotatedWorldPoint.getX() < templateChunkX + CHUNK_SIZE
                        && rotatedWorldPoint.getY() >= templateChunkY && rotatedWorldPoint.getY() < templateChunkY + CHUNK_SIZE) {
                    // Calculate local coordinates within the scene
                    int localX = (rotatedWorldPoint.getX() - templateChunkX) + (chunkX * CHUNK_SIZE);
                    int localY = (rotatedWorldPoint.getY() - templateChunkY) + (chunkY * CHUNK_SIZE);

                    // Return the corresponding LocalPoint
                    return  LocalPoint.fromScene(localX, localY, ctxProvider.get().getClient().getTopLevelWorldView());
                }
            }
        }
        return null;
    }

    /**
     * Gets the coordinate of the tile that contains the passed world point,
     * accounting for instances.
     *
     * @param worldPoint the instance worldpoint
     * @return the tile coordinate containing the local point
     */
    public WorldPoint fromInstance(WorldPoint worldPoint) {
        LocalPoint localPoint = LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), worldPoint);

        if(localPoint == null || !ctxProvider.get().getClient().getTopLevelWorldView().isInstance())
            return worldPoint;

        int sceneX = localPoint.getSceneX();
        int sceneY = localPoint.getSceneY();

        int chunkX = sceneX / CHUNK_SIZE;
        int chunkY = sceneY / CHUNK_SIZE;

        // get the template chunk for the chunk
        int[][][] instanceTemplateChunks = ctxProvider.get().getClient().getTopLevelWorldView().getInstanceTemplateChunks();
        int templateChunk = instanceTemplateChunks[worldPoint.getPlane()][chunkX][chunkY];

        int rotation = templateChunk >> 1 & 0x3;
        int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
        int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;
        int templateChunkPlane = templateChunk >> 24 & 0x3;

        // calculate world point of the template
        int x = templateChunkX + (sceneX & (CHUNK_SIZE - 1));
        int y = templateChunkY + (sceneY & (CHUNK_SIZE - 1));

        // create and rotate point back to 0, to match with template
        return rotate(new WorldPoint(x, y, templateChunkPlane), 4 - rotation);
    }

    /**
     * Converts a world point into a list of instanced world points
     * @param worldPoint World point to convert
     * @return List of instanced world points.
     */
    public ArrayList<WorldPoint> toInstance(WorldPoint worldPoint) {
        // if not in an instanced region, return the world point as is
        if (!ctxProvider.get().getClient().getTopLevelWorldView().isInstance()) {
            return new ArrayList<>(Collections.singletonList(worldPoint));
        }

        // find instance chunks using the template point. there might be more than one.
        ArrayList<WorldPoint> worldPoints = new ArrayList<>();
        int[][][] instanceTemplateChunks = ctxProvider.get().getClient().getTopLevelWorldView().getInstanceTemplateChunks();
        for (int z = 0; z < instanceTemplateChunks.length; z++) {
            for (int x = 0; x < instanceTemplateChunks[z].length; ++x) {
                for (int y = 0; y < instanceTemplateChunks[z][x].length; ++y) {
                    int chunkData = instanceTemplateChunks[z][x][y];
                    int rotation = chunkData >> 1 & 0x3;
                    int templateChunkY = (chunkData >> 3 & 0x7FF) * CHUNK_SIZE;
                    int templateChunkX = (chunkData >> 14 & 0x3FF) * CHUNK_SIZE;
                    int plane = chunkData >> 24 & 0x3;
                    if (worldPoint.getX() >= templateChunkX && worldPoint.getX() < templateChunkX + CHUNK_SIZE
                            && worldPoint.getY() >= templateChunkY && worldPoint.getY() < templateChunkY + CHUNK_SIZE
                            && plane == worldPoint.getPlane())
                    {
                        WorldPoint p = new WorldPoint(ctxProvider.get().getClient().getTopLevelWorldView().getBaseX() + x * CHUNK_SIZE + (worldPoint.getX() & (CHUNK_SIZE - 1)),
                                ctxProvider.get().getClient().getTopLevelWorldView().getBaseY() + y * CHUNK_SIZE + (worldPoint.getY() & (CHUNK_SIZE - 1)),
                                z);
                        p = rotate(p, rotation);
                        worldPoints.add(p);
                    }
                }
            }
        }
        if(worldPoints.isEmpty())
            worldPoints.add(worldPoint);
        return worldPoints;
    }

    /**
     * Rotate the coordinates in the chunk according to chunk rotation
     *
     * @param point    point
     * @param rotation rotation
     * @return world point
     */
    private WorldPoint rotate(WorldPoint point, int rotation) {
        int chunkX = point.getX() & -CHUNK_SIZE;
        int chunkY = point.getY() & -CHUNK_SIZE;
        int x = point.getX() & (CHUNK_SIZE - 1);
        int y = point.getY() & (CHUNK_SIZE - 1);
        switch (rotation)
        {
            case 1:
                return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
            case 2:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
            case 3:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
        }
        return point;
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
