package com.kraken.api.service.walker;

import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.HomeTeleports;
import net.runelite.api.coords.WorldPoint;

import java.time.Instant;

/**
 * Decides when a Lumbridge home teleport is a better first step than walking from here.
 *
 * <p>The shortest-path dataset includes the spell, but walking is cheaper than its cooldown, so the
 * planner never picks it. The walker injects it when the player is on the standard book, the
 * thirty-minute timer is clear, they are more than fifty tiles from the courtyard, and the dense
 * path from the courtyard is shorter than the walk from here. Farm to the castle bank teleports;
 * farm to the Grand Exchange does not. Musa Point to the Karamja dungeon does not either: 2D
 * distance to an underground tile is dominated by {@code y + 6400}, which made Lumbridge look closer
 * even though the climb-down is fifty tiles. CS2 {@code isCastable} does not see that timer, so
 * cooldown is {@code AIDE_TELE_TIMER}.</p>
 *
 * <p>Other spellbooks' home teleports land elsewhere and are not used here. Law-rune teleports are
 * left to the planner; this does not hide them when laws are missing.</p>
 */
public final class HomeTeleportPlan {

    /** Where the standard-book home teleport lands. */
    public static final WorldPoint COURTYARD = new WorldPoint(3222, 3218, 0);

    /**
     * How far from the courtyard the player must be before the cooldown is worth spending.
     * Matches VitaLite's {@code distanceFromPlayer() > 50} gate.
     */
    public static final int MIN_DISTANCE_TILES = 50;

    /** Home teleport takes about twenty seconds to channel. */
    public static final long CAST_TIMEOUT_MS = 25_000;

    private HomeTeleportPlan() {
    }

    /**
     * Whether a spell is any book's home teleport, which share the thirty-minute timer.
     *
     * @param spell the spell, may be null
     * @return true when it is a home teleport
     */
    public static boolean isHomeTeleport(CastableSpell spell) {
        return HomeTeleports.isHomeTeleport(spell);
    }

    /**
     * Whether the shared home-teleport timer is still running.
     *
     * @param aideTeleTimerMinutes the {@code AIDE_TELE_TIMER} varp, minutes since epoch of last use
     * @param now the current time, may be null
     * @return true when the spell cannot be cast yet
     */
    public static boolean isOnCooldown(int aideTeleTimerMinutes, Instant now) {
        return HomeTeleports.isOnCooldown(aideTeleTimerMinutes, now);
    }

    /**
     * Whether the player is far enough from the courtyard for a home teleport to be worth it.
     *
     * @param here where they are standing, may be null
     * @return true when 2D distance to the courtyard exceeds {@link #MIN_DISTANCE_TILES}
     */
    public static boolean isFarFromCourtyard(WorldPoint here) {
        return here != null && here.distanceTo2D(COURTYARD) > MIN_DISTANCE_TILES;
    }

    /**
     * Whether the courtyard is closer to the destination than the player is, in 2D.
     *
     * <p>Used to skip a second pathfind when a home teleport could not possibly shorten the walk.
     * Underground tiles at {@code y + 6400} can still look closer in 2D; {@link #preferViaCourtyard}
     * is what actually chooses, using dense path length.</p>
     *
     * @param here where the player is, may be null
     * @param destination where they are going, may be null
     * @return true when the courtyard is strictly closer
     */
    public static boolean courtyardIsCloser(WorldPoint here, WorldPoint destination) {
        return WalkPlan.progressDistance(COURTYARD, destination)
                < WalkPlan.progressDistance(here, destination);
    }

    /**
     * Whether a Lumbridge home teleport would leave a shorter remaining walk.
     *
     * <p>Compares dense path lengths, matching VitaLite's extra start at the courtyard: the cheaper
     * search wins. Sparse waypoint counts are not used — a 116-tile walk compressed to 38 points
     * would otherwise lose to a denser 40-point walk from the courtyard. 2D distance to the
     * destination is not used either: an underground tile at {@code y + 6400} makes Lumbridge look
     * closer than a volcano you are already standing on.</p>
     *
     * @param here where the player is standing, may be null
     * @param walkingComplete whether the route from here reaches the destination
     * @param walkingTiles how many tiles the route from here is
     * @param viaCourtyardComplete whether the route from the courtyard reaches the destination
     * @param viaCourtyardTiles how many tiles the route from the courtyard is
     * @return true when a home teleport would leave a shorter walk
     */
    public static boolean preferViaCourtyard(
            WorldPoint here,
            boolean walkingComplete,
            int walkingTiles,
            boolean viaCourtyardComplete,
            int viaCourtyardTiles) {
        if (!isFarFromCourtyard(here) || !viaCourtyardComplete) {
            return false;
        }

        if (!walkingComplete) {
            return true;
        }

        return viaCourtyardTiles < walkingTiles;
    }
}
