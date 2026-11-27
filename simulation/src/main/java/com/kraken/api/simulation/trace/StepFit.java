package com.kraken.api.simulation.trace;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.move.MoveNaive;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.Scratch;
import com.kraken.api.simulation.world.SimState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/**
 * Offline fitting harness for the smart NPC step rule: replays every recorded smart NPC step from the live traces
 * (inputs rebuilt from the previous tick, the chaser sees the player's previous tile) through candidate route
 * policies and reports per policy match rates. Ticks under the action gates, in escape state (player inside the
 * footprint), or carrying an impossible capture step are skipped. The winning rate is the provenance for the tie
 * break encoded in MoveSmart.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StepFit {
    /** Candidate step policies; each takes one sub step from the slot's current tile. */
    private enum Policy {
        /** Field descent, ties toward the naive preference (diagonal, x, y). */
        DIAG_FIRST,
        /** Field descent, ties toward the dominant axis cardinal, then diagonal, then minor (MoveSmart). */
        AXIS_FIRST,
        /** OSRS route finder reference: BFS from the NPC in dumb order W,E,S,N,SW,SE,NW,NE to the melee reach
         *  goal set, backtracked; first route step. */
        OSRS_BFS,
        /** Same BFS, but the destination is the player's previous tile itself and the mover may land on it. */
        OSRS_BFS_TILE
    }

    private static final int[] BFS_DX = {-1, 1, 0, 0, -1, 1, -1, 1};
    private static final int[] BFS_DY = {0, 0, -1, 1, -1, -1, 1, 1};

    /**
     * Fits the policies over a set of live traces and prints the match rates.
     * @param traces the live traces
     * @param data the tables
     * @param out where the report goes
     */
    public static void run(List<WaveTrace> traces, MechanicsData data, PrintStream out) {
        int[] match = new int[Policy.values().length];
        int evaluated = 0;
        int skippedGate = 0;
        int skippedEscape = 0;
        int skippedStale = 0;
        int skippedHold = 0;
        int misses = 0;
        for (WaveTrace trace : traces) {
            int[] perTrace = new int[Policy.values().length];
            int perTraceEval = 0;
            Grid grid = trace.grid();
            Frame frame = new Frame();
            frame.grid = grid;
            frame.data = data;
            int slot = 0;
            int[] slotById = new int[512];
            Arrays.fill(slotById, -1);
            for (TraceNpc decl : trace.npcById.values()) {
                if (slot >= SimState.MAX_NPCS) {
                    break;
                }
                frame.npcTypeIdx[slot] = (byte) typeIndex(data, decl);
                slotById[decl.id] = slot++;
            }
            frame.finishOrder();
            SimState st = new SimState();
            st.frame = frame;
            Scratch sc = new Scratch();
            sc.beginPass();

            for (TraceTick cur : trace.ticks) {
                TraceTick prev = trace.tickByT.get(cur.t - 1);
                if (WaveTrace.isGap(prev) || WaveTrace.isGap(cur)) {
                    continue;
                }
                int px = prev.p.xy[0];
                int py = prev.p.xy[1];

                for (TraceNpc decl : trace.npcById.values()) {
                    if (!decl.smart) {
                        continue;
                    }
                    int s = slotById[decl.id];
                    TraceTick.Np before = WaveTrace.np(prev, decl.id);
                    TraceTick.Np after = WaveTrace.np(cur, decl.id);
                    if (s < 0 || before == null || after == null) {
                        continue;
                    }
                    // World as seen when this NPC takes its turn during t: NPCs that process earlier in the engine
                    // order have already stepped to their tick t tiles; the rest still stand on their t-1 tiles.
                    Arrays.fill(st.npcFlags, (byte) 0);
                    for (TraceNpc other : trace.npcById.values()) {
                        int os = slotById[other.id];
                        if (os < 0) {
                            continue;
                        }
                        boolean earlier = orderIndex(frame, os) < orderIndex(frame, s);
                        TraceTick.Np np = WaveTrace.np(earlier ? cur : prev, other.id);
                        if (np == null) {
                            continue;
                        }
                        st.npcPos[os] = (short) Traces.pack(np.xy);
                        st.npcFlags[os] = SimState.FLAG_ALIVE;
                    }
                    st.rebuildOccupancy();
                    NpcDef def = data.npcs[frame.npcTypeIdx[s]];
                    int since = cur.t - Math.max(0, decl.firstT);
                    if (since < def.moveGate(data.flags)) {
                        skippedGate++;
                        continue;
                    }
                    if (Traces.covers(before.xy, def.size, prev.p.xy) || Traces.covers(after.xy, def.size, cur.p.xy)) {
                        skippedEscape++;
                        continue;
                    }
                    int stepLen = Math.max(Math.abs(after.xy[0] - before.xy[0]),
                            Math.abs(after.xy[1] - before.xy[1]));
                    // The capture staleness alias splits a 2 tile run into a 1/3 (or 0/4) pair: drop the impossible
                    // jump and the preceding under step half.
                    TraceTick.Np afterNext = WaveTrace.np(trace.tickByT.get(cur.t + 1), decl.id);
                    int nextStepLen = afterNext == null ? 0
                            : Math.max(Math.abs(afterNext.xy[0] - after.xy[0]),
                            Math.abs(afterNext.xy[1] - after.xy[1]));
                    if (stepLen > def.moveSpeed || nextStepLen > def.moveSpeed) {
                        skippedStale++;
                        continue;
                    }
                    int src = Traces.pack(before.xy);
                    if (Los.npcCanAttack(grid, before.xy[0], before.xy[1], def.size,
                            def.attackRange, px, py)) {
                        skippedHold++;
                        continue;
                    }
                    int expected = Traces.pack(after.xy);
                    evaluated++;
                    perTraceEval++;
                    for (Policy policy : Policy.values()) {
                        int got = predict(st, sc, grid, s, def, src, px, py, policy);
                        if (got == expected) {
                            match[policy.ordinal()]++;
                            perTrace[policy.ordinal()]++;
                        } else if (policy == Policy.OSRS_BFS && misses < 12) {
                            misses++;
                            out.printf("  miss w%d t=%d %s (%d,%d)->live(%d,%d) pred(%d,%d) player(%d,%d)%n",
                                    trace.meta.wave, cur.t, decl.key, before.xy[0], before.xy[1],
                                    after.xy[0], after.xy[1], Coords.x(got), Coords.y(got), px, py);
                        }
                    }
                }
            }
            out.printf("%s wave %d: evaluated %d |", trace.meta.source, trace.meta.wave, perTraceEval);
            for (Policy p : Policy.values()) {
                out.printf(" %s %d", p, perTrace[p.ordinal()]);
            }
            out.println();
        }
        out.printf("TOTAL evaluated %d (skipped: gate %d, escape %d, stale %d, hold %d)%n",
                evaluated, skippedGate, skippedEscape, skippedStale, skippedHold);
        for (Policy p : Policy.values()) {
            out.printf("  %-11s %4d/%d (%.1f%%)%n", p, match[p.ordinal()], evaluated,
                    evaluated == 0 ? 0 : 100.0 * match[p.ordinal()] / evaluated);
        }
    }

    /** Runs one policy for up to moveSpeed sub steps with the engine's hold on reach rule. */
    private static int predict(SimState st, Scratch sc, Grid grid, int slot, NpcDef def, int src, int px, int py,
                               Policy policy) {
        short saved = st.npcPos[slot];
        st.npcPos[slot] = (short) src;
        int pos = src;
        // Both route sub steps execute in full: live movers do not stop mid move on gaining reach (the reach goal
        // policies stop naturally at their goal cells).
        for (int sub = 0; sub < def.moveSpeed; sub++) {
            st.clearFootprint(slot, def.size);
            int next = stepOnce(st, sc, grid, slot, def.size, px, py, policy);
            st.npcPos[slot] = (short) next;
            st.setFootprint(slot, def.size);
            if (next == pos) {
                break;
            }
            pos = next;
        }
        st.clearFootprint(slot, def.size);
        st.npcPos[slot] = saved;
        st.setFootprint(slot, def.size);
        return pos;
    }

    private static int stepOnce(SimState st, Scratch sc, Grid grid, int slot, int size, int px, int py,
                                Policy policy) {
        int pos = st.npcPos[slot];
        if (policy == Policy.OSRS_BFS || policy == Policy.OSRS_BFS_TILE) {
            return osrsStep(st, sc, grid, slot, size, px, py, policy == Policy.OSRS_BFS_TILE);
        }
        short[] field = sc.approachField(grid, size, px, py);
        int cur = field[pos & 0xfff];
        if (cur <= 0 || cur == Scratch.UNREACHED) {
            return pos;
        }
        int x = Coords.x(pos);
        int y = Coords.y(pos);
        int sx = Integer.signum(px - x);
        int sy = Integer.signum(py - y);
        int[] order = policy == Policy.AXIS_FIRST
                ? axisFirstOrder(sx, sy, Math.abs(px - x) >= Math.abs(py - y))
                : diagFirstOrder(sx, sy);
        int best = cur;
        int bestPos = pos;
        for (int k = 0; k < order.length; k += 2) {
            int ddx = order[k];
            int ddy = order[k + 1];
            if (ddx == 0 && ddy == 0) {
                continue;
            }
            int v = candidate(st, grid, field, x, y, ddx, ddy, size, px, py);
            if (v < best) {
                best = v;
                bestPos = (x + ddx) | (y + ddy) << 6;
            }
        }
        return bestPos;
    }

    /** Diagonal toward the player, its x part, its y part, rest. */
    private static int[] diagFirstOrder(int sx, int sy) {
        return new int[]{sx, sy, sx, 0, 0, sy,
                -1, 0, 1, 0, 0, -1, 0, 1, -1, -1, 1, -1, -1, 1, 1, 1};
    }

    /** Dominant axis cardinal, then the diagonal, then the minor cardinal, rest. */
    private static int[] axisFirstOrder(int sx, int sy, boolean xDominant) {
        int[] first = xDominant ? new int[]{sx, 0, sx, sy, 0, sy} : new int[]{0, sy, sx, sy, sx, 0};
        return new int[]{first[0], first[1], first[2], first[3], first[4], first[5],
                -1, 0, 1, 0, 0, -1, 0, 1, -1, -1, 1, -1, -1, 1, 1, 1};
    }

    private static int candidate(SimState st, Grid g, short[] field, int x, int y,
                                 int ddx, int ddy, int size, int px, int py) {
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

    /**
     * OSRS dumb order BFS over static anchors, W,E,S,N,SW,SE,NW,NE; goal = the shared melee reach field's zero
     * cells; backtracks the parent chain and returns the first step, standing if another NPC occupies it.
     */
    private static int osrsStep(SimState st, Scratch sc, Grid grid, int slot, int size, int px, int py,
                                boolean toTile) {
        int src = st.npcPos[slot] & 0xfff;
        int playerTile = px | py << 6;
        short[] reach = toTile ? null : sc.approachField(grid, size, px, py);
        if (toTile ? src == playerTile : reach[src] == 0) {
            return src;
        }
        byte[] cameFrom = new byte[4096];
        Arrays.fill(cameFrom, (byte) -1);
        int[] queue = new int[4096];
        int head = 0;
        int tail = 0;
        cameFrom[src] = 8;
        queue[tail++] = src;
        int goal = -1;
        while (head < tail && goal < 0) {
            int cell = queue[head++];
            int x = cell & 0x3f;
            int y = cell >> 6;
            for (int d = 0; d < 8 && goal < 0; d++) {
                int nx = x + BFS_DX[d];
                int ny = y + BFS_DY[d];
                if (nx < 0 || ny < 0 || nx >= Grid.SIZE || ny >= Grid.SIZE) {
                    continue;
                }
                int next = nx | ny << 6;
                if (cameFrom[next] >= 0 || grid.illegalStatic(size, nx, ny)) {
                    continue;
                }
                if (size == 1 && BFS_DX[d] != 0 && BFS_DY[d] != 0
                        && (grid.illegalStatic(1, nx, y) || grid.illegalStatic(1, x, ny))) {
                    continue;
                }
                if (!toTile && px >= nx && px <= nx + size - 1 && py >= ny && py <= ny + size - 1) {
                    continue;
                }
                cameFrom[next] = (byte) d;
                queue[tail++] = next;
                if (toTile ? next == playerTile : reach[next] == 0) {
                    goal = next;
                }
            }
        }
        if (goal < 0) {
            return src;
        }
        int cell = goal;
        while (true) {
            int d = cameFrom[cell];
            int prev = (cell & 0x3f) - BFS_DX[d] | (cell >> 6) - BFS_DY[d] << 6;
            if (prev == src) {
                break;
            }
            cell = prev;
        }
        int sx = cell & 0x3f;
        int sy = cell >> 6;
        return MoveNaive.legal(st, grid, sx, sy, size) ? cell : src;
    }

    private static int orderIndex(Frame frame, int slot) {
        for (int i = 0; i < frame.npcOrderCount; i++) {
            if (frame.npcOrder[i] == slot) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static int typeIndex(MechanicsData data, TraceNpc decl) {
        int type = Traces.typeIndex(data, decl);
        if (type < 0) {
            throw new IllegalArgumentException("unknown npc " + decl.key + "/" + decl.npcId);
        }
        return type;
    }
}
