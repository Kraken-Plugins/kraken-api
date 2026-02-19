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
    private final List<SimulationNpcSnapshot> npcs;

    public SimulationSnapshot(
            int gameTick,
            int plane,
            int baseX,
            int baseY,
            int[][] collisionFlags,
            @NonNull WorldPoint playerWorldPoint,
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
        this.npcs = npcs == null
                ? Collections.emptyList()
                : List.copyOf(npcs);
    }

    public int getSceneWidth() {
        return collisionFlags.length;
    }

    public int getSceneHeight() {
        return collisionFlags[0].length;
    }

    public boolean isSceneInBounds(int sceneX, int sceneY) {
        return sceneX >= 0 && sceneY >= 0 && sceneX < getSceneWidth() && sceneY < getSceneHeight();
    }

    public boolean isWorldInBounds(int worldX, int worldY) {
        return isSceneInBounds(worldX - baseX, worldY - baseY);
    }

    public int getCollisionFlagAtScene(int sceneX, int sceneY) {
        if (!isSceneInBounds(sceneX, sceneY)) {
            return 0;
        }
        return collisionFlags[sceneX][sceneY];
    }

    public int getCollisionFlagAtWorld(int worldX, int worldY) {
        return getCollisionFlagAtScene(worldX - baseX, worldY - baseY);
    }

    public int[][] copyCollisionFlags() {
        return deepCopy(collisionFlags);
    }

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
