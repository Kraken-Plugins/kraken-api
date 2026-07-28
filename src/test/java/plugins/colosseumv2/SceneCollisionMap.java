package plugins.colosseumv2;

import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.WorldView;
import plugins.colosseumv2.engine.CollisionMap;

/**
 * {@link CollisionMap} backed by the client's collision flags for the current plane. Built
 * once per game tick on the client thread; the flags array reference is only read during
 * that tick's engine pass.
 */
public class SceneCollisionMap implements CollisionMap {

    private final int[][] flags;

    private SceneCollisionMap(int[][] flags) {
        this.flags = flags;
    }

    /**
     * Captures the current plane's collision flags.
     *
     * @param client The game client, must be called on the client thread.
     * @return A collision map, or {@code null} when collision data is unavailable.
     */
    public static SceneCollisionMap capture(Client client) {
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null) {
            return null;
        }

        CollisionData[] collisionData = worldView.getCollisionMaps();
        int plane = worldView.getPlane();
        if (collisionData == null || plane < 0 || plane >= collisionData.length || collisionData[plane] == null) {
            return null;
        }

        return new SceneCollisionMap(collisionData[plane].getFlags());
    }

    private boolean inBounds(int x, int y) {
        return flags != null
                && x >= 0
                && x < flags.length
                && y >= 0
                && flags[x] != null
                && y < flags[x].length;
    }

    @Override
    public boolean isMovementBlocked(int sceneX, int sceneY) {
        if (!inBounds(sceneX, sceneY)) {
            return true;
        }
        return (flags[sceneX][sceneY] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0;
    }

    @Override
    public boolean isLosBlocked(int sceneX, int sceneY) {
        if (!inBounds(sceneX, sceneY)) {
            return true;
        }
        return (flags[sceneX][sceneY] & CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL) != 0;
    }
}
