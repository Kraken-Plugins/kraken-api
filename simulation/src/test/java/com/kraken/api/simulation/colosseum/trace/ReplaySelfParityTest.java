package com.kraken.api.simulation.colosseum.trace;

import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.trace.TraceDiff;
import com.kraken.api.simulation.trace.TraceTick;
import com.kraken.api.simulation.trace.Traces;
import com.kraken.api.simulation.trace.WaveTrace;
import com.kraken.api.simulation.trace.WaveTraceReplayer;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The replay contract in one bite: when the "live" world is the engine itself, replaying its trace through
 * {@link WaveTraceReplayer} must reproduce every behavioural channel exactly: same NPC steps, same launches, same
 * sight, same charge timing. A structural divergence here means the ghost forcing conventions (tick base, overhead
 * timing, player position ordering) broke, which would poison every real live against sim comparison.
 */
public class ReplaySelfParityTest {

    @Test
    public void replayReproducesAScriptedWaveExactly() throws IOException {
        // Wave 4: three smart pathing warbanders, a shaman, and a manticore (charge and volley machine); the script
        // moves and re prays mid wave so the ghost forcing has real player dynamics to mirror.
        TraceTestRuns run = new TraceTestRuns().run(4, 42L, 44, k -> {
            switch (k) {
                case 6: return Command.move(Coords.pack(37, 30));
                case 12: return Command.overhead(Command.OVERHEAD_MISSILES);
                case 18: return Command.move(Coords.pack(31, 24));
                case 26: return Command.overhead(Command.OVERHEAD_MAGIC);
                case 32: return Command.move(Coords.pack(28, 31));
                default: return Command.NONE;
            }
        });
        WaveTrace source = run.parse();
        assertTrue("scenario must field several NPCs", source.npcById.size() >= 4);
        int attacks = 0;
        for (TraceTick t : source.ticks) {
            attacks += WaveTrace.events(t, Traces.EV_ATK).size();
        }
        assertTrue("scenario must contain attack launches", attacks > 0);

        List<String> replayLines = new ArrayList<>();
        WaveTraceReplayer.replay(source, run.pack, replayLines::add, "self-test");
        WaveTrace replay = WaveTrace.read(new StringReader(String.join("\n", replayLines)));
        assertEquals("replay covers the same ticks", source.ticks.size(), replay.ticks.size());
        assertEquals("replay re-issues the same trace ids",
                source.npcById.keySet(), replay.npcById.keySet());

        ByteArrayOutputStream report = new ByteArrayOutputStream();
        int diverging = TraceDiff.diff(source, replay, run.pack.data(), run.pack.trace(), new PrintStream(report));
        assertEquals("replay must reproduce the scripted wave:\n" + report, 0, diverging);
    }
}
