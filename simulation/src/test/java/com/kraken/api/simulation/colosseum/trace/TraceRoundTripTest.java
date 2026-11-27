package com.kraken.api.simulation.colosseum.trace;

import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.trace.TraceNpc;
import com.kraken.api.simulation.trace.TraceTick;
import com.kraken.api.simulation.trace.Traces;
import com.kraken.api.simulation.trace.WaveTrace;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Writer and reader symmetry: a serialized run parses back with every layer intact, pack columns included. */
public class TraceRoundTripTest {

    @Test
    public void writtenTraceParsesBackIntact() throws IOException {
        TraceTestRuns run = new TraceTestRuns().run(3, 7L, 10, k -> Command.NONE);
        // Readers must skip line types they do not know (schema additions stay readable).
        run.lines.add(1, "{\"type\":\"future\",\"x\":1}");
        WaveTrace trace = run.parse();

        assertEquals("sim", trace.meta.source);
        assertEquals(ColosseumPack.NAME, trace.meta.pack);
        assertEquals(3, trace.meta.wave);
        assertEquals(Traces.SCHEMA, trace.meta.schema);
        assertTrue(trace.meta.anchored);
        assertEquals("the pack's meta columns are written", 0, trace.meta.columns.get("modifierMask").getAsInt());
        assertEquals(67, trace.meta.columns.get("reinforcementTick").getAsInt());

        assertArrayEquals(run.grid.blockMove, trace.blockMove);
        assertArrayEquals(run.grid.blockLos, trace.blockLos);
        Grid rebuilt = trace.grid();
        assertTrue("outside the octagon must stay blocked", rebuilt.blockedMove(63, 63));

        // Initial record at t = -1 plus one per advance.
        assertEquals(11, trace.ticks.size());
        assertEquals(-1, trace.firstT());
        assertEquals(9, trace.lastT());
        assertNotNull(trace.end);
        assertEquals("aborted", trace.end.result);

        assertTrue("wave 3 spawns warband, shaman and javelins", trace.npcById.size() >= 5);
        for (TraceNpc n : trace.npcById.values()) {
            assertNotNull(n.key);
            assertTrue(n.size >= 1);
            assertTrue(n.maxHp > 0);
            assertEquals(-1 /* initial record */, n.firstT);
        }

        boolean sawJavelinCounter = false;
        for (TraceTick t : trace.ticks) {
            assertNotNull("no gap ticks in a sim trace", t.p);
            assertNotNull(t.p.xy);
            assertFalse(t.npcs.isEmpty());
            for (TraceTick.Np n : t.npcs) {
                assertTrue(trace.npcById.containsKey(n.id));
                sawJavelinCounter |= n.column("spc") != null;
            }
        }
        assertTrue("javelin colossi must carry their special counter", sawJavelinCounter);
    }
}
