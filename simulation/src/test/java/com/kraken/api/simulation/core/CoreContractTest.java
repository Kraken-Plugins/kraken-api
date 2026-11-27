package com.kraken.api.simulation.core;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.data.PipelineFlags;
import com.kraken.api.simulation.data.PlayerData;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.hits.Hits;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.npc.PackRules;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.query.Threats;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.tick.TickContext;
import com.kraken.api.simulation.trace.SimTraceRecorder;
import com.kraken.api.simulation.trace.TracePack;
import com.kraken.api.simulation.trace.TraceTick;
import com.kraken.api.simulation.trace.Traces;
import com.kraken.api.simulation.trace.WaveTrace;
import com.kraken.api.simulation.trace.WaveTraceWriter;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The core's contract on a two type test pack, no Colosseum involved: an "archer" with no behaviour (the core's
 * standard turn) and a "brute" whose behaviour replaces its attack (a rage counter in lane 0) and books its kills,
 * plus one hazard rule (a lava tile the player must not end a tick on).
 */
public class CoreContractTest {
    private static final int LAVA = Coords.pack(20, 25);

    /** Replaces the attack: a melee swing every time its rage reaches 3, rage +1 every tick it has reach. */
    static final class Brute extends NpcBehavior {
        @Override
        public void attack(TickContext c, int slot) {
            SimState st = c.state();
            if (!c.canAttack() || !c.los()) {
                return;
            }
            if (++st.npcLane[0][slot] >= 3) {
                st.npcLane[0][slot] = 0;
                c.launchStyled(slot, Style.MELEE, 20, 1, 1, false);
                c.notifyAttack(slot, Style.MELEE);
            }
        }

        @Override
        public void onDeath(TickContext c, int slot) {
            c.state().accLane[0]++;
        }
    }

    /** Lava burns the player's ending tile; counts its own calls in pack lane 0. */
    static final class Lava extends PackRules {
        int contactCalls;
        int afterTurnCalls;
        int lastClearTickSeen = Integer.MIN_VALUE;

        @Override
        public void contact(TickContext c) {
            contactCalls++;
            if (c.state().playerPos == LAVA) {
                c.damagePlayer(7, 7, 1);
            }
        }

        @Override
        public void afterNpcTurns(TickContext c) {
            afterTurnCalls++;
        }

        @Override
        public void endOfTick(TickContext c) {
            lastClearTickSeen = c.state().waveClearTick;
        }
    }

    private MechanicsData data;
    private Lava lava;
    private Frame frame;
    private SimState st;
    private Engine engine;

    private static NpcDef type(String key, String behavior, int range, int speed, int maxHit, String style) {
        NpcDef def = new NpcDef();
        def.key = key;
        def.name = key;
        def.behavior = behavior;
        def.hp = 40;
        def.attackRange = range;
        def.attackSpeed = speed;
        def.maxHit = maxHit;
        def.style = style;
        def.hitDelayTicks = 2;
        def.corpseTicks = 2;
        return def;
    }

    @Before
    public void setUp() {
        PipelineFlags flags = new PipelineFlags();
        data = new MechanicsData(new NpcDef[]{
                type("archer", "none", 8, 4, 10, "ranged"),
                type("brute", "brute", 1, 4, 20, "melee")}, new PlayerData(), flags);
        data.bindBehavior(1, new Brute());
        lava = new Lava();
        data.bindRules(lava);
        frame = new Frame();
        frame.data = data;
        frame.grid = new Grid();
        frame.grid.finish();
        st = new SimState();
        st.frame = frame;
        st.playerPos = Coords.pack(20, 20);
        st.hpExpected = 99;
        st.prayerExpected = 99;
        engine = new Engine();
        engine.beginPass();
    }

    private void npc(int slot, int type, int x, int y) {
        frame.npcTypeIdx[slot] = (byte) type;
        frame.npcMaxHp[slot] = 40;
        frame.npcIndex[slot] = slot;
        st.npcPos[slot] = Coords.pack(x, y);
        st.npcHp[slot] = 40;
        st.npcFlags[slot] = SimState.FLAG_ALIVE;
        st.npcSpawnTick[slot] = SimState.SPAWN_OPEN;
    }

    private void finish() {
        frame.finishOrder();
        st.rebuildOccupancy();
        st.resetAccumulators();
    }

    @Test
    public void bindingRecordsOnlyTheHooksABehaviourOverrides() {
        assertEquals(NpcBehavior.HOOK_ATTACK | NpcBehavior.HOOK_DEATH, data.npcs[1].hookMask);
        assertEquals(0, data.npcs[0].hookMask);
        assertNull(data.npcs[0].behaviorImpl);
        data.bindBehavior(1, null);
        assertEquals(0, data.npcs[1].hookMask);
    }

