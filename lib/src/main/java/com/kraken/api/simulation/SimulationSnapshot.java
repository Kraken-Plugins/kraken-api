package com.kraken.api.simulation;

import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Immutable snapshot used as the input payload for simulation.
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
    private final SimulationPlayerSnapshot player;
    @Getter
    private final List<SimulationNpcSnapshot> npcs;

    /**
     * Creates a snapshot from collision, player, and npc input.
     *
     * @param gameTick client game tick at capture.
     * @param plane current plane.
     * @param baseX world base x for the collision array.
     * @param baseY world base y for the collision array.
     * @param collisionFlags scene collision map indexed as [sceneX][sceneY].
     * @param player player snapshot.
     * @param npcs npc position snapshots.
     */
    public SimulationSnapshot(
            int gameTick,
            int plane,
            int baseX,
            int baseY,
            int[][] collisionFlags,
            @NonNull SimulationPlayerSnapshot player,
            List<SimulationNpcSnapshot> npcs
    ) {
        if (collisionFlags == null || collisionFlags.length == 0 || collisionFlags[0] == null || collisionFlags[0].length == 0) {
            throw new IllegalArgumentException("collisionFlags must be non-empty");
        }

        int expectedHeight = collisionFlags[0].length;
        for (int[] row : collisionFlags) {
            if (row == null || row.length != expectedHeight) {
                throw new IllegalArgumentException("collisionFlags must be a rectangular matrix");
            }
        }

        this.gameTick = gameTick;
        this.plane = plane;
        this.baseX = baseX;
        this.baseY = baseY;
        this.collisionFlags = deepCopy(collisionFlags);
        this.player = player;
        this.npcs = npcs == null
                ? Collections.emptyList()
                : npcs.stream()
                .filter(npc -> npc != null)
                .sorted(Comparator.comparingInt(SimulationNpcSnapshot::getIndex))
                .collect(Collectors.toUnmodifiableList());
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
     * Checks whether scene coordinates are in the captured collision area.
     *
     * @param sceneX scene x coordinate.
     * @param sceneY scene y coordinate.
     * @return true when the tile is in bounds.
     */
    public boolean isSceneInBounds(int sceneX, int sceneY) {
        return sceneX >= 0 && sceneY >= 0 && sceneX < getSceneWidth() && sceneY < getSceneHeight();
    }

    /**
     * Checks whether a world tile is in the captured collision area.
     *
     * @param worldX world x coordinate.
     * @param worldY world y coordinate.
     * @return true when the tile is in bounds.
     */
    public boolean isWorldInBounds(int worldX, int worldY) {
        return isSceneInBounds(worldX - baseX, worldY - baseY);
    }

    /**
     * Reads collision flags by scene coordinate.
     *
     * @param sceneX scene x coordinate.
     * @param sceneY scene y coordinate.
     * @return collision flags, or 0 if out of bounds.
     */
    public int getCollisionFlagAtScene(int sceneX, int sceneY) {
        if (!isSceneInBounds(sceneX, sceneY)) {
            return 0;
        }
        return collisionFlags[sceneX][sceneY];
    }

    /**
     * Reads collision flags by world coordinate.
     *
     * @param worldX world x coordinate.
     * @param worldY world y coordinate.
     * @return collision flags, or 0 if out of bounds.
     */
    public int getCollisionFlagAtWorld(int worldX, int worldY) {
        return getCollisionFlagAtScene(worldX - baseX, worldY - baseY);
    }

    /**
     * @return deep copy of the collision map.
     */
    public int[][] copyCollisionFlags() {
        return deepCopy(collisionFlags);
    }

    /**
     * @return player world point from this snapshot.
     */
    public WorldPoint getPlayerWorldPoint() {
        return player.getWorldPoint();
    }

    /**
     * Creates a mutable root state using default npc profiles.
     *
     * @return root simulation state.
     */
    public SimulationState createState() {
        return SimulationState.fromScenario(new SimulationScenario(this, Collections.emptyMap()));
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
