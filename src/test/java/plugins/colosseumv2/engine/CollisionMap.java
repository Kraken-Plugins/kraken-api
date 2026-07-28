package plugins.colosseumv2.engine;

/**
 * Minimal scene-coordinate collision view used by the colosseum pathing and line of sight
 * calculations. The colosseum arena only contains full blockers (pillars, arena walls), so
 * directional wall flags are intentionally not modelled.
 */
public interface CollisionMap {

    /**
     * Returns whether the tile blocks NPC movement.
     *
     * @param sceneX Scene x coordinate.
     * @param sceneY Scene y coordinate.
     * @return True when an NPC cannot occupy the tile.
     */
    boolean isMovementBlocked(int sceneX, int sceneY);

    /**
     * Returns whether the tile blocks line of sight.
     *
     * @param sceneX Scene x coordinate.
     * @param sceneY Scene y coordinate.
     * @return True when sight lines crossing the tile are broken.
     */
    boolean isLosBlocked(int sceneX, int sceneY);
}
