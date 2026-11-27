package com.kraken.api.simulation.colosseum.trace;

import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.trace.TraceDiff;
import com.kraken.api.simulation.trace.WaveTrace;
import com.kraken.api.simulation.trace.WaveTraceReplayer;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The recorded waves are the gate for every mechanics change (rule 3 of the simulation core in the vault spec
 * "Kraken - Simulation Engine Comparison"): each live trace is replayed through the core with the Colosseum pack and
 * diffed, and the summed mismatches may not rise above the counts Auto Colosseum V2 3.0.0 reached on 2026-11-26.
 * Lower the ceilings when a change earns it; a rise means a rule got worse against the game.
 *
 * <p>What the ceilings hold, all seven waves of 2026-08-25: 3 NPC position ticks and 10 shifted attacks, all one
 * jaguar on wave 1 (19:18) that stepped onto a fresh shaman corpse on the tick after the kill, which every other
 * recorded corpse forbade; 2 engine only attacks, both warband seer swings whose animation a defend animation
 * replaced (the hitsplat lands a tick later in the trace).
 */
public class RecordedWaveGateTest {
    private static final int MAX_POSITION_MISMATCHES = 3;
    private static final int MAX_SHIFTED_ATTACKS = 10;
    private static final int MAX_LIVE_ONLY_ATTACKS = 0;
    private static final int MAX_ENGINE_ONLY_ATTACKS = 2;
    private static final int MIN_EXACT_ATTACKS = 232;

    private static final Pattern SUMMARY = Pattern.compile(
            "summary: pos=(\\d+)/(\\d+) .*atk\\[exact=(\\d+) shifted=(\\d+) aOnly=(\\d+) bOnly=(\\d+)\\]");

    @Test
    public void recordedWavesReplayWithinTheCeilings() throws IOException {
        ColosseumPack pack = ColosseumPack.load();
        int[] sum = new int[6];
        StringBuilder detail = new StringBuilder();
        int waves = 0;
        for (Path file : RecordedWaves.files()) {
            WaveTrace live;
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                live = WaveTrace.read(r);
            }
            List<String> lines = new ArrayList<>();
            WaveTraceReplayer.replay(live, pack, lines::add, file.getFileName().toString());
            WaveTrace replay = WaveTrace.read(new StringReader(String.join("\n", lines)));
            ByteArrayOutputStream report = new ByteArrayOutputStream();
            TraceDiff.diff(live, replay, pack.data(), pack.trace(), new PrintStream(report, true, "UTF-8"));
            String text = report.toString("UTF-8");
            Matcher m = SUMMARY.matcher(text);
            assertTrue("no summary for " + file + ":\n" + text, m.find());
            for (int i = 0; i < 6; i++) {
                sum[i] += Integer.parseInt(m.group(i + 1));
            }
            detail.append(file.getFileName()).append(": ").append(m.group(0)).append('\n');
            waves++;
        }
        assertEquals("all seven recorded waves present", 7, waves);
        String why = "\n" + detail;
        assertEquals("NPC position ticks compared" + why, 2783, sum[1]);
        assertTrue("NPC position ticks wrong: " + sum[0] + why, sum[0] <= MAX_POSITION_MISMATCHES);
        assertTrue("exact attacks: " + sum[2] + why, sum[2] >= MIN_EXACT_ATTACKS);
        assertTrue("shifted attacks: " + sum[3] + why, sum[3] <= MAX_SHIFTED_ATTACKS);
        assertTrue("live-only attacks: " + sum[4] + why, sum[4] <= MAX_LIVE_ONLY_ATTACKS);
        assertTrue("engine-only attacks: " + sum[5] + why, sum[5] <= MAX_ENGINE_ONLY_ATTACKS);
    }
}
