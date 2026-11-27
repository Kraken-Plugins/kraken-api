package com.kraken.api.simulation.trace;

import com.kraken.api.simulation.tick.AttackListener;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

/**
 * Serializes an engine run into the wave trace format: install as the engine's {@code attackListener}, call
 * {@link #begin} once with the initial state, then {@link #afterAdvance} after every {@code advanceInPlace}. Records
 * carry {@code t = st.tick - 1}, the wave tick the advance just simulated, which is the same base the live recorder
 * uses, so a live trace and a sim trace of the same wave align tick for tick.
 */
public final class SimTraceRecorder implements AttackListener {
    private final WaveTraceWriter writer;
    private Frame frame;
    private final boolean[] prevAlive = new boolean[SimState.MAX_NPCS];

    /**
     * A recorder writing through a writer.
     * @param writer the writer
     */
    public SimTraceRecorder(WaveTraceWriter writer) {
        this.writer = writer;
    }

    /**
     * Writes meta, grid, initial declarations, and the initial tick record.
     * @param meta the meta
     * @param f the frame
     * @param st the initial state
     */
    public void begin(TraceMeta meta, Frame f, SimState st) {
        frame = f;
        writer.begin(meta, f.grid);
        recordState(f, st);
    }

    /**
     * Feeds buffered deaths and records the post advance world; call once per advance (after any replay forcing,
     * so forced channels serialize as forced).
     * @param f the frame
     * @param st the state after the advance
     */
    public void afterAdvance(Frame f, SimState st) {
        frame = f;
        for (int slot = 0; slot < SimState.MAX_NPCS; slot++) {
            if (prevAlive[slot] && f.npcTypeIdx[slot] >= 0 && !st.alive(slot)) {
                TraceTick.Ev die = new TraceTick.Ev();
                die.e = Traces.EV_DIE;
                writer.event(die, WaveTraceWriter.identOf(f, slot));
            }
        }
        recordState(f, st);
    }

    /**
     * Writes the end line.
     * @param result "clear", "death" or "aborted"
     * @param reason a note, or null
     */
    public void end(String result, String reason) {
        writer.end(writer.lastT(), result, reason);
    }

    private void recordState(Frame f, SimState st) {
        writer.tick(f, st, st.tick - 1, -1, -1, null, null);
        for (int slot = 0; slot < SimState.MAX_NPCS; slot++) {
            prevAlive[slot] = f.npcTypeIdx[slot] >= 0 && st.alive(slot);
        }
    }

    @Override
    public void onNpcAttack(int slot, int style, int tick) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_ATK;
        ev.style = Traces.styleName(style);
        writer.event(ev, WaveTraceWriter.identOf(frame, slot));
    }

    @Override
    public void onNpcHeal(int srcSlot, int targetSlot, int amount, int tick) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_HEAL_CAST;
        ev.amt = amount;
        writer.healEvent(ev, WaveTraceWriter.identOf(frame, srcSlot),
                WaveTraceWriter.identOf(frame, targetSlot));
    }

    @Override
    public void onTileAttack(int srcSlot, int targetTile, int landDelayTicks, int tick) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_TILE_ATTACK;
        ev.xy = Traces.xy(targetTile);
        ev.land = tick + landDelayTicks;
        writer.event(ev, WaveTraceWriter.identOf(frame, srcSlot));
    }

    @Override
    public void onPlayerHit(int srcSlot, int style, int expected, int worst, int tick) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_PLAYER_HIT;
        ev.style = Traces.styleName(style);
        ev.exp = expected;
        ev.worst = worst;
        if (srcSlot < SimState.MAX_NPCS) {
            writer.event(ev, WaveTraceWriter.identOf(frame, srcSlot));
        } else {
            writer.event(ev);
        }
    }
}
