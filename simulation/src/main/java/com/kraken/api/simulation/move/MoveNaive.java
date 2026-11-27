package com.kraken.api.simulation.move;

import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.world.SimState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Naive (non route finding) NPC stepping, ported exactly from colosim: step the anchor one tile toward the player,
 * diagonal preferred, else the x component, else the y component. Size 1 NPCs may not cut corners (the diagonal
 * needs both cardinal sub positions legal); larger NPCs may. A diagonal that would land on the player has its y
 * component cancelled first, the slide that walks melee NPCs around to a cardinal side. No route finding: this is
 * exactly what makes pillar stacking work.
 *
 * <p>The caller clears the mover's own footprint from the occupancy board before calling and re marks it after, so
 * legality here is a static eroded mask bit plus an occupancy test.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MoveNaive {

    /**
     * One naive step of a slot toward a target tile.
     * @param st the state
     * @param slot the mover
     * @param size the mover's footprint size
     * @param px the target's x
     * @param py the target's y
     */
    public static void step(SimState st, int slot, int size, int px, int py) {
        int pos = st.npcPos[slot];
        int x = Coords.x(pos);
        int y = Coords.y(pos);
        int dx = x + Integer.signum(px - x);
        int dy = y + Integer.signum(py - y);
        if (px >= dx && px <= dx + size - 1 && py >= dy && py <= dy + size - 1) {
            dy = y;
        }
        Grid g = st.frame.grid;
        if (legal(st, g, dx, dy, size)
                && (size > 1 || (legal(st, g, dx, y, size) && legal(st, g, x, dy, size)))) {
            st.npcPos[slot] = Coords.pack(dx, dy);
        } else if (legal(st, g, dx, y, size)) {
            st.npcPos[slot] = Coords.pack(dx, y);
        } else if (legal(st, g, x, dy, size)) {
            st.npcPos[slot] = Coords.pack(x, dy);
        }
    }

    /**
     * Anchor legality: in the window, no static blocker under the footprint, no other blocking NPC body (living or
     * a corpse; pass through types never block) overlapping it.
     * @param st the state
     * @param g the arena
     * @param x the anchor's x
     * @param y the anchor's y
     * @param size the footprint size
     * @return true when the footprint may stand there
     */
    public static boolean legal(SimState st, Grid g, int x, int y, int size) {
        if (x < 0 || y < 0 || x >= Grid.SIZE || y >= Grid.SIZE || g.illegalStatic(size, x, y)) {
            return false;
        }
        long mask = ((1L << size) - 1) << x;
        for (int r = 0; r < size; r++) {
            if ((st.occupancy[y + r] & mask) != 0) {
                return false;
            }
        }
        return true;
    }
}
