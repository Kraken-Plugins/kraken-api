package com.kraken.api.simulation.live;

import com.google.gson.JsonObject;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.data.PipelineFlags;
import com.kraken.api.simulation.data.PlayerData;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.trace.TracePack;
import com.kraken.api.simulation.trace.TraceTick;
import com.kraken.api.simulation.trace.Traces;
import com.kraken.api.simulation.trace.WaveTrace;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The live recorder base: one wave's lines, wave relative ticks, buffered events, and the pack's columns. */
class LiveTraceRecorderTest {

    private static final TracePack PACK = new TracePack() {
        @Override
        public String packName() {
            return "test";
        }

        @Override
        public void writeMeta(Frame frame, JsonObject meta) {
            meta.addProperty("arena", "pit");
        }

        @Override
        public void writeNpc(Frame frame, SimState st, int slot, IntUnaryOperator traceIdOfSlot, JsonObject npc) {
            npc.addProperty("rage", (int) st.npcLane[0][slot]);
        }
    };

    @Test
    void aWaveRecordsWithItsEventsAndPackColumns() throws IOException {
        NpcDef brute = new NpcDef();
        brute.key = "brute";
        brute.hp = 60;
        brute.attackRange = 1;
        MechanicsData data = new MechanicsData(new NpcDef[]{brute}, new PlayerData(), new PipelineFlags());
        Frame frame = new Frame();
        frame.data = data;
        frame.grid = new Grid();
        frame.grid.finish();
        frame.npcTypeIdx[0] = 0;
        frame.npcIndex[0] = 812;
        frame.npcMaxHp[0] = 60;
        frame.finishOrder();
        SimState state = new SimState();
        state.frame = frame;
        state.playerPos = Coords.pack(20, 20);
        state.npcPos[0] = Coords.pack(22, 20);
        state.npcHp[0] = 60;
        state.npcFlags[0] = SimState.FLAG_ALIVE;
        state.npcLane[0][0] = 7;

        List<List<String>> waves = new ArrayList<>();
        LiveTraceRecorder recorder = new LiveTraceRecorder(wave -> {
            List<String> lines = new ArrayList<>();
            waves.add(lines);
            return lines::add;
        }, PACK);
        assertFalse(recorder.isOpen());
        recorder.onNpcAttack(812, 2, 4000, false);
        recorder.record(frame, state, 3, 1005, 1000, -1, null, null);
        assertTrue(recorder.isOpen());
        recorder.onPlayerSplat(12);
        recorder.onNpcDeath(812);
        state.npcFlags[0] = 0;
        recorder.record(frame, state, 3, 1006, 1000, -1, null, null);
        recorder.finish(1006, 1000, "clear", null);
        assertFalse(recorder.isOpen());

        assertEquals(1, waves.size());
        WaveTrace trace = WaveTrace.read(new StringReader(String.join("\n", waves.get(0))));
        assertEquals("live", trace.meta.source);
        assertEquals("test", trace.meta.pack);
        assertEquals(3, trace.meta.wave);
        assertEquals(1000, trace.meta.waveStartClientTick);
        assertEquals("pit", trace.meta.columns.get("arena").getAsString());
        assertEquals(5, trace.firstT());
        assertEquals(6, trace.lastT());
        TraceTick first = trace.tickByT.get(5);
        assertEquals(1, first.npcs.size());
        assertEquals(7, first.npcs.get(0).column("rage").getAsInt());
        assertEquals(7, first.npcs.get(0).lanes[0], "the core writes the lanes beside the pack's columns");
        assertFalse(first.npcs.get(0).los, "a melee NPC two tiles east has no reach");
        assertEquals(1, WaveTrace.events(first, Traces.EV_ATK).size(), "events buffered before the record flush into it");
        TraceTick second = trace.tickByT.get(6);
        assertEquals(1, WaveTrace.events(second, Traces.EV_PLAYER_SPLAT).size());
        assertEquals(1, WaveTrace.events(second, Traces.EV_DIE).size());
        assertNotNull(trace.end);
        assertEquals("clear", trace.end.result);
    }
}
