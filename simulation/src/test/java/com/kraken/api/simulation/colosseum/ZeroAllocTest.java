package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.world.SimState;
import org.junit.Test;

import java.lang.management.ManagementFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * The Phase 1 zero-allocation gate: at steady state, search passes — state copies,
 * advanceInPlace with player pathing (BFS), naive and smart NPC stepping (approach-field
 * flood fills), manticore volleys — allocate nothing. Measured with the HotSpot
 * per-thread allocation counter over a busy scenario after JIT warmup.
 */
public class ZeroAllocTest {
    @Test
    public void steadyStatePassesAllocateZeroBytes() {
        java.lang.management.ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        assumeTrue("needs com.sun.management.ThreadMXBean",
                mx instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) mx;
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        if (!bean.isThreadAllocatedMemoryEnabled()) {
            bean.setThreadAllocatedMemoryEnabled(true);
        }

        ColosseumPack pack = ColosseumPack.load();
        MechanicsData data = pack.data();
        Grid arena = TestArenas.grid(new Simulation());
        Frame f = TestArenas.frame(arena, pack);
        SimState base = new SimState();
        base.frame = f;
        base.playerPos = Coords.pack(16, 16);
        TestArenas.addNpc(f, base, 0, data, "manticore", 4, 15);
        TestArenas.addNpc(f, base, 1, data, "manticore", 27, 18);
        base.npcFlags[0] |= Lanes.FLAG_CHARGED;
        base.npcFlags[1] |= Lanes.FLAG_CHARGED;
        TestArenas.addNpc(f, base, 2, data, "serpent_shaman", 2, 16);
        TestArenas.addNpc(f, base, 3, data, "jaguar_warrior", 16, 28);
        TestArenas.addNpc(f, base, 4, data, "minotaur", 22, 4);
        TestArenas.addNpc(f, base, 5, data, "warband_berserker", 12, 2);
        TestArenas.addNpc(f, base, 6, data, "warband_seer", 30, 30);
        f.finishOrder();
        base.rebuildOccupancy();

        Engine engine = new Engine();
        SimState scratch = new SimState();
        short[] dests = {Coords.pack(13, 13), Coords.pack(20, 13), Coords.pack(20, 20), Coords.pack(13, 20)};

        for (int warm = 0; warm < 5000; warm++) {
            pass(engine, base, scratch, dests[warm & 3]);
        }
        long tid = Thread.currentThread().getId();
        long before = bean.getThreadAllocatedBytes(tid);
        for (int i = 0; i < 2000; i++) {
            pass(engine, base, scratch, dests[i & 3]);
        }
        long allocated = bean.getThreadAllocatedBytes(tid) - before;
        assertEquals("bytes allocated across 2000 steady-state passes", 0L, allocated);
    }

    /** One search-pass shape: reset, one commanded tick, a 10-tick rollout, adopt the result. */
    private static void pass(Engine engine, SimState base, SimState scratch, short dest) {
        engine.beginPass();
        scratch.copyFrom(base);
        engine.advanceInPlace(scratch, Command.move(dest));
        for (int h = 0; h < 10; h++) {
            engine.advanceInPlace(scratch, Command.NONE);
        }
        base.copyFrom(scratch);
    }
}
