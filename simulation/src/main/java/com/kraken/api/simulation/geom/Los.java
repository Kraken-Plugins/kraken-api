package com.kraken.api.simulation.geom;

import com.kraken.api.simulation.hits.Style;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Line of sight and attack range checks, ported from the colosim simulator (validated against the live game) into
 * SW anchored y north coordinates. The fixed point ray is the OSRS engine algorithm (a 16.16 walk along the major
 * axis checking the tile stepped into and, on a minor axis boundary crossing, the second tile), not a Bresenham
 * approximation (https://oldschool.runescape.wiki/w/Line_of_sight).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Los {

    /**
     * Whether an NPC can attack the player's tile: combined range and sight test, exactly colosim's
     * {@code hasLOS(..., isNPC=true)}. Melee (range 1) uses cardinal adjacency against the footprint, no corners.
     * Ranged and magic clamp the player onto the nearest footprint tile, then require Chebyshev distance within range
     * and a clear ray cast from the player to that tile.
     * @param g the arena
     * @param nx the NPC's anchor x
     * @param ny the NPC's anchor y
     * @param size the NPC's footprint size
     * @param range the NPC's attack range
     * @param px the player's x
     * @param py the player's y
     * @return true when the NPC has reach and sight
     */
    public static boolean npcCanAttack(Grid g, int nx, int ny, int size, int range, int px, int py) {
        if (g.blockedLos(nx, ny) || g.blockedLos(px, py)) {
            return false;
        }
        if (px >= nx && px <= nx + size - 1 && py >= ny && py <= ny + size - 1) {
            return false;
        }
        if (range == 1) {
            return meleeAdjacent(nx, ny, size, px, py);
        }
        int tx = Math.max(nx, Math.min(nx + size - 1, px));
        int ty = Math.max(ny, Math.min(ny + size - 1, py));
        if (g.blockedLos(tx, ty)) {
            return false;
        }
        if (Math.abs(tx - px) > range || Math.abs(ty - py) > range) {
            return false;
        }
        return rayClear(g, px, py, tx, ty);
    }

    /**
     * Standard melee adjacency for a footprint: the target must sit cardinally against the footprint edge; corner
     * tiles cannot be attacked from or into.
     * @param nx the footprint's anchor x
     * @param ny the footprint's anchor y
     * @param size the footprint's size
     * @param px the target's x
     * @param py the target's y
     * @return true when the target is cardinally adjacent
     */
    public static boolean meleeAdjacent(int nx, int ny, int size, int px, int py) {
        int dx = px - nx;
        int dy = py - ny;
        return (dx >= 0 && dx < size && (dy == -1 || dy == size))
                || (dy >= 0 && dy < size && (dx == -1 || dx == size));
    }

    /**
     * Whether the player standing at (px,py) can attack a footprint with a weapon of the given style and effective
     * range: melee needs cardinal adjacency, ranged and magic cast the ray from the player toward the nearest
     * footprint tile, and a player under the footprint cannot attack it.
     * @param g the arena
     * @param px the player's x
     * @param py the player's y
     * @param nx the footprint's anchor x
     * @param ny the footprint's anchor y
     * @param size the footprint's size
     * @param styleCode the weapon's {@link Style}
     * @param range the weapon's effective range
     * @return true when the attack can be made from this tile
     */
    public static boolean playerCanAttack(Grid g, int px, int py, int nx, int ny, int size,
                                          int styleCode, int range) {
        if (styleCode == Style.MELEE) {
            return meleeAdjacent(nx, ny, size, px, py);
        }
        if (px >= nx && px <= nx + size - 1 && py >= ny && py <= ny + size - 1) {
            return false;
        }
        int tx = Math.max(nx, Math.min(nx + size - 1, px));
        int ty = Math.max(ny, Math.min(ny + size - 1, py));
        if (Math.max(Math.abs(tx - px), Math.abs(ty - py)) > range) {
            return false;
        }
        return !g.blockedLos(px, py) && !g.blockedLos(tx, ty) && rayClear(g, px, py, tx, ty);
    }

    /**
     * Sight ray from (x1,y1) to (x2,y2) over the sight blockers; the starting tile is not checked, the target tile is.
     * Direction matters: OSRS casts from the attack's point of view (for NPC attacks, from the player toward the
     * clamped footprint tile).
     * @param g the arena
     * @param x1 the ray's start x
     * @param y1 the ray's start y
     * @param x2 the ray's end x
     * @param y2 the ray's end y
     * @return true when nothing blocks the ray
     */
    public static boolean rayClear(Grid g, int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int dxAbs = Math.abs(dx);
        int dyAbs = Math.abs(dy);
        if (dxAbs > dyAbs) {
            int xTile = x1;
            int y = (y1 << 16) + 0x8000;
            int slope = (dy << 16) / dxAbs;
            int xInc = dx > 0 ? 1 : -1;
            if (dy < 0) {
                y -= 1;
            }
            while (xTile != x2) {
                xTile += xInc;
                int yTile = y >>> 16;
                if (g.blockedLos(xTile, yTile)) {
                    return false;
                }
                y += slope;
                int newYTile = y >>> 16;
                if (newYTile != yTile && g.blockedLos(xTile, newYTile)) {
                    return false;
                }
            }
        } else {
            int yTile = y1;
            int x = (x1 << 16) + 0x8000;
            int slope = dyAbs == 0 ? 0 : (dx << 16) / dyAbs;
            int yInc = dy > 0 ? 1 : -1;
            if (dx < 0) {
                x -= 1;
            }
            while (yTile != y2) {
                yTile += yInc;
                int xTile = x >>> 16;
                if (g.blockedLos(xTile, yTile)) {
                    return false;
                }
                x += slope;
                int newXTile = x >>> 16;
                if (newXTile != xTile && g.blockedLos(newXTile, yTile)) {
                    return false;
                }
            }
        }
        return true;
    }
}
