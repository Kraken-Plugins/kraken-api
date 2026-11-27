package com.kraken.api.simulation.trace;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * Serializes a wave into the JSONL trace format from per tick (Frame, SimState) pairs plus buffered events, with the
 * pack's columns. The live recorder and the sim and replay recorders all write through this class, which is what
 * keeps the two sides of a comparison compatible.
 *
 * <p>The writer owns the slot to trace id binding: an NPC gets one id for its lifetime (keyed on the caller's
 * identity token: the game's NPC index live, the synthetic slot index in sim runs), a new declaration line on first
 * appearance, and an automatic "gone" event when it disappears without a buffered death. Ids are never reused.
 */
public final class WaveTraceWriter {
    private static final class PendingEv {
        final TraceTick.Ev ev;
        final int ident;
        final int srcIdent;
        final int tgtIdent;

        PendingEv(TraceTick.Ev ev, int ident, int srcIdent, int tgtIdent) {
            this.ev = ev;
            this.ident = ident;
            this.srcIdent = srcIdent;
            this.tgtIdent = tgtIdent;
        }
    }

    private static final int NO_IDENT = Integer.MIN_VALUE;

    private final Consumer<String> sink;
    private final TracePack pack;
    private final int[] identBySlot = new int[SimState.MAX_NPCS];
    private final int[] idBySlot = new int[SimState.MAX_NPCS];
    private final boolean[] deadSinceBind = new boolean[SimState.MAX_NPCS];
    private final Map<Integer, Integer> idByIdent = new HashMap<>();
    private final long[] prevSand = new long[Grid.SIZE];
    private final List<PendingEv> pending = new ArrayList<>();
    private int prevAliveMask;
    private int nextId = 1;
    private boolean open;
    private int lastT = Integer.MIN_VALUE;

    /**
     * A writer with no pack columns.
     * @param sink receives one JSON line per call
     */
    public WaveTraceWriter(Consumer<String> sink) {
        this(sink, TracePack.NONE);
    }

    /**
     * A writer with a pack's columns.
     * @param sink receives one JSON line per call
     * @param pack the pack's trace hooks
     */
    public WaveTraceWriter(Consumer<String> sink, TracePack pack) {
        this.sink = sink;
        this.pack = pack;
        Arrays.fill(identBySlot, NO_IDENT);
        Arrays.fill(idBySlot, -1);
    }

    /**
     * The identity token for a slot: the game's NPC index when present, else the slot.
     * @param f the frame
     * @param slot the slot
     * @return the token
     */
    public static int identOf(Frame f, int slot) {
        return f.npcIndex[slot] >= 0 ? f.npcIndex[slot] : 0x40000000 | slot;
    }

    /**
     * The last tick recorded.
     * @return the tick, or Integer.MIN_VALUE before the first
     */
    public int lastT() {
        return lastT;
    }

    /**
     * The trace id bound to a slot.
     * @param slot the slot
     * @return the id, or -1 when none
     */
    public int traceIdOf(int slot) {
        return idBySlot[slot];
    }

    /**
     * Writes the meta and grid lines.
     * @param meta the meta, with the pack's columns
     * @param grid the arena
     */
    public void begin(TraceMeta meta, Grid grid) {
        JsonObject line = Traces.GSON.toJsonTree(meta).getAsJsonObject();
        merge(line, meta.columns);
        sink.accept(Traces.GSON.toJson(line));
        TraceGrid g = new TraceGrid();
        g.blockMove = Traces.encodeBoard(grid.blockMove);
        g.blockLos = Traces.encodeBoard(grid.blockLos);
        sink.accept(Traces.GSON.toJson(g));
        open = true;
    }

    /**
     * Buffers an event with no NPC reference for the next tick record.
     * @param ev the event
     */
    public void event(TraceTick.Ev ev) {
        pending.add(new PendingEv(ev, NO_IDENT, NO_IDENT, NO_IDENT));
    }

    /**
     * Buffers an event about one NPC; {@code ident} resolves to {@code ev.id} at flush.
     * @param ev the event
     * @param ident the NPC's identity token
     */
    public void event(TraceTick.Ev ev, int ident) {
        pending.add(new PendingEv(ev, ident, NO_IDENT, NO_IDENT));
    }

    /**
     * Buffers a heal cast; both tokens resolve to {@code ev.src} and {@code ev.tgt} at flush.
     * @param ev the event
     * @param srcIdent the healer's identity token
     * @param tgtIdent the healed NPC's identity token
     */
    public void healEvent(TraceTick.Ev ev, int srcIdent, int tgtIdent) {
        pending.add(new PendingEv(ev, NO_IDENT, srcIdent, tgtIdent));
    }

    /**
     * Whether a die or gone event for this identity is already buffered this tick.
     * @param ident the identity token
     * @return true when buffered
     */
    public boolean hasDeathBuffered(int ident) {
        for (PendingEv p : pending) {
            if (p.ident == ident
                    && (Traces.EV_DIE.equals(p.ev.e) || Traces.EV_GONE.equals(p.ev.e))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Records one tick: the world after wave tick {@code t}. {@code clientTick}, {@code playerAnim}, {@code npcIds}
     * and {@code npcAnims} are the live only extras (pass -1 or null from sim recorders).
     * @param f the frame
     * @param st the state after the tick
     * @param t the wave tick
     * @param clientTick the client tick, or -1
     * @param playerAnim the player's animation, or -1
     * @param npcIds live NPC ids per slot, or null
     * @param npcAnims live NPC animations per slot, or null
     */
    public void tick(Frame f, SimState st, int t, int clientTick, int playerAnim, int[] npcIds, int[] npcAnims) {
        int aliveNow = 0;
        for (int slot = 0; slot < SimState.MAX_NPCS; slot++) {
            if (f.npcTypeIdx[slot] >= 0 && st.alive(slot)) {
                aliveNow |= 1 << slot;
            }
        }
        // Disappearances first, so the gone and die events land in this tick's record.
        for (int slot = 0; slot < SimState.MAX_NPCS; slot++) {
            if ((prevAliveMask >> slot & 1) == 0 || (aliveNow >> slot & 1) != 0
                    || identBySlot[slot] == NO_IDENT) {
                continue;
            }
            deadSinceBind[slot] = true;
            if (!hasDeathBuffered(identBySlot[slot])) {
                TraceTick.Ev gone = new TraceTick.Ev();
                gone.e = Traces.EV_GONE;
                event(gone, identBySlot[slot]);
            }
        }
        // Bind new appearances (a fresh declaration per NPC lifetime, ids never reused).
        for (int slot = 0; slot < SimState.MAX_NPCS; slot++) {
            if ((aliveNow >> slot & 1) == 0) {
                continue;
            }
            int ident = identOf(f, slot);
            boolean respawnSameIdent = identBySlot[slot] == ident && deadSinceBind[slot];
            if (identBySlot[slot] != ident || respawnSameIdent) {
                // A same tick slot handover: the previous occupant never hit the vanish pass, so its
                // disappearance is recorded here.
                if (identBySlot[slot] != NO_IDENT && identBySlot[slot] != ident
                        && (prevAliveMask >> slot & 1) != 0
                        && !hasDeathBuffered(identBySlot[slot])) {
                    TraceTick.Ev gone = new TraceTick.Ev();
                    gone.e = Traces.EV_GONE;
                    event(gone, identBySlot[slot]);
                }
                bind(f, st, slot, ident, t, npcIds);
            }
        }

        TraceTick out = new TraceTick();
        out.t = t;
        out.ct = clientTick >= 0 ? clientTick : null;
        out.p = player(st, playerAnim);
        out.npcs = new ArrayList<>();
        int[] slots = new int[SimState.MAX_NPCS];
        int count = 0;
        int px = Coords.x(st.playerPos);
        int py = Coords.y(st.playerPos);
        for (int slot = 0; slot < SimState.MAX_NPCS; slot++) {
            if ((aliveNow >> slot & 1) != 0) {
                out.npcs.add(npc(f, st, slot, px, py, npcAnims));
                slots[count++] = slot;
            }
        }
        sandDiff(st.moltenSand);
        out.ev = flushEvents();

        JsonObject line = Traces.GSON.toJsonTree(out).getAsJsonObject();
        pack.writePlayer(f, st, line.getAsJsonObject("p"));
        JsonArray npcs = line.getAsJsonArray("npcs");
        for (int k = 0; k < count; k++) {
            pack.writeNpc(f, st, slots[k], this::traceIdOf, npcs.get(k).getAsJsonObject());
        }
        pack.writeTick(f, st, line);
        sink.accept(Traces.GSON.toJson(line));
        prevAliveMask = aliveNow;
        System.arraycopy(st.moltenSand, 0, prevSand, 0, Grid.SIZE);
        lastT = t;
    }

    /**
     * A tick with no usable state (live capture failed); buffered events still flush.
     * @param t the wave tick
     * @param clientTick the client tick, or -1
     * @param reason why the tick has no state
     */
    public void gap(int t, int clientTick, String reason) {
        TraceTick out = new TraceTick();
        out.t = t;
        out.ct = clientTick >= 0 ? clientTick : null;
        TraceTick.Ev gap = new TraceTick.Ev();
        gap.e = Traces.EV_GAP;
        gap.reason = reason;
        event(gap);
        out.ev = flushEvents();
        sink.accept(Traces.GSON.toJson(out));
        lastT = t;
    }

    /**
     * Flushes leftover events as a final record, then writes the end line.
     * @param t the wave tick the trace ends on
     * @param result "clear", "death" or "aborted"
     * @param reason a note, or null
     */
    public void end(int t, String result, String reason) {
        if (!open) {
            return;
        }
        if (!pending.isEmpty()) {
            TraceTick out = new TraceTick();
            out.t = t;
            out.ev = flushEvents();
            sink.accept(Traces.GSON.toJson(out));
            lastT = t;
        }
        TraceEnd end = new TraceEnd();
        end.t = Math.max(t, lastT == Integer.MIN_VALUE ? t : lastT);
        end.result = result;
        end.reason = reason;
        sink.accept(Traces.GSON.toJson(end));
        open = false;
    }

    private void bind(Frame f, SimState st, int slot, int ident, int t, int[] npcIds) {
        NpcDef def = f.def(slot);
        TraceNpc decl = new TraceNpc();
        decl.id = nextId++;
        decl.npcId = npcIds != null && npcIds[slot] >= 0 ? npcIds[slot]
                : def.ids.length > 0 ? def.ids[0] : -1;
        decl.key = def.key;
        decl.name = def.name;
        decl.size = def.size;
        decl.maxHp = f.npcMaxHp[slot];
        decl.smart = f.smartPath(slot);
        decl.slot = slot;
        decl.rlIndex = f.npcIndex[slot];
        decl.firstT = t;
        identBySlot[slot] = ident;
        idBySlot[slot] = decl.id;
        deadSinceBind[slot] = false;
        idByIdent.put(ident, decl.id);
        sink.accept(Traces.GSON.toJson(decl));
    }

    private TraceTick.Pl player(SimState st, int playerAnim) {
        TraceTick.Pl p = new TraceTick.Pl();
        p.xy = Traces.xy(st.playerPos);
        p.hp = st.hpExpected;
        p.pr = st.prayerExpected;
        p.run = st.runEnergy100;
        p.spec = st.specEnergy10;
        p.oh = st.overhead;
        if (st.targetSlot >= 0 && idBySlot[st.targetSlot] >= 0) {
            p.tgt = idBySlot[st.targetSlot];
        }
        if (st.moveDest != Coords.NONE) {
            p.dest = Traces.xy(st.moveDest);
        }
        if (playerAnim >= 0) {
            p.anim = playerAnim;
        }
        if ((st.venomState & 1) != 0) {
            p.venom = true;
        }
        return p;
    }

    private TraceTick.Np npc(Frame f, SimState st, int slot, int px, int py, int[] npcAnims) {
        NpcDef def = f.def(slot);
        TraceTick.Np n = new TraceTick.Np();
        n.id = idBySlot[slot];
        n.xy = Traces.xy(st.npcPos[slot]);
        n.hp = st.npcHp[slot];
        n.cd = st.npcCooldown[slot];
        n.los = Los.npcCanAttack(f.grid, Coords.x(st.npcPos[slot]), Coords.y(st.npcPos[slot]),
                def.size, def.attackRange, px, py);
        if (npcAnims != null && npcAnims[slot] >= 0) {
            n.anim = npcAnims[slot];
        }
        boolean anyLane = false;
        for (int lane = 0; lane < SimState.NPC_LANES; lane++) {
            anyLane |= st.npcLane[lane][slot] != 0;
        }
        if (anyLane) {
            n.lanes = new int[SimState.NPC_LANES];
            for (int lane = 0; lane < SimState.NPC_LANES; lane++) {
                n.lanes[lane] = st.npcLane[lane][slot];
            }
        }
        return n;
    }

    private void sandDiff(long[] sand) {
        for (int y = 0; y < Grid.SIZE; y++) {
            long added = sand[y] & ~prevSand[y];
            while (added != 0) {
                int x = Long.numberOfTrailingZeros(added);
                added &= added - 1;
                TraceTick.Ev ev = new TraceTick.Ev();
                ev.e = Traces.EV_SAND;
                ev.xy = new int[]{x, y};
                event(ev);
            }
        }
    }

    private List<TraceTick.Ev> flushEvents() {
        if (pending.isEmpty()) {
            return null;
        }
        List<TraceTick.Ev> out = new ArrayList<>(pending.size());
        for (PendingEv p : pending) {
            if (p.ident != NO_IDENT) {
                p.ev.id = idByIdent.get(p.ident);
            }
            if (p.srcIdent != NO_IDENT) {
                p.ev.src = idByIdent.get(p.srcIdent);
            }
            if (p.tgtIdent != NO_IDENT) {
                p.ev.tgt = idByIdent.get(p.tgtIdent);
            }
            out.add(p.ev);
        }
        pending.clear();
        return out;
    }

    private static void merge(JsonObject into, JsonObject columns) {
        if (columns == null) {
            return;
        }
        for (Entry<String, JsonElement> e : columns.entrySet()) {
            if (!into.has(e.getKey())) {
                into.add(e.getKey(), e.getValue());
            }
        }
    }
}
