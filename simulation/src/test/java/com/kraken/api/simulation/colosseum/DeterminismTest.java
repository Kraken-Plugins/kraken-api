package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.world.SimState;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

/**
 * Determinism: identical (frame, state, command stream) must produce identical states —
 * there is no RNG anywhere in the engine. 50 random scenarios (mixed naive and smart
 * NPCs, charged and uncharged manticores, random move/run commands) x 200 ticks, replayed
 * from scratch with fresh engines, compared by per-tick state hash: 10,000 tick pairs.
 */
public class DeterminismTest {
    private static final String[] KEYS = {
            "serpent_shaman", "javelin_colossus", "jaguar_warrior", "minotaur",
            "shockwave_colossus", "manticore", "warband_berserker", "warband_seer",
    };

    private final ColosseumPack pack = ColosseumPack.load();
    private final MechanicsData data = pack.data();
    private final Grid arena = TestArenas.grid(new Simulation());

    @Test
    public void replayedScenariosHashIdentically() {
        for (long seed = 1; seed <= 50; seed++) {
            assertArrayEquals("seed " + seed, run(seed), run(seed));
        }
    }

    private long[] run(long seed) {
        Random rng = new Random(seed);
        Frame f = TestArenas.frame(arena, pack);
        SimState st = new SimState();
        st.frame = f;
        st.playerPos = openTile(rng);

        int mobs = 2 + rng.nextInt(6);
        int placedMask = 0;
        for (int slot = 0; slot < mobs; slot++) {
            String key = KEYS[rng.nextInt(KEYS.length)];
            NpcDef def = data.npcs[data.typeIndex(key)];
            for (int attempt = 0; attempt < 100; attempt++) {
                int pos = openTile(rng);
                int x = Coords.x(pos);
                int y = Coords.y(pos);
                if (arena.illegalStatic(def.size, x, y) || overlapsPlaced(f, st, placedMask, x, y, def.size)) {
                    continue;
                }
                TestArenas.addNpc(f, st, slot, data, key, x, y);
                if (ColosseumPack.manticore(def) != null && rng.nextBoolean()) {
                    st.npcFlags[slot] |= Lanes.FLAG_CHARGED;
                }
                placedMask |= 1 << slot;
                break;
            }
        }
        f.finishOrder();
        st.rebuildOccupancy();

        Engine engine = new Engine();
        engine.beginPass();
        long[] hashes = new long[200];
        for (int t = 0; t < 200; t++) {
            long cmd = Command.NONE;
            int roll = rng.nextInt(100);
            if (roll < 20) {
                cmd = Command.move(openTile(rng));
            } else if (roll < 25) {
                st.running = rng.nextBoolean();
            }
            engine.advanceInPlace(st, cmd);
            hashes[t] = st.stateHash();
        }
        return hashes;
    }

    private short openTile(Random rng) {
        while (true) {
            int x = rng.nextInt(35);
            int y = rng.nextInt(34);
            if (!arena.blockedMove(x, y)) {
                return Coords.pack(x, y);
            }
        }
    }

    private boolean overlapsPlaced(Frame f, SimState st, int placedMask, int x, int y, int size) {
        for (int i = 0; i < SimState.MAX_NPCS; i++) {
            if ((placedMask >> i & 1) == 0) {
                continue;
            }
            int os = data.npcs[f.npcTypeIdx[i]].size;
            int ox = Coords.x(st.npcPos[i]);
            int oy = Coords.y(st.npcPos[i]);
            if (!(x > ox + os - 1 || x + size - 1 < ox || y > oy + os - 1 || y + size - 1 < oy)) {
                return true;
            }
        }
        return false;
    }
}
