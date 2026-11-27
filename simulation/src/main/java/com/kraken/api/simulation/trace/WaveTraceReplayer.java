package com.kraken.api.simulation.trace;

import com.kraken.api.simulation.data.ContentPack;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Re runs a recorded wave through the forward model and serializes it in the same trace format, so live and
 * simulated behaviour diff tick for tick. The player is a ghost and the NPCs are the experiment:
 *
 * <ul>
 * <li><b>Forced from the live trace</b> (mirrored, not simulated; listed in the output meta's {@code forced}): the
 *     player's position, vitals and overhead prayer; every NPC's hp; NPC spawns and deaths (appearance and
 *     disappearance ticks and spawn tiles); the pack's observation derived state ({@link TracePack#forceObserved}).
 *     </li>
 * <li><b>Simulated by the engine and compared</b>: NPC movement, attack launches and cadence, line of sight, the
 *     pack's NPC mechanics, heal casts, hazards, and the damage the model books against the player ({@code phit}
 *     against the live {@code psplat} events).</li>
 * </ul>
 *
 * <p>Per tick T the replay applies the OSRS ordering the engine models: NPC turns during T see the player on the live
 * tile of T-1 (NPCs process before player movement), while launches during T are checked against the overhead
 * recorded at the end of T (prayer clicks apply in the input phase before NPC turns). After the advance, forced
 * channels are overwritten with the live tick T values and the tick is recorded, so divergence in an unforced channel
 * never contaminates a forced one. Live gap ticks free run without forcing. NPC hp forcing keeps heal predicates and
 * death timing anchored to reality; the model's own heal decisions still surface as {@code heal} events.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WaveTraceReplayer {
    private static final List<String> FORCED_NPC = Arrays.asList("player", "npc.hp", "npc.alive");
    /** Listed after the pack's channels. */
    private static final String FORCED_UNDERFOOT = "npc.pos.underfoot";

    /**
     * Replays {@code live} and writes the sim side trace to {@code out}.
     * @param live the live trace
     * @param pack the content pack the trace was recorded with
     * @param out receives one JSON line per call
     * @param replayOf the live trace's file name, for the output meta
     */
    public static void replay(WaveTrace live, ContentPack pack, Consumer<String> out, String replayOf) {
        if (live.ticks.isEmpty()) {
            throw new IllegalArgumentException("live trace has no tick records");
        }
        if (live.npcById.size() > SimState.MAX_NPCS) {
            throw new IllegalArgumentException("trace has " + live.npcById.size()
                    + " NPCs; the replay frame supports " + SimState.MAX_NPCS);
        }
        MechanicsData data = pack.data();
        TracePack tp = pack.trace();

        Frame frame = new Frame();
        frame.grid = live.grid();
        frame.data = data;
        tp.readMeta(live.meta.columns, frame);
        frame.waveNumber = (byte) Math.min(127, live.meta.wave);
        frame.playerMaxHp = (short) live.meta.playerMaxHp;
        frame.playerMaxPrayer = (short) live.meta.playerMaxPrayer;
        frame.agilityLevel = live.meta.agility;
        frame.lightbearer = live.meta.lightbearer;
        frame.waveStartGates = live.meta.anchored && live.firstT() <= 0;
        frame.overhealCap = 0;

        Map<Integer, Integer> slotById = new HashMap<>();
        int slot = 0;
        for (TraceNpc decl : live.npcById.values()) {
            int type = typeIndex(data, decl);
            frame.npcTypeIdx[slot] = (byte) type;
            frame.npcMaxHp[slot] = (short) decl.maxHp;
            // The trace id doubles as the identity token, so the replay writer re issues the same ids in the same
            // first appearance order as the live trace.
            frame.npcIndex[slot] = decl.id;
            if (decl.smart) {
                frame.setSmartPath(slot);
            }
            TraceTick.Np first = WaveTrace.np(live.tickByT.get(decl.firstT), decl.id);
            if (first != null) {
                frame.npcHomePos[slot] = (short) Traces.pack(first.xy);
            }
            slotById.put(decl.id, slot);
            slot++;
        }
        frame.finishOrder();

        TraceTick first = live.ticks.get(0);
        if (WaveTrace.isGap(first)) {
            throw new IllegalArgumentException("live trace starts with a gap tick");
        }
        SimState st = new SimState();
        st.frame = frame;
        st.tick = first.t + 1;
        tp.startReplay(live, st);
        forcePlayer(st, tp, first.p);
        for (TraceTick.Np np : first.npcs) {
            // The wave roster's gate anchor is tick 0 by trace convention (a sim trace's initial record sits at
            // t=-1, before the wave clock starts).
            forceSpawn(st, tp, slotById.get(np.id), np, frame.waveStartGates ? 0 : SimState.SPAWN_OPEN);
        }
        forceObserved(st, tp, slotById, first);
        for (TraceTick.Ev ev : WaveTrace.events(first, Traces.EV_SAND)) {
            Grid.set(st.moltenSand, ev.xy[0], ev.xy[1]);
        }
        st.rebuildOccupancy();
        st.resetAccumulators();

        WaveTraceWriter writer = new WaveTraceWriter(out, tp);
        SimTraceRecorder rec = new SimTraceRecorder(writer);
        TraceMeta meta = Traces.metaFrom(frame, "replay", tp);
        meta.waveStartClientTick = live.meta.waveStartClientTick;
        meta.replayOf = replayOf;
        List<String> forced = new ArrayList<>(FORCED_NPC);
        forced.addAll(tp.forcedChannels());
        forced.add(FORCED_UNDERFOOT);
        meta.forced = Collections.unmodifiableList(forced);
        rec.begin(meta, frame, st);

        Engine engine = new Engine();
        engine.attackListener = rec;
        engine.beginPass();
        boolean[] underfootLast = new boolean[SimState.MAX_NPCS];
        for (int t = first.t + 1; t <= live.lastT(); t++) {
            TraceTick liveTick = live.tickByT.get(t);
            boolean gap = WaveTrace.isGap(liveTick);
            st.moveDest = Coords.NONE;
            st.pathKey = -1;
            st.pathId = -1;
            st.pathCursor = 0;
            st.targetSlot = -1;
            if (!gap) {
                st.overhead = (byte) liveTick.p.oh;
                forceObserved(st, tp, slotById, liveTick);
                forceDeaths(st, liveTick);
            }
            engine.advanceInPlace(st, Command.NONE);
            if (!gap) {
                forceWorld(st, tp, slotById, liveTick, underfootLast);
            }
            rec.afterAdvance(frame, st);
        }
        rec.end(live.end != null ? live.end.result : "aborted",
                live.end != null ? live.end.reason : "live trace ended without an end line");
    }

    private static int typeIndex(MechanicsData data, TraceNpc decl) {
        int type = Traces.typeIndex(data, decl);
        if (type < 0) {
            throw new IllegalArgumentException("trace NPC " + decl.id + " has unknown key '"
                    + decl.key + "' and unknown npcId " + decl.npcId);
        }
        return type;
    }

    /** Injects the observation derived state exactly as live had resolved it by this tick. */
    private static void forceObserved(SimState st, TracePack tp, Map<Integer, Integer> slotById, TraceTick liveTick) {
        for (TraceTick.Np np : liveTick.npcs) {
            Integer slot = slotById.get(np.id);
            if (slot == null || st.frame.npcTypeIdx[slot] < 0 || !st.alive(slot)) {
                continue;
            }
            tp.forceObserved(st, slot, np, slotById);
        }
    }

    /**
     * Applies the live tick's deaths before the engine advances it: the killing hit lands before NPC turns, so a
     * dying NPC never acts on its death tick, and its body stays on its tile, blocking, until deleted (counted down
     * on its turn from this tick).
     */
    private static void forceDeaths(SimState st, TraceTick liveTick) {
        boolean any = false;
        for (TraceTick.Ev ev : WaveTrace.events(liveTick, Traces.EV_DIE)) {
            for (int slot = 0; ev.id != null && slot < SimState.MAX_NPCS; slot++) {
                if (st.frame.npcIndex[slot] != ev.id || !st.alive(slot)) {
                    continue;
                }
                NpcDef def = st.frame.def(slot);
                st.npcFlags[slot] &= (byte) ~SimState.FLAG_ALIVE;
                st.npcHp[slot] = 0;
                if (!def.noBody && def.corpseTicks > 0) {
                    st.npcCorpse[slot] = (byte) (def.corpseTicks + 1);
                }
                any = true;
            }
        }
        if (any) {
            st.rebuildOccupancy();
        }
    }

    /**
     * Overwrites the forced channels with the live tick's values (see class doc). While the live player stands
     * inside an NPC's footprint (plus one resync tick after) the NPC's position is mirrored too: the in game escape
     * walk is random, so free running it would poison every downstream tick of the position experiment.
     */
    private static void forceWorld(SimState st, TracePack tp, Map<Integer, Integer> slotById, TraceTick liveTick,
                                   boolean[] underfootLast) {
        int present = 0;
        for (TraceTick.Np np : liveTick.npcs) {
            Integer slot = slotById.get(np.id);
            if (slot == null) {
                throw new IllegalArgumentException("tick " + liveTick.t + " references undeclared NPC id " + np.id);
            }
            present |= 1 << slot;
            if (st.alive(slot)) {
                st.npcHp[slot] = (short) np.hp;
                boolean underfoot = liveTick.p != null && liveTick.p.xy != null
                        && Traces.covers(np.xy, st.frame.def(slot).size, liveTick.p.xy);
                if (underfoot || underfootLast[slot]) {
                    st.npcPos[slot] = (short) Traces.pack(np.xy);
                }
                underfootLast[slot] = underfoot;
            } else {
                forceSpawn(st, tp, slot, np, liveTick.t);
                underfootLast[slot] = false;
            }
        }
        for (int slot = 0; slot < SimState.MAX_NPCS; slot++) {
            if (st.frame.npcTypeIdx[slot] >= 0 && st.alive(slot) && (present >> slot & 1) == 0) {
                st.npcFlags[slot] &= (byte) ~SimState.FLAG_ALIVE;
                st.npcHp[slot] = 0;
                underfootLast[slot] = false;
            }
        }
        forcePlayer(st, tp, liveTick.p);
        st.rebuildOccupancy();
    }

    private static void forcePlayer(SimState st, TracePack tp, TraceTick.Pl p) {
        st.playerPos = (short) Traces.pack(p.xy);
        st.hpExpected = (short) p.hp;
        st.prayerExpected = (short) p.pr;
        st.runEnergy100 = (short) p.run;
        st.specEnergy10 = (short) p.spec;
        st.overhead = (byte) p.oh;
        st.hitStacksWorst = 0;
        tp.forcePlayer(st, p);
        st.died = false;
    }

    private static void forceSpawn(SimState st, TracePack tp, Integer slot, TraceTick.Np np, int spawnTick) {
        st.npcPos[slot] = (short) Traces.pack(np.xy);
        st.npcHp[slot] = (short) np.hp;
        st.npcFlags[slot] = (byte) (SimState.FLAG_ALIVE | SimState.FLAG_SPAWNED);
        st.npcCooldown[slot] = (byte) Math.max(-1, Math.min(127, np.cd));
        st.npcSpawnTick[slot] = (short) spawnTick;
        for (int lane = 0; lane < SimState.NPC_LANES; lane++) {
            st.npcLane[lane][slot] = 0;
        }
        tp.forceSpawn(st, slot, np);
    }
}
