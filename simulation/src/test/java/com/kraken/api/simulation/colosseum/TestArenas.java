package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

/**
 * The single place where colosim coordinates translate to engine coordinates (A1): the colosim y axis is screen down
 * with footprints spanning [x, x+s-1] x [y-s+1, y], ours is y north with SW anchors spanning [x, x+s-1] x
 * [y, y+s-1]. Anchors therefore map (x, y) to (x, 33 - y) directly. Colosim's legal window is x in [0,34], y in
 * [0,33]; everything outside is blocked in the translated grid so the engine's eroded mask bounds reproduce colosim's
 * explicit bounds checks exactly.
 */
public final class TestArenas {
    public static final int MAX_CY = 33;

    private TestArenas() {
    }

    public static int flip(int colosimY) {
        return MAX_CY - colosimY;
    }

    /** Translated arena grid built from the oracle's own wall table and pillar rectangles. */
    public static Grid grid(Simulation sim) {
        Grid g = new Grid();
        for (int y = 0; y < Grid.SIZE; y++) {
            for (int x = 0; x < Grid.SIZE; x++) {
                if (x > 34 || y > MAX_CY || colosimBlocked(sim, x, flip(y))) {
                    Grid.set(g.blockMove, x, y);
                    Grid.set(g.blockLos, x, y);
                }
            }
        }
        g.finish();
        return g;
    }

    /** Offset from colosim's 34 by 34 frame to the region local tiles live capture uses. */
    public static final int LIVE_DX = 16;
    public static final int LIVE_DY = 18;

    /**
     * The same arena in region local tiles, the frame every coordinate in the data files is written in
     * (reinforcement gates, pillars, Solarflare rings): pillars at x 24..26 and 39..41 by y 26..28 and 41..43.
     */
    public static Grid liveGrid(Simulation sim) {
        Grid g = new Grid();
        for (int y = 0; y < Grid.SIZE; y++) {
            for (int x = 0; x < Grid.SIZE; x++) {
                int ax = x - LIVE_DX;
                int ay = y - LIVE_DY;
                if (ax < 0 || ay < 0 || ax > 34 || ay > MAX_CY || colosimBlocked(sim, ax, flip(ay))) {
                    Grid.set(g.blockMove, x, y);
                    Grid.set(g.blockLos, x, y);
                }
            }
        }
        g.finish();
        return g;
    }

    /** Colosim's isPillar over its own tables: wall ranges plus the four 3x3 pillars. */
    public static boolean colosimBlocked(Simulation sim, int x, int cy) {
        if (cy < 0 || cy > MAX_CY || x < 0) {
            return true;
        }
        for (int[] range : sim.getBlockedTileRanges()[cy]) {
            if (x >= range[0] && x < range[1]) {
                return true;
            }
        }
        for (int[] pillar : sim.getPillars()) {
            if (x >= pillar[0] && x <= pillar[0] + 2 && cy >= pillar[1] - 2 && cy <= pillar[1]) {
                return true;
            }
        }
        return false;
    }

    /** True when a size footprint anchored at colosim (x, cy) fits the arena statics. */
    public static boolean colosimLegalStatic(Simulation sim, Grid g, int x, int cy, int size) {
        int oy = flip(cy);
        return oy >= 0 && !g.illegalStatic(size, x, oy);
    }

    /** Frame over a grid with the pack's data and no modifiers, for engine only (non oracle) tests. */
    public static Frame frame(Grid grid, ColosseumPack pack) {
        Frame f = pack.frame(Mods.NONE);
        f.grid = grid;
        return f;
    }

    /** Adds an NPC in engine coordinates; callers run frame.finishOrder() + state.rebuildOccupancy() after. */
    public static void addNpc(Frame f, SimState st, int slot, MechanicsData data, String key, int x, int y) {
        int typeIdx = data.typeIndex(key);
        NpcDef def = data.npcs[typeIdx];
        f.npcTypeIdx[slot] = (byte) typeIdx;
        f.npcMaxHp[slot] = (short) def.hp;
        f.npcIndex[slot] = slot;
        if (def.smartPath) {
            f.setSmartPath(slot);
        }
        st.npcPos[slot] = Coords.pack(x, y);
        st.npcHp[slot] = (short) def.hp;
        st.npcFlags[slot] = SimState.FLAG_ALIVE;
        st.npcSpawnTick[slot] = SimState.SPAWN_OPEN;
        for (int lane = 0; lane < SimState.NPC_LANES; lane++) {
            st.npcLane[lane][slot] = 0;
        }
    }
}
