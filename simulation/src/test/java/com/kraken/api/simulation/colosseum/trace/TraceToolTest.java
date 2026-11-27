package com.kraken.api.simulation.colosseum.trace;

import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.trace.TraceTool;
import com.kraken.api.simulation.trace.WaveTrace;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The command line glue end to end: trace file in, replay file out, diff over both. */
public class TraceToolTest {

    @Test
    public void checkReplaysAndDiffsAFileOnDisk() throws IOException {
        TraceTestRuns run = new TraceTestRuns().run(2, 9L, 20, k -> Command.NONE);
        Path dir = Files.createTempDirectory("wave-trace");
        Path live = dir.resolve("wave-02-test.live.jsonl");
        Files.write(live, run.lines, StandardCharsets.UTF_8);

        TraceTool.main(new String[]{ColosseumPack.class.getName(), "check", live.toString()});

        Path replay = dir.resolve("wave-02-test.replay.jsonl");
        assertTrue("check must write the sibling replay trace", Files.exists(replay));
        WaveTrace replayed = WaveTrace.read(replay);
        assertEquals("replay", replayed.meta.source);
        assertEquals(run.parse().ticks.size(), replayed.ticks.size());
    }
}
