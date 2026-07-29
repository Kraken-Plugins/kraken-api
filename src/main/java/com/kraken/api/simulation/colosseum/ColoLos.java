package com.kraken.api.simulation.colosseum;

/**
 * Line-of-sight and melee reachability for the colosseum grid.
 *
 * <p>Implements the OSRS scene raycast (the same fixed-point Bresenham used by the client and
 * by the community colosseum wave simulator) against the grid's line-of-sight bitmask. For a
 * multi-tile NPC the ray is cast between the target tile and the footprint tile closest to the
 * target, which is the behaviour the community simulator validated against the live arena.</p>
 *
 * <p>Melee (range 1) never uses the raycast: it requires orthogonal adjacency to the attacker's
 * bounding box, so a melee NPC can never attack diagonally.</p>
 */
public final class ColoLos {

    private ColoLos() {
    }

    /**
     * Checks whether an attacker footprint has line of sight (and range) to a single-tile target.
     *
     * <p>Order of checks matches the validated colosseum simulator: a target underneath the
     * attacker footprint is never visible; melee uses bounding-box adjacency; otherwise the
     * footprint tile nearest the target is clamped and a tile raycast decides.</p>
     *
     * @param grid collision grid.
     * @param anchor packed south-west anchor of the attacker footprint.
     * @param size attacker footprint size.
     * @param range attack range in tiles (1 for melee).
     * @param target packed target tile.
     * @return true when the attacker can hit the target from its current position.
     */
    public static boolean footprintHasLosTo(ColoGrid grid, short anchor, int size, int range, short target) {
        int ax = ColoCoords.x(anchor);
        int ay = ColoCoords.y(anchor);
        int tx = ColoCoords.x(target);
        int ty = ColoCoords.y(target);

        if (overlaps(ax, ay, size, tx, ty)) {
            return false;
        }
        if (range == 1) {
            return isMeleeAdjacent(ax, ay, size, tx, ty);
        }

        int nearestX = clamp(tx, ax, ax + size - 1);
        int nearestY = clamp(ty, ay, ay + size - 1);
        if (Math.abs(tx - nearestX) > range || Math.abs(ty - nearestY) > range) {
            return false;
        }
        return tileToTile(grid, tx, ty, nearestX, nearestY);
    }

    /**
     * Checks whether a single-tile attacker (the player) can hit an NPC footprint.
     *
     * @param grid collision grid.
     * @param from packed attacker tile.
     * @param range attack range in tiles (1 for melee).
     * @param targetAnchor packed south-west anchor of the target footprint.
     * @param targetSize target footprint size.
     * @return true when the attacker can hit the target footprint.
     */
    public static boolean tileHasLosToFootprint(ColoGrid grid, short from, int range, short targetAnchor, int targetSize) {
        int fx = ColoCoords.x(from);
        int fy = ColoCoords.y(from);
        int ax = ColoCoords.x(targetAnchor);
        int ay = ColoCoords.y(targetAnchor);

        if (overlaps(ax, ay, targetSize, fx, fy)) {
            return false;
        }
        if (range == 1) {
            return isMeleeAdjacent(ax, ay, targetSize, fx, fy);
        }

        int nearestX = clamp(fx, ax, ax + targetSize - 1);
        int nearestY = clamp(fy, ay, ay + targetSize - 1);
        if (Math.abs(fx - nearestX) > range || Math.abs(fy - nearestY) > range) {
            return false;
        }
        return tileToTile(grid, fx, fy, nearestX, nearestY);
    }

    /**
     * Raycast between two tiles. Source tile blocking is ignored; every stepped tile including
     * the destination must be clear of line-of-sight blockers.
     *
     * @param grid collision grid.
     * @param x0 source local x.
     * @param y0 source local y.
     * @param x1 target local x.
     * @param y1 target local y.
     * @return true when unobstructed.
     */
    public static boolean tileToTile(ColoGrid grid, int x0, int y0, int x1, int y1) {
        if (grid.isLosBlocked(x0, y0) || grid.isLosBlocked(x1, y1)) {
            return false;
        }
        int dx = x1 - x0;
        int dy = y1 - y0;
        if (dx == 0 && dy == 0) {
            return true;
        }
        int dxAbs = Math.abs(dx);
        int dyAbs = Math.abs(dy);

        if (dxAbs > dyAbs) {
            int x = x0;
            int yBig = (y0 << 16) + 0x8000;
            int slope = (dy << 16) / dxAbs;
            if (dy < 0) {
                yBig--;
            }
            int step = dx < 0 ? -1 : 1;
            while (x != x1) {
                x += step;
                int y = yBig >>> 16;
                if (grid.isLosBlocked(x, y)) {
                    return false;
                }
                yBig += slope;
                int nextY = yBig >>> 16;
                if (nextY != y && grid.isLosBlocked(x, nextY)) {
                    return false;
                }
            }
        } else {
            int y = y0;
            int xBig = (x0 << 16) + 0x8000;
            int slope = (dx << 16) / dyAbs;
            if (dx < 0) {
                xBig--;
            }
            int step = dy < 0 ? -1 : 1;
            while (y != y1) {
                y += step;
                int x = xBig >>> 16;
                if (grid.isLosBlocked(x, y)) {
                    return false;
                }
                xBig += slope;
                int nextX = xBig >>> 16;
                if (nextX != x && grid.isLosBlocked(nextX, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Bounding-box melee adjacency: the target must be orthogonally adjacent to the footprint
     * edge, never diagonal.
     *
     * @param anchorX footprint south-west x.
     * @param anchorY footprint south-west y.
     * @param size footprint size.
     * @param targetX target x.
     * @param targetY target y.
     * @return true when melee-reachable.
     */
    public static boolean isMeleeAdjacent(int anchorX, int anchorY, int size, int targetX, int targetY) {
        int dx = targetX - anchorX;
        int dy = targetY - anchorY;
        return (dx >= 0 && dx < size && (dy == -1 || dy == size))
                || (dy >= 0 && dy < size && (dx == -1 || dx == size));
    }

    /**
     * Square footprint overlap test (south-west anchored, second footprint is 1x1).
     *
     * @param anchorX first footprint south-west x.
     * @param anchorY first footprint south-west y.
     * @param size first footprint size.
     * @param tileX single tile x.
     * @param tileY single tile y.
     * @return true when the tile lies inside the footprint.
     */
    public static boolean overlaps(int anchorX, int anchorY, int size, int tileX, int tileY) {
        return tileX >= anchorX && tileX < anchorX + size && tileY >= anchorY && tileY < anchorY + size;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }
}
