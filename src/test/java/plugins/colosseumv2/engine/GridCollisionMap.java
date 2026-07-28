package plugins.colosseumv2.engine;

/**
 * Simple array-backed {@link CollisionMap}. Used by unit tests and as a per-tick snapshot of
 * client collision flags so the engine never touches client state directly.
 */
public class GridCollisionMap implements CollisionMap {

    private final boolean[][] movementBlocked;
    private final boolean[][] losBlocked;
    private final int width;
    private final int height;

    public GridCollisionMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.movementBlocked = new boolean[width][height];
        this.losBlocked = new boolean[width][height];
    }

    /**
     * Marks a rectangle of tiles as blocking both movement and line of sight (e.g. a pillar).
     *
     * @param sceneX Southwest x of the blocker.
     * @param sceneY Southwest y of the blocker.
     * @param sizeX  Width in tiles.
     * @param sizeY  Height in tiles.
     */
    public void block(int sceneX, int sceneY, int sizeX, int sizeY) {
        for (int x = sceneX; x < sceneX + sizeX; x++) {
            for (int y = sceneY; y < sceneY + sizeY; y++) {
                if (inBounds(x, y)) {
                    movementBlocked[x][y] = true;
                    losBlocked[x][y] = true;
                }
            }
        }
    }

    /**
     * Sets the blocking flags for a single tile.
     *
     * @param sceneX   Scene x coordinate.
     * @param sceneY   Scene y coordinate.
     * @param movement Whether the tile blocks movement.
     * @param los      Whether the tile blocks line of sight.
     */
    public void set(int sceneX, int sceneY, boolean movement, boolean los) {
        if (inBounds(sceneX, sceneY)) {
            movementBlocked[sceneX][sceneY] = movement;
            losBlocked[sceneX][sceneY] = los;
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    @Override
    public boolean isMovementBlocked(int sceneX, int sceneY) {
        return !inBounds(sceneX, sceneY) || movementBlocked[sceneX][sceneY];
    }

    @Override
    public boolean isLosBlocked(int sceneX, int sceneY) {
        return !inBounds(sceneX, sceneY) || losBlocked[sceneX][sceneY];
    }
}