    @Test
    public void aTypeWithNoBehaviourChasesThenAttacksOnItsCadence() {
        npc(0, 0, 20, 34);
        finish();
        List<Integer> attackTicks = new ArrayList<>();
        engine.attackListener = (slot, style, tick) -> attackTicks.add(tick);
        for (int t = 0; t < 14; t++) {
            engine.advanceInPlace(st, Command.NONE);
        }
        assertEquals("it steps until it has reach (range 8)", Coords.pack(20, 28), st.npcPos[0]);
        // The sixth step (tick 5) gains reach, and an NPC that gains reach on its step attacks that tick.
        assertEquals("then attacks every 4 ticks", List.of(5, 9), attackTicks.subList(0, 2));
    }

    @Test
    public void protectionPrayerBlocksTheStandardLaunch() {
        npc(0, 0, 20, 27);
        finish();
        engine.advanceInPlace(st, Command.overhead(Command.OVERHEAD_MISSILES));
        assertEquals("a blocked launch never enters the ring", 0, st.pendingCount);
        st.overhead = 0;
        st.npcCooldown[0] = 0;
        engine.advanceInPlace(st, Command.NONE);
        assertEquals(1, st.pendingCount);
        assertEquals(Style.RANGED, Hits.styleOf(st.pendingHits[0]));
        assertEquals(10 * 300 / 1000, Hits.expectedOf(st.pendingHits[0]));
    }

    @Test
    public void aBehaviourKeepsItsStateInLanesThatCopiesCarry() {
        npc(1, 1, 20, 21);
        finish();
        engine.advanceInPlace(st, Command.NONE);
        engine.advanceInPlace(st, Command.NONE);
        assertEquals(2, st.npcLane[0][1]);
        SimState copy = new SimState();
        copy.copyFrom(st);
        assertEquals(2, copy.npcLane[0][1]);
        long hash = copy.stateHash();
        copy.npcLane[2][1] = 1;
        assertNotEquals("every lane is part of the state's identity", hash, copy.stateHash());
        engine.advanceInPlace(st, Command.NONE);
        assertEquals("the third tick of reach swings and resets", 0, st.npcLane[0][1]);
        assertEquals(1, st.pendingCount);
        assertEquals(Style.MELEE, Hits.styleOf(st.pendingHits[0]));
    }

    @Test
    public void theCoreCreditsKillsAndTheBehaviourBooksItsOwn() {
        npc(1, 1, 20, 21);
        finish();
        st.npcHp[1] = 5;
        Hits.push(st, Hits.vsNpc(Hits.encode(0, Style.MELEE, 9, 0, Hits.SRC_PLAYER, 1, 1)));
        engine.advanceInPlace(st, Command.NONE);
        assertFalse(st.alive(1));
        assertEquals(1, st.accKills);
        assertEquals(1, st.accLane[0]);
        assertTrue("its body blocks until deleted", st.corpse(1));
        assertEquals("the wave clears the tick its last holder dies", 0, st.waveClearTick);
        assertEquals("endOfTick runs after the wave clear check", 0, lava.lastClearTickSeen);
    }

    @Test
    public void worldHooksRunOnlyWhenTheFrameNamesThem() {
        finish();
        st.playerPos = (short) LAVA;
        engine.advanceInPlace(st, Command.NONE);
        assertEquals(1, lava.contactCalls);
        assertEquals(99 - 7, st.hpExpected);
        assertEquals("no hook set the tick's scratch", 0, lava.afterTurnCalls);
        frame.packHooks = PackRules.HOOK_END_OF_TICK;
        engine.advanceInPlace(st, Command.NONE);
        assertEquals("contact is gated off for this frame", 1, lava.contactCalls);
        assertEquals(99 - 7, st.hpExpected);
    }

    @Test
    public void threatsListTheNpcsWithReachAndReadiness() {
        npc(0, 0, 20, 27);
        npc(1, 1, 20, 21);
        npc(2, 0, 50, 50);
        finish();
        int tile = Coords.pack(20, 20);
        assertEquals(0b011, Threats.whoCanAttack(st, tile, Integer.MAX_VALUE));
        st.npcCooldown[0] = 3;
        assertEquals(0b010, Threats.whoCanAttack(st, tile, 2));
        assertEquals(0b011, Threats.whoCanAttack(st, tile, 3));
    }

    @Test
    public void aTraceWithoutAPackRoundTripsTheCoreColumns() throws IOException {
        npc(1, 1, 20, 21);
        finish();
        List<String> lines = new ArrayList<>();
        SimTraceRecorder rec = new SimTraceRecorder(new WaveTraceWriter(lines::add));
        rec.begin(Traces.metaFrom(frame, "sim", TracePack.NONE), frame, st);
        engine.attackListener = rec;
        for (int t = 0; t < 3; t++) {
            engine.advanceInPlace(st, Command.NONE);
            rec.afterAdvance(frame, st);
        }
        rec.end("aborted", "test");
        WaveTrace trace = WaveTrace.read(new StringReader(String.join("\n", lines)));
        assertNull(trace.meta.pack);
        assertEquals(Traces.SCHEMA, trace.meta.schema);
        TraceTick second = trace.tickByT.get(1);
        assertNotNull(second.npcs.get(0).lanes);
        assertEquals(2, second.npcs.get(0).lanes[0]);
        assertEquals(1, WaveTrace.events(trace.tickByT.get(2), Traces.EV_ATK).size());
    }
}
