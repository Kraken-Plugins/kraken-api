package com.kraken.api.simulation.live;

import com.kraken.api.simulation.geom.Grid;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;

/**
 * Packs the client's collision flags into the simulation's bitboards, once per encounter. Tiles never observed
 * (outside the scene or the window) stay blocked, which is the safe default. Movement and sight blockers are captured
 * separately from the full tile object flags; thin walls are not captured. The window is the anchor's one region, so
 * an arena that crosses a region boundary is cut off at it, and only the top level world view's current plane is read.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollisionCapture {
    private static final int MOVE_MASK = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
    private static final int LOS_MASK = CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL;

    /** Maps a scene tile to the packed region local position it lies on. */
    @FunctionalInterface
    public interface SceneToLocal {
        /**
         * The packed position of a scene tile.
         * @param sceneX the scene x
         * @param sceneY the scene y
         * @return the packed position, or -1 outside the window
         */
        int local(int sceneX, int sceneY);
    }

    /**
     * Builds the static grid from the current scene's plane.
     * @param client the client
     * @param anchor the anchored window
     * @return the grid, or null when the scene or its collision maps are unavailable
     */
    public static Grid capture(Client client, RegionAnchor anchor) {
        WorldView wv = client.getTopLevelWorldView();
        if (wv == null) {
            return null;
        }
        CollisionData[] maps = wv.getCollisionMaps();
        if (maps == null) {
            return null;
        }
        CollisionData collision = maps[wv.getPlane()];
        if (collision == null) {
            return null;
        }
        return fromFlags(collision.getFlags(), wv.getSizeX(), wv.getSizeY(), (sx, sy) -> {
            WorldPoint wp = WorldPoint.fromLocalInstance(client, LocalPoint.fromScene(sx, sy, wv));
            return wp == null ? -1 : anchor.local(wp, 0);
        });
    }

    /**
     * Builds a grid from scene collision flags: every tile starts blocked and each scene tile inside the window opens
     * unless its flags block movement (or sight, for the sight board).
     * @param flags the scene's collision flags, {@code flags[sceneX][sceneY]}
     * @param sizeX the scene's width
     * @param sizeY the scene's height
     * @param toLocal maps scene tiles into the window
     * @return the finished grid
     */
    public static Grid fromFlags(int[][] flags, int sizeX, int sizeY, SceneToLocal toLocal) {
        Grid grid = new Grid();
        Arrays.fill(grid.blockMove, ~0L);
        Arrays.fill(grid.blockLos, ~0L);
        for (int sx = 0; sx < sizeX; sx++) {
            for (int sy = 0; sy < sizeY; sy++) {
                int local = toLocal.local(sx, sy);
                if (local < 0) {
                    continue;
                }
                int f = flags[sx][sy];
                int x = local & 0x3f;
                int y = local >> 6;
                if ((f & MOVE_MASK) == 0) {
                    Grid.clear(grid.blockMove, x, y);
                }
                if ((f & LOS_MASK) == 0) {
                    Grid.clear(grid.blockLos, x, y);
                }
            }
        }
        grid.finish();
        return grid;
    }
}
