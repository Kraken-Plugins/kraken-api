package com.kraken.api.service.actor;

import com.kraken.api.Context;
import com.kraken.api.core.Services;
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

    private static final int MAX_ACTOR_PATH_STEPS = 256;
    private static Context ctx() {
        return Services.context();
    }
    private static TileService tileService() {
        return Services.get(TileService.class);
    }

    /**
     * Computes the movement path an NPC would take towards the local player using local collision data.
     * The returned list does not include the NPC's current tile.
     *
     * @param npc The source NPC.
     * @return The predicted step-by-step path toward the local player.
     */
    public static List<WorldPoint> getActorPath(NPC npc) {
        return ctx().runOnClientThread(() -> {
            Player localPlayer = ctx().getClient().getLocalPlayer();
            if (localPlayer == null) {
                return new ArrayList<>();
            }
            return getActorPathInternal(npc, localPlayer.getWorldLocation());
        });
    }

    /**
     * Computes the movement path an actor would take towards a target player using local collision data.
     * The returned list does not include the actor's current tile.
     *
     * @param actor  The source actor.
     * @param player The target player.
     * @return The predicted step-by-step path.
     */
    public static List<WorldPoint> getActorPath(Actor actor, Player player) {
        return ctx().runOnClientThread(() -> {
            if (player == null) {
                return new ArrayList<>();
            }
            return getActorPathInternal(actor, player.getWorldLocation());
        });
    }

    /**
     * Computes the movement path an actor would take towards a destination tile using local collision data.
     * The returned list does not include the actor's current tile.
     *
     * @param actor       The source actor.
     * @param destination The destination world tile.
     * @return The predicted step-by-step path.
     */
    public static List<WorldPoint> getActorPath(Actor actor, WorldPoint destination) {
        return ctx().runOnClientThread(() -> getActorPathInternal(actor, destination));
    }

    /**
     * Computes the movement path an NPC would take towards the local player and truncates it at the
     * first tile where the NPC gains line of sight to the player.
     *
     * @param npc The source NPC.
     * @return Path steps up to and including the first line-of-sight tile. Returns an empty list
     * when line of sight is already available from the NPC's current tile.
     */
    public static List<WorldPoint> getActorPathUntilLineOfSight(NPC npc) {
        return ctx().runOnClientThread(() -> {
            Player localPlayer = ctx().getClient().getLocalPlayer();
            if (localPlayer == null) {
                return new ArrayList<>();
            }
            return getActorPathUntilLineOfSightInternal(npc, localPlayer.getWorldLocation());
        });
    }

    /**
     * Computes the movement path an NPC would take towards a target player and truncates it at the
     * first tile where the NPC gains line of sight to that player.
     *
     * @param npc    The source NPC.
     * @param player The target player.
     * @return Path steps up to and including the first line-of-sight tile. Returns an empty list
     * when line of sight is already available from the NPC's current tile.
     */
    public static List<WorldPoint> getActorPathUntilLineOfSight(NPC npc, Player player) {
        return ctx().runOnClientThread(() -> {
            if (player == null) {
                return new ArrayList<>();
            }
            return getActorPathUntilLineOfSightInternal(npc, player.getWorldLocation());
        });
    }

    /**
     * Finds the tile where an NPC path would terminate once the NPC has line of sight to the local player.
     * If line of sight is already available, the NPC's current tile is returned.
     *
     * @param npc The source NPC.
     * @return The termination tile, or null when inputs are invalid.
     */
    public static WorldPoint getActorLineOfSightTerminationTile(NPC npc) {
        return ctx().runOnClientThread(() -> {
            Player localPlayer = ctx().getClient().getLocalPlayer();
            if (localPlayer == null) {
                return null;
            }
            return getActorLineOfSightTerminationTileInternal(npc, localPlayer.getWorldLocation());
        });
    }

    /**
     * Finds the tile where an NPC path would terminate once the NPC has line of sight to the target player.
     * If line of sight is already available, the NPC's current tile is returned.
     *
     * @param npc    The source NPC.
     * @param player The target player.
     * @return The termination tile, or null when inputs are invalid.
     */
    public static WorldPoint getActorLineOfSightTerminationTile(NPC npc, Player player) {
        return ctx().runOnClientThread(() -> {
            if (player == null) {
                return null;
            }
            return getActorLineOfSightTerminationTileInternal(npc, player.getWorldLocation());
        });
    }

    /**
     * Checks if there is a clear line of sight between two world points.
     *
     * @param source The starting {@link WorldPoint}.
     * @param other  The target {@link WorldPoint}.
     * @return True if there is an unobstructed line of sight, false otherwise.
     */
    public static boolean hasLineOfSightTo(WorldPoint source, WorldPoint other) {
        Tile sourceTile = tileService().getTile(source.getX(), source.getY());
        Tile otherTile = tileService().getTile(other.getX(), other.getY());

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
        return ctx().runOnClientThread(() -> {
            if(source == null || other == null) {
                return false;
            }

            if (source.getPlane() != other.getPlane()) {
                return false;
            }

            Client client = ctx().getClient();

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
        return ctx().runOnClientThread(() -> {
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
     * Internal path resolver that predicts movement from an actor's current tile toward a destination.
     * Validates plane/world-view/collision-map availability before pathing.
     *
     * @param actor The actor being simulated.
     * @param destination The destination tile.
     * @return A predicted path excluding the starting tile, or an empty list when invalid/unreachable.
     */
    private static List<WorldPoint> getActorPathInternal(Actor actor, WorldPoint destination) {
        if (actor == null || destination == null) {
            return new ArrayList<>();
        }

        WorldPoint start = actor.getWorldLocation();
        if (start == null || start.getPlane() != destination.getPlane()) {
            return new ArrayList<>();
        }

        Client client = ctx().getClient();
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null || worldView.getPlane() != start.getPlane()) {
            return new ArrayList<>();
        }

        CollisionData[] collisionData = worldView.getCollisionMaps();
        if (collisionData == null || worldView.getPlane() < 0 || worldView.getPlane() >= collisionData.length) {
            return new ArrayList<>();
        }

        int actorSize = getActorSize(actor);
        int[][] collisionDataFlags = collisionData[worldView.getPlane()].getFlags();
        return computeActorPath(start, destination, actorSize, worldView, collisionDataFlags);
    }

    /**
     * Internal LoS-aware path resolver. Computes full actor pathing and truncates at the first step
     * where the actor has line of sight to the target tile.
     *
     * @param actor The actor being simulated.
     * @param target The target tile that line of sight is checked against.
     * @return Steps up to the first LoS tile, or an empty list when LoS is already available/invalid.
     */
    private static List<WorldPoint> getActorPathUntilLineOfSightInternal(Actor actor, WorldPoint target) {
        if (actor == null || target == null) {
            return new ArrayList<>();
        }

        WorldPoint start = actor.getWorldLocation();
        if (start == null || start.getPlane() != target.getPlane()) {
            return new ArrayList<>();
        }

        Client client = ctx().getClient();
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null || worldView.getPlane() != start.getPlane()) {
            return new ArrayList<>();
        }

        CollisionData[] collisionData = worldView.getCollisionMaps();
        Tile[][][] tiles = worldView.getScene().getTiles();
        if (collisionData == null || tiles == null || worldView.getPlane() < 0 || worldView.getPlane() >= collisionData.length) {
            return new ArrayList<>();
        }

        int actorSize = getActorSize(actor);
        int[][] collisionDataFlags = collisionData[worldView.getPlane()].getFlags();
        List<WorldPoint> fullPath = computeActorPath(start, target, actorSize, worldView, collisionDataFlags);
        List<WorldPoint> losPath = new ArrayList<>();

        if (hasActorLineOfSightAt(start, actorSize, target, worldView, tiles, collisionDataFlags)) {
            return losPath;
        }

        for (WorldPoint step : fullPath) {
            losPath.add(step);
            if (hasActorLineOfSightAt(step, actorSize, target, worldView, tiles, collisionDataFlags)) {
                break;
            }
        }

        return losPath;
    }

    /**
     * Internal helper to resolve the tile where actor movement would stop once line of sight is obtained.
     *
     * @param actor The actor being simulated.
     * @param target The target tile used for LoS checks.
     * @return Current tile if LoS already exists, otherwise the first path tile with LoS, or null when invalid.
     */
    private static WorldPoint getActorLineOfSightTerminationTileInternal(Actor actor, WorldPoint target) {
        if (actor == null || target == null) {
            return null;
        }

        WorldPoint start = actor.getWorldLocation();
        if (start == null || start.getPlane() != target.getPlane()) {
            return null;
        }

        Client client = ctx().getClient();
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null || worldView.getPlane() != start.getPlane()) {
            return null;
        }

        CollisionData[] collisionData = worldView.getCollisionMaps();
        Tile[][][] tiles = worldView.getScene().getTiles();
        if (collisionData == null || tiles == null || worldView.getPlane() < 0 || worldView.getPlane() >= collisionData.length) {
            return null;
        }

        int actorSize = getActorSize(actor);
        int[][] collisionDataFlags = collisionData[worldView.getPlane()].getFlags();

        if (hasActorLineOfSightAt(start, actorSize, target, worldView, tiles, collisionDataFlags)) {
            return start;
        }

        List<WorldPoint> fullPath = computeActorPath(start, target, actorSize, worldView, collisionDataFlags);
        WorldPoint termination = start;
        for (WorldPoint step : fullPath) {
            termination = step;
            if (hasActorLineOfSightAt(step, actorSize, target, worldView, tiles, collisionDataFlags)) {
                break;
            }
        }
        return termination;
    }

    /**
     * Computes greedy step-by-step movement toward a destination using collision checks and cardinal
     * fallbacks when diagonal movement is blocked.
     *
     * @param start Actor origin tile (south-west for multi-tile NPCs).
     * @param destination Target tile.
     * @param actorSize Actor footprint size.
     * @param worldView Active world view used for base coordinates.
     * @param collisionDataFlags Plane collision flags.
     * @return Path excluding the start tile; may be partial when blocked.
     */
    private static List<WorldPoint> computeActorPath(
            WorldPoint start,
            WorldPoint destination,
            int actorSize,
            WorldView worldView,
            int[][] collisionDataFlags
    ) {
        List<WorldPoint> path = new ArrayList<>();
        if (start == null || destination == null || start.getPlane() != destination.getPlane()) {
            return path;
        }

        int curX = start.getX();
        int curY = start.getY();
        int destX = destination.getX();
        int destY = destination.getY();
        int plane = start.getPlane();
        int steps = 0;

        while ((curX != destX || curY != destY) && steps < MAX_ACTOR_PATH_STEPS) {
            int difX = destX - curX;
            int difY = destY - curY;
            int dx = Integer.signum(difX);
            int dy = Integer.signum(difY);

            if (canActorStep(worldView, collisionDataFlags, curX, curY, plane, dx, dy, actorSize)) {
                curX += dx;
                curY += dy;
            } else if (dx != 0 && canActorStep(worldView, collisionDataFlags, curX, curY, plane, dx, 0, actorSize)) {
                curX += dx;
            } else if (dy != 0 && canActorStep(worldView, collisionDataFlags, curX, curY, plane, 0, dy, actorSize)) {
                curY += dy;
            } else {
                break;
            }

            path.add(new WorldPoint(curX, curY, plane));
            steps++;
        }

        return path;
    }

    /**
     * Returns whether an actor footprint can move one step in the specified direction.
     * Every occupied tile in the footprint must pass movement checks.
     *
     * @param worldView Active world view.
     * @param collisionDataFlags Plane collision flags.
     * @param currX Current world x (south-west anchor).
     * @param currY Current world y (south-west anchor).
     * @param plane Current plane.
     * @param dx Step delta x.
     * @param dy Step delta y.
     * @param actorSize Actor footprint size.
     * @return True when the step is valid for the full footprint.
     */
    private static boolean canActorStep(
            WorldView worldView,
            int[][] collisionDataFlags,
            int currX,
            int currY,
            int plane,
            int dx,
            int dy,
            int actorSize
    ) {
        if (dx == 0 && dy == 0) {
            return true;
        }

        for (int x = 0; x < actorSize; x++) {
            for (int y = 0; y < actorSize; y++) {
                int tileX = currX + x;
                int tileY = currY + y;
                if (plane != worldView.getPlane()) {
                    return false;
                }

                int sceneX = tileX - worldView.getBaseX();
                int sceneY = tileY - worldView.getBaseY();
                if (!isWalkable(collisionDataFlags, sceneX, sceneY, dx, dy)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Performs single-tile movement validation against RuneLite collision flags.
     * Supports cardinal and diagonal movement, including side-tile checks for diagonals.
     *
     * @param flags Plane collision flags.
     * @param sceneX Source scene x.
     * @param sceneY Source scene y.
     * @param dx Step delta x.
     * @param dy Step delta y.
     * @return True when the movement is allowed by collision data.
     */
    private static boolean isWalkable(int[][] flags, int sceneX, int sceneY, int dx, int dy) {
        if (!isInScene(flags, sceneX, sceneY)) {
            return false;
        }

        int targetX = sceneX + dx;
        int targetY = sceneY + dy;
        if (!isInScene(flags, targetX, targetY)) {
            return false;
        }

        int sourceFlags = flags[sceneX][sceneY];
        int targetFlags = flags[targetX][targetY];

        if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0) {
            return false;
        }

        if (dx == 0 && dy == 0) {
            return true;
        }

        if (dx == 0 || dy == 0) {
            if (dx > 0) {
                return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                        && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
            }
            if (dx < 0) {
                return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                        && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
            }
            if (dy > 0) {
                return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0
                        && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
            }
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
        }

        int xFlags = flags[sceneX + dx][sceneY];
        int yFlags = flags[sceneX][sceneY + dy];

        if (dx > 0 && dy > 0) { // NE
            if ((sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0
                    || (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) {
                return false;
            }
            if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0
                    || (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) {
                return false;
            }
            return (xFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0
                    && (yFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
        }
        if (dx < 0 && dy > 0) { // NW
            if ((sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0
                    || (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) {
                return false;
            }
            if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0
                    || (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) {
                return false;
            }
            return (xFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0
                    && (yFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
        }
        if (dx > 0 && dy < 0) { // SE
            if ((sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0
                    || (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) {
                return false;
            }
            if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0
                    || (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) {
                return false;
            }
            return (xFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0
                    && (yFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
        }
        if (dx < 0 && dy < 0) { // SW
            if ((sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0
                    || (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) {
                return false;
            }
            if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0
                    || (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) {
                return false;
            }
            return (xFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0
                    && (yFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
        }

        return false;
    }

    /**
     * Bounds-check utility for scene-indexed collision arrays.
     *
     * @param flags Plane collision flags.
     * @param sceneX Scene x index.
     * @param sceneY Scene y index.
     * @return True when the indices are inside the loaded collision grid.
     */
    private static boolean isInScene(int[][] flags, int sceneX, int sceneY) {
        return sceneX >= 0
                && sceneY >= 0
                && sceneX < flags.length
                && sceneY < flags[sceneX].length;
    }

    /**
     * Checks whether an actor at a given south-west anchor has line of sight to a target tile.
     * For multi-tile actors, LoS is tested from the footprint tile closest to the target.
     *
     * @param actorSouthWest Actor south-west anchor tile.
     * @param actorSize Actor footprint size.
     * @param target LoS target tile.
     * @param worldView Active world view.
     * @param tiles Scene tile grid.
     * @param collisionDataFlags Plane collision flags.
     * @return True when a valid source/target tile pair has unobstructed LoS.
     */
    private static boolean hasActorLineOfSightAt(
            WorldPoint actorSouthWest,
            int actorSize,
            WorldPoint target,
            WorldView worldView,
            Tile[][][] tiles,
            int[][] collisionDataFlags
    ) {
        WorldPoint source = getClosestNpcTileToTarget(actorSouthWest, actorSize, target);
        Tile sourceTile = getTileAtWorldPoint(worldView, tiles, source);
        Tile targetTile = getTileAtWorldPoint(worldView, tiles, target);
        if (sourceTile == null || targetTile == null) {
            return false;
        }

        return hasLineOfSightToInternal(sourceTile, targetTile, collisionDataFlags);
    }

    /**
     * Resolves actor footprint size in tiles.
     * NPC size is read from composition; non-NPC actors default to size 1.
     *
     * @param actor Actor to inspect.
     * @return Footprint size in tiles.
     */
    private static int getActorSize(Actor actor) {
        if (actor instanceof NPC) {
            NPCComposition composition = ((NPC) actor).getComposition();
            if (composition != null && composition.getSize() > 0) {
                return composition.getSize();
            }
        }
        return 1;
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

        Client client = ctx().getClient();
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
