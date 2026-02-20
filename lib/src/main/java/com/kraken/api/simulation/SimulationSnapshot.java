package com.kraken.api.simulation;

import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.List;

/**
 * Immutable RuneLite-compatible snapshot used as the root input for fast simulations.
 */
public final class SimulationSnapshot {
    @Getter
    private final int gameTick;
    @Getter
    private final int plane;
    @Getter
    private final int baseX;
    @Getter
    private final int baseY;
    private final int[][] collisionFlags;
    @Getter
    private final WorldPoint playerWorldPoint;
    @Getter
    private final SimulationPlayerSnapshot player;
    @Getter
    private final List<SimulationNpcSnapshot> npcs;

    /**
     * Creates an immutable snapshot used to seed one or more simulation states.
     *
     * @param gameTick RuneLite client tick at capture time.
     * @param plane active scene plane.
     * @param baseX world-view base x.
     * @param baseY world-view base y.
     * @param collisionFlags copied scene collision flags indexed by [sceneX][sceneY].
     * @param playerWorldPoint local player world position at capture time.
     * @param npcs captured NPC snapshots.
     */
    public SimulationSnapshot(
            int gameTick,
            int plane,
            int baseX,
            int baseY,
            int[][] collisionFlags,
            @NonNull WorldPoint playerWorldPoint,
            List<SimulationNpcSnapshot> npcs
    ) {
        this(
                gameTick,
                plane,
                baseX,
                baseY,
                collisionFlags,
                playerWorldPoint,
                null,
                npcs
        );
    }

    /**
     * Creates an immutable snapshot used to seed one or more simulation states.
     *
     * @param gameTick RuneLite client tick at capture time.
     * @param plane active scene plane.
     * @param baseX world-view base x.
     * @param baseY world-view base y.
     * @param collisionFlags copied scene collision flags indexed by [sceneX][sceneY].
     * @param playerWorldPoint local player world position at capture time.
     * @param player captured player combat/action metadata.
     * @param npcs captured NPC snapshots.
     */
    public SimulationSnapshot(
            int gameTick,
            int plane,
            int baseX,
            int baseY,
            int[][] collisionFlags,
            @NonNull WorldPoint playerWorldPoint,
            SimulationPlayerSnapshot player,
            List<SimulationNpcSnapshot> npcs
    ) {
        if (collisionFlags == null || collisionFlags.length == 0 || collisionFlags[0] == null || collisionFlags[0].length == 0) {
            throw new IllegalArgumentException("collisionFlags must be non-empty");
        }

        int expectedHeight = collisionFlags[0].length;
        for (int[] collisionFlag : collisionFlags) {
            if (collisionFlag == null || collisionFlag.length != expectedHeight) {
                throw new IllegalArgumentException("collisionFlags must be a rectangular matrix");
            }
        }

        this.gameTick = gameTick;
        this.plane = plane;
        this.baseX = baseX;
        this.baseY = baseY;
        this.collisionFlags = deepCopy(collisionFlags);
        this.playerWorldPoint = playerWorldPoint;
        this.player = player == null ? SimulationPlayerSnapshot.empty() : player;
        this.npcs = npcs == null
                ? Collections.emptyList()
                : List.copyOf(npcs);
    }

    /**
     * @return scene width in tiles.
     */
    public int getSceneWidth() {
        return collisionFlags.length;
    }

    /**
     * @return scene height in tiles.
     */
    public int getSceneHeight() {
        return collisionFlags[0].length;
    }

    /**
     * Checks whether the provided scene coordinates are inside the captured collision grid.
     *
     * @param sceneX scene x coordinate.
     * @param sceneY scene y coordinate.
     * @return true when in bounds.
     */
    public boolean isSceneInBounds(int sceneX, int sceneY) {
        return sceneX >= 0 && sceneY >= 0 && sceneX < getSceneWidth() && sceneY < getSceneHeight();
    }

    /**
     * Checks whether a world tile is inside the captured scene bounds.
     *
     * @param worldX world x coordinate.
     * @param worldY world y coordinate.
     * @return true when in bounds.
     */
    public boolean isWorldInBounds(int worldX, int worldY) {
        return isSceneInBounds(worldX - baseX, worldY - baseY);
    }

    /**
     * Reads a collision flag by scene coordinate.
     *
     * @param sceneX scene x coordinate.
     * @param sceneY scene y coordinate.
     * @return collision flags, or {@code 0} when out of bounds.
     */
    public int getCollisionFlagAtScene(int sceneX, int sceneY) {
        if (!isSceneInBounds(sceneX, sceneY)) {
            return 0;
        }
        return collisionFlags[sceneX][sceneY];
    }

    /**
     * Reads a collision flag by world coordinate.
     *
     * @param worldX world x coordinate.
     * @param worldY world y coordinate.
     * @return collision flags, or {@code 0} when out of bounds.
     */
    public int getCollisionFlagAtWorld(int worldX, int worldY) {
        return getCollisionFlagAtScene(worldX - baseX, worldY - baseY);
    }

    /**
     * @return deep copy of the captured collision flags.
     */
    public int[][] copyCollisionFlags() {
        return deepCopy(collisionFlags);
    }

    /**
     * Creates a mutable simulation state initialized from this snapshot.
     *
     * @return simulation state rooted at this snapshot.
     */
    public SimulationState createState() {
        return SimulationState.fromSnapshot(this);
    }

    int[][] collisionFlagsUnsafe() {
        return collisionFlags;
    }

    private static int[][] deepCopy(int[][] source) {
        int[][] copied = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copied[i] = source[i].clone();
        }
        return copied;
    }
}
