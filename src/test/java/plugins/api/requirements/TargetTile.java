package plugins.api.requirements;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * A destination tile the runner supplies on the player's behalf.
 *
 * <p>The movement, camera and pathfinder tests were written to poll for a tile the user had picked by
 * shift right clicking "Set", blocking for up to thirty seconds. That makes an unattended run
 * impossible. Declaring the tile instead lets the runner publish one before the test starts, so the
 * poll returns on its first iteration and the manual pathway still works when no suite is running.</p>
 */
@Value
public class TargetTile {

    /** How the tile is derived. */
    public enum Kind {
        /** An offset from wherever the player happens to be standing. */
        RELATIVE_TO_PLAYER,
        /** A fixed world coordinate. */
        ABSOLUTE
    }

    /** How to resolve this tile. */
    Kind kind;

    /** East west offset, used when {@link #kind} is {@link Kind#RELATIVE_TO_PLAYER}. */
    int deltaX;

    /** North south offset, used when {@link #kind} is {@link Kind#RELATIVE_TO_PLAYER}. */
    int deltaY;

    /** The fixed destination, used when {@link #kind} is {@link Kind#ABSOLUTE}. */
    WorldPoint absolute;

    /**
     * A tile offset from the player.
     *
     * <p>Prefer this over an absolute tile. The camera test converts its target with
     * {@code LocalPoint.fromWorld}, which only resolves for tiles inside the loaded scene, so a
     * coordinate on the far side of the map yields null and a spurious failure.</p>
     *
     * @param deltaX east west offset in tiles
     * @param deltaY north south offset in tiles
     * @return the target tile declaration
     */
    public static TargetTile relativeToPlayer(int deltaX, int deltaY) {
        return new TargetTile(Kind.RELATIVE_TO_PLAYER, deltaX, deltaY, null);
    }

    /**
     * A fixed world tile.
     *
     * @param point the destination
     * @return the target tile declaration
     */
    public static TargetTile absolute(WorldPoint point) {
        return new TargetTile(Kind.ABSOLUTE, 0, 0, point);
    }

    /**
     * Resolves this declaration against the player's current position.
     *
     * @param playerLocation where the player is standing; only needed for relative tiles
     * @return the destination tile, or null when a relative tile is requested without a player
     *         location
     */
    public WorldPoint resolve(WorldPoint playerLocation) {
        if (kind == Kind.ABSOLUTE) {
            return absolute;
        }

        if (playerLocation == null) {
            return null;
        }

        return playerLocation.dx(deltaX).dy(deltaY);
    }
}
