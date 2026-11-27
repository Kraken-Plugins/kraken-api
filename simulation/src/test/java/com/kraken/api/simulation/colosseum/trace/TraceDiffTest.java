package com.kraken.api.simulation.colosseum.trace;

import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.trace.TraceDiff;
import com.kraken.api.simulation.trace.TraceNpc;
import com.kraken.api.simulation.trace.TraceTick;
import com.kraken.api.simulation.trace.Traces;
import com.kraken.api.simulation.trace.WaveTrace;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The diff tool: silent on identical traces, loud on injected divergence. */
public class TraceDiffTest {

    private static int diff(TraceTestRuns run, WaveTrace a, WaveTrace b, ByteArrayOutputStream report) {
        return TraceDiff.diff(a, b, run.pack.data(), run.pack.trace(), new PrintStream(report));
    }

    @Test
    public void identicalTracesReportParity() throws IOException {
        TraceTestRuns run = new TraceTestRuns().run(1, 5L, 16, k -> Command.NONE);
        int diverging = diff(run, run.parse(), run.parse(), new ByteArrayOutputStream());
        assertEquals(0, diverging);
    }

    @Test
    public void movedNpcIsReportedWithItsFirstDivergenceTick() throws IOException {
        TraceTestRuns run = new TraceTestRuns().run(1, 5L, 16, k -> Command.NONE);
        WaveTrace a = run.parse();
        WaveTrace b = run.parse();
        TraceTick.Np np = b.tickByT.get(5).npcs.get(0);
        np.xy = new int[]{np.xy[0] + 1, np.xy[1]};

        ByteArrayOutputStream report = new ByteArrayOutputStream();
        int diverging = diff(run, a, b, report);
        assertTrue(diverging >= 1);
        assertTrue(report.toString(), report.toString().contains("diverges from t=5"));
    }

    @Test
    public void droppedAttackIsReported() throws IOException {
        TraceTestRuns run = new TraceTestRuns().run(1, 5L, 16, k -> Command.NONE);
        WaveTrace a = run.parse();
        WaveTrace b = run.parse();
        outer:
        for (TraceTick t : b.ticks) {
            if (t.ev != null) {
                for (TraceTick.Ev ev : t.ev) {
                    if (Traces.EV_ATK.equals(ev.e)) {
                        t.ev.remove(ev);
                        break outer;
                    }
                }
            }
        }

        ByteArrayOutputStream report = new ByteArrayOutputStream();
        int diverging = diff(run, a, b, report);
        assertTrue(diverging >= 1);
        assertTrue(report.toString(), report.toString().contains("A-only"));
    }

    @Test
    public void defendAnimAttackEventsAreExcluded() throws IOException {
        TraceTestRuns run = new TraceTestRuns().run(1, 5L, 16, k -> Command.NONE);
        WaveTrace a = run.parse();
        WaveTrace b = run.parse();
        int archer = idByKey(a, "warband_archer");
        TraceTick tick = a.tickByT.get(5);
        TraceTick.Ev defend = new TraceTick.Ev();
        defend.e = Traces.EV_ATK;
        defend.id = archer;
        defend.style = "ranged";
        defend.anim = 10851;
        if (tick.ev == null) {
            tick.ev = new ArrayList<>();
        }
        tick.ev.add(defend);

        ByteArrayOutputStream report = new ByteArrayOutputStream();
        int diverging = diff(run, a, b, report);
        assertEquals("a defend anim must not register as an attack:\n" + report, 0, diverging);
    }

    @Test
    public void staleCaptureSingletonIsAdvisory() throws IOException {
        TraceTestRuns run = new TraceTestRuns().run(1, 5L, 16, k -> Command.NONE);
        WaveTrace a = run.parse();
        WaveTrace b = run.parse();
        boolean mutated = false;
        outer:
        for (int id : a.npcById.keySet()) {
            for (int t = 1; t < 15; t++) {
                TraceTick.Np prev = WaveTrace.np(a.tickByT.get(t - 1), id);
                TraceTick.Np cur = WaveTrace.np(a.tickByT.get(t), id);
                TraceTick.Np next = WaveTrace.np(a.tickByT.get(t + 1), id);
                if (prev == null || cur == null || next == null) {
                    continue;
                }
                int jump = Math.max(Math.abs(next.xy[0] - prev.xy[0]),
                        Math.abs(next.xy[1] - prev.xy[1]));
                if (jump >= 2) {
                    // Re record the tick as the recorder would with a one tick stale read.
                    cur.xy = prev.xy.clone();
                    mutated = true;
                    break outer;
                }
            }
        }
        assertTrue("scenario must contain a straight two-step segment", mutated);

        ByteArrayOutputStream report = new ByteArrayOutputStream();
        int diverging = diff(run, a, b, report);
        assertEquals("a stale capture hold must stay advisory:\n" + report, 0, diverging);
        assertTrue(report.toString(), report.toString().contains("stale"));
    }

    @Test
    public void underfootEscapeMismatchIsAdvisory() throws IOException {
        TraceTestRuns run = new TraceTestRuns().run(1, 5L, 16, k -> Command.NONE);
        WaveTrace a = run.parse();
        WaveTrace b = run.parse();
        int id = a.npcById.keySet().iterator().next();
        TraceTick.Np na = WaveTrace.np(a.tickByT.get(5), id);
        TraceTick.Np nb = WaveTrace.np(b.tickByT.get(5), id);
        // Both players stand on the NPC's tile; the model's escape step differs: random in game, so advisory.
        a.tickByT.get(5).p.xy = na.xy.clone();
        b.tickByT.get(5).p.xy = na.xy.clone();
        nb.xy = new int[]{nb.xy[0] + 1, nb.xy[1]};

        ByteArrayOutputStream report = new ByteArrayOutputStream();
        int diverging = diff(run, a, b, report);
        assertEquals("an under-footprint mismatch must stay advisory:\n" + report, 0, diverging);
        assertTrue(report.toString(), report.toString().contains("escape"));
    }

    @Test
    public void verdictCarriesAMachineReadableSummary() throws IOException {
        TraceTestRuns run = new TraceTestRuns().run(1, 5L, 16, k -> Command.NONE);
        ByteArrayOutputStream report = new ByteArrayOutputStream();
        diff(run, run.parse(), run.parse(), report);
        assertTrue(report.toString(), report.toString().contains("summary: pos="));
    }

    private static int idByKey(WaveTrace trace, String key) {
        for (TraceNpc n : trace.npcById.values()) {
            if (key.equals(n.key)) {
                return n.id;
            }
        }
        throw new AssertionError("no NPC with key " + key + " in the scenario");
    }
}
