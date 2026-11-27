package com.kraken.api.simulation.move;

import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.world.Scratch;
import com.kraken.api.simulation.world.SimState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Route finding NPC stepping: descend the shared distance to melee reach field flood filled from the player
 * adjacent anchors, so these NPCs walk around pillars instead of stacking on them. Ties between equally good
 * neighbours break toward the cardinal along the dominant axis, then the diagonal, then the minor cardinal: the axis
 * first dog leg shape the OSRS route finder produces and live warband routes show (StepFit over the recorded waves:
 * 45.3% exact step match against 35.9% for diagonal first ties). Dynamic blockers (other NPCs, the player tile) are
 * excluded from the field and handled here by skipping occupied candidates; if the field is unreachable from the
 * current anchor the NPC falls back to a naive step to close distance.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MoveSmart {

    /**
     * One route finding step of a slot toward melee reach of a target tile.
     * @param st the state
     * @param sc the thread's scratch
     * @param slot the mover
     * @param size the mover's footprint size
     * @param px the target's x
     * @param py the target's y
     */
    public static void step(SimState st, Scratch sc, int slot, int size, int px, int py) {
        int pos = st.npcPos[slot];
        int x = Coords.x(pos);
        int y = Coords.y(pos);
        Grid g = st.frame.grid;
        short[] field = sc.approachField(g, size, px, py);
        int cur = field[pos & 0xfff];
        if (cur <= 0) {
            return;
        }
        if (cur == Scratch.UNREACHED) {
            MoveNaive.step(st, slot, size, px, py);
            return;
        }
        int sx = Integer.signum(px - x);
        int sy = Integer.signum(py - y);
        boolean xDominant = Math.abs(px - x) >= Math.abs(py - y);
        int best = cur;
        int bestPos = -1;
        for (int k = 0; k < 3; k++) {
            int ddx;
            int ddy;
            if (k == 1) {
                ddx = sx;
                ddy = sy;
            } else if ((k == 0) == xDominant) {
                ddx = sx;
                ddy = 0;
            } else {
                ddx = 0;
                ddy = sy;
            }
            if (ddx == 0 && ddy == 0) {
                continue;
            }
            int v = candidate(st, g, field, x, y, ddx, ddy, size, px, py);
            if (v < best) {
                best = v;
                bestPos = (x + ddx) | (y + ddy) << 6;
            }
        }
        for (int d = 0; d < 8; d++) {
            int ddx = Scratch.DIR_X[d];
            int ddy = Scratch.DIR_Y[d];
            if ((ddx == sx && ddy == sy) || (ddx == sx && ddy == 0 && sx != 0) || (ddx == 0 && ddy == sy && sy != 0)) {
                continue;
            }
            int v = candidate(st, g, field, x, y, ddx, ddy, size, px, py);
            if (v < best) {
                best = v;
                bestPos = (x + ddx) | (y + ddy) << 6;
            }
        }
        if (bestPos >= 0) {
            st.npcPos[slot] = (short) bestPos;
        }
    }

    private static int candidate(SimState st, Grid g, short[] field, int x, int y, int ddx, int ddy,
                                 int size, int px, int py) {
        int nx = x + ddx;
        int ny = y + ddy;
        if (!MoveNaive.legal(st, g, nx, ny, size)) {
            return Scratch.UNREACHED;
        }
        if (px >= nx && px <= nx + size - 1 && py >= ny && py <= ny + size - 1) {
            return Scratch.UNREACHED;
        }
        if (size == 1 && ddx != 0 && ddy != 0
                && (!MoveNaive.legal(st, g, nx, y, 1) || !MoveNaive.legal(st, g, x, ny, 1))) {
            return Scratch.UNREACHED;
        }
        return field[nx | ny << 6];
    }
}
