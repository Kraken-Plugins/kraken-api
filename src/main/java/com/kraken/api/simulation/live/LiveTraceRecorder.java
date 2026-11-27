package com.kraken.api.simulation.live;

import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.trace.TraceMeta;
import com.kraken.api.simulation.trace.TracePack;
import com.kraken.api.simulation.trace.TraceTick;
import com.kraken.api.simulation.trace.Traces;
import com.kraken.api.simulation.trace.WaveTraceWriter;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Records how a live wave plays out, in the wave trace format: the plugin feeds the tick's observed events (attack
 * animations, hitsplats, deaths, tile attacks and picks, the pack's own events) as they arrive, then hands over each
 * captured state; one JSONL file per wave lands in the trace directory. The pack's columns come from the
 * {@link TracePack} given, which may carry live only extras the plugin sets before each {@link #record}. Runs on the
 * client thread only, with or without a planner, so manually piloted waves record the same way autonomous ones do.
 *
 * <p>Ticks are wave relative ({@code t = clientTick - waveStartTick}); events buffered between captures flush into
 * the next tick record, so the killing blow of a wave still lands in the trace through the final flush at
 * {@link #finish}.
 */
public final class LiveTraceRecorder implements AutoCloseable {
    /** A buffered event feed, applied once the wave's writer exists. */
    private interface PendingFeed {
        void apply(WaveTraceWriter w, int waveStartTick);
    }

    private final Function<Integer, Consumer<String>> sinkForWave;
    private final TracePack pack;
    private final List<PendingFeed> pending = new ArrayList<>();
    private Consumer<String> sink;
    private WaveTraceWriter writer;
    private int openWave = -1;

    /**
     * A recorder writing {@code wave-NN-yyyyMMdd-HHmmss.live.jsonl} files into a directory.
     * @param directory the trace directory
     * @param pack the pack's trace columns
     */
    public LiveTraceRecorder(File directory, TracePack pack) {
        this(wave -> new JsonlFileSink(new File(directory, String.format("wave-%02d-%s.live.jsonl", wave,
                new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()))), "wave trace", 16), pack);
    }

    /**
     * A recorder writing each wave's lines to a sink of the caller's (tests, other storage).
     * @param sinkForWave opens the sink for a wave number; closed at the wave's end when it is AutoCloseable
     * @param pack the pack's trace columns
     */
    public LiveTraceRecorder(Function<Integer, Consumer<String>> sinkForWave, TracePack pack) {
        this.sinkForWave = sinkForWave;
        this.pack = pack;
    }

    /**
     * Whether a wave's trace is open.
     * @return true between the first record and the finish
     */
    public boolean isOpen() {
        return writer != null;
    }

    /**
     * An NPC attack launch folded from the animation stream.
     * @param npcIndex the game's NPC index
     * @param styleCode the type's style
     * @param animId the animation
     * @param tileSpecial whether it is a tile targeted special (recorded as typeless)
     */
    public void onNpcAttack(int npcIndex, int styleCode, int animId, boolean tileSpecial) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_ATK;
        ev.style = Traces.styleName(tileSpecial ? Style.TYPELESS : styleCode);
        ev.anim = animId;
        if (tileSpecial) {
            ev.spec = true;
        }
        pending.add((w, ws) -> w.event(ev, npcIndex));
    }

    /**
     * A tile targeted launch: the player's tile at launch, the provisional impact.
     * @param npcIndex the game's NPC index
     * @param launchTilePos the player's packed tile at launch
     * @param landClientTick the client tick the impact lands
     */
    public void onTileAttack(int npcIndex, int launchTilePos, int landClientTick) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_TILE_ATTACK;
        ev.xy = Traces.xy(launchTilePos);
        pending.add((w, ws) -> {
            ev.land = ws >= 0 ? landClientTick - ws : null;
            w.event(ev, npcIndex);
        });
    }

    /**
     * A tile targeted launch's tile, observed as it is picked: the authoritative impact tile.
     * @param tilePos the packed tile
     * @param landClientTick the client tick the impact lands
     */
    public void onTilePick(int tilePos, int landClientTick) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_TILE_PICK;
        ev.xy = Traces.xy(tilePos);
        pending.add((w, ws) -> {
            ev.land = ws >= 0 ? landClientTick - ws : null;
            w.event(ev);
        });
    }

    /**
     * The player's attack animation.
     * @param animId the animation
     */
    public void onPlayerAttackAnim(int animId) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_PLAYER_ATK;
        ev.anim = animId;
        pending.add((w, ws) -> w.event(ev));
    }

    /**
     * A player hitsplat, zeros (misses) included.
     * @param amount the splat's amount
     */
    public void onPlayerSplat(int amount) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_PLAYER_SPLAT;
        ev.d = amount;
        pending.add((w, ws) -> w.event(ev));
    }

    /**
     * An NPC hitsplat.
     * @param npcIndex the game's NPC index
     * @param amount the splat's amount
     * @param heal whether it heals
     */
    public void onNpcSplat(int npcIndex, int amount, boolean heal) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = heal ? Traces.EV_NPC_HEAL_SPLAT : Traces.EV_NPC_SPLAT;
        ev.d = amount;
        pending.add((w, ws) -> w.event(ev, npcIndex));
    }

    /**
     * An NPC death.
     * @param npcIndex the game's NPC index
     */
    public void onNpcDeath(int npcIndex) {
        TraceTick.Ev ev = new TraceTick.Ev();
        ev.e = Traces.EV_DIE;
        pending.add((w, ws) -> w.event(ev, npcIndex));
    }

    /**
     * A pack's own event with no NPC subject.
     * @param ev the event
     */
    public void event(TraceTick.Ev ev) {
        pending.add((w, ws) -> w.event(ev));
    }

    /**
     * A pack's own event about one NPC.
     * @param ev the event
     * @param npcIndex the game's NPC index
     */
    public void event(TraceTick.Ev ev, int npcIndex) {
        pending.add((w, ws) -> w.event(ev, npcIndex));
    }

    /**
     * Serializes one captured tick; opens the wave's file on the first call.
     * @param frame the captured frame
     * @param state the captured state
     * @param wave the wave number
     * @param clientTick the client tick of the capture
     * @param waveStartTick the wave start's client tick, or -1 when unknown
     * @param playerAnim the player's animation, or -1
     * @param npcIds live NPC ids per slot, or null
     * @param npcAnims live NPC animations per slot, or null
     */
    public void record(Frame frame, SimState state, int wave, int clientTick, int waveStartTick, int playerAnim,
                       int[] npcIds, int[] npcAnims) {
        if (writer != null && openWave != wave) {
            finish(-1, -1, "aborted", "wave changed without an end message");
        }
        if (writer == null) {
            open(frame, wave, waveStartTick);
        }
        int t = tickFor(clientTick, waveStartTick);
        flushPending(waveStartTick);
        writer.tick(frame, state, t, clientTick, playerAnim, npcIds, npcAnims);
    }

    /**
     * A tick whose capture failed: state unknown, but buffered events still flush.
     * @param clientTick the client tick
     * @param waveStartTick the wave start's client tick, or -1 when unknown
     * @param reason why the capture failed
     */
    public void gap(int clientTick, int waveStartTick, String reason) {
        if (writer == null) {
            return;
        }
        flushPending(waveStartTick);
        writer.gap(tickFor(clientTick, waveStartTick), clientTick, reason);
    }

    /**
     * Ends the open trace: flushes events buffered since the last capture (the wave's killing blow arrives here),
     * writes the end line, and closes the sink. No op when nothing is open.
     * @param clientTick the client tick of the end, or -1
     * @param waveStartTick the wave start's client tick, or -1 when unknown
     * @param result "clear", "death" or "aborted"
     * @param reason a note, or null
     */
    public void finish(int clientTick, int waveStartTick, String result, String reason) {
        if (writer == null) {
            pending.clear();
            return;
        }
        int t = clientTick >= 0 && waveStartTick >= 0
                ? clientTick - waveStartTick : writer.lastT() + (pending.isEmpty() ? 0 : 1);
        flushPending(waveStartTick);
        writer.end(t, result, reason);
        if (sink instanceof AutoCloseable) {
            try {
                ((AutoCloseable) sink).close();
            } catch (Exception ignored) {
                // A sink that cannot close has already logged its own failure.
            }
        }
        sink = null;
        writer = null;
        openWave = -1;
    }

    @Override
    public void close() {
        finish(-1, -1, "aborted", "recorder closed");
    }

    private void open(Frame frame, int wave, int waveStartTick) {
        sink = sinkForWave.apply(wave);
        writer = new WaveTraceWriter(sink, pack);
        TraceMeta meta = Traces.metaFrom(frame, "live", pack);
        meta.wave = wave;
        meta.waveStartClientTick = waveStartTick;
        writer.begin(meta, frame.grid);
        openWave = wave;
    }

    private int tickFor(int clientTick, int waveStartTick) {
        if (waveStartTick >= 0) {
            return clientTick - waveStartTick;
        }
        return writer.lastT() == Integer.MIN_VALUE ? 0 : writer.lastT() + 1;
    }

    private void flushPending(int waveStartTick) {
        for (PendingFeed feed : pending) {
            feed.apply(writer, waveStartTick);
        }
        pending.clear();
    }
}
