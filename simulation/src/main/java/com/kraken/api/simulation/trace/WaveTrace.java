package com.kraken.api.simulation.trace;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kraken.api.simulation.geom.Grid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed wave trace: the in memory form of one JSONL file, used by the replayer and the diff tool. Lines with
 * unknown types are ignored so older readers survive schema additions; every record keeps its raw object for the
 * pack's columns.
 */
public final class WaveTrace {
    /** The meta line. */
    public TraceMeta meta;
    /** Movement blocker rows decoded from the grid line. */
    public long[] blockMove;
    /** Sight blocker rows decoded from the grid line. */
    public long[] blockLos;
    /** Declared NPCs by trace id, in declaration order. */
    public final Map<Integer, TraceNpc> npcById = new LinkedHashMap<>();
    /** Tick records in file order. */
    public final List<TraceTick> ticks = new ArrayList<>();
    /** Tick records by wave tick. */
    public final Map<Integer, TraceTick> tickByT = new LinkedHashMap<>();
    /** The end line, or null when the file has none. */
    public TraceEnd end;

    /**
     * Reads a trace file.
     * @param path the file
     * @return the trace
     * @throws IOException when the file cannot be read or has no meta line
     */
    public static WaveTrace read(Path path) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return read(r);
        }
    }

    /**
     * Reads a trace.
     * @param reader the JSONL text
     * @return the trace
     * @throws IOException when the text cannot be read or has no meta line
     */
    public static WaveTrace read(Reader reader) throws IOException {
        WaveTrace trace = new WaveTrace();
        BufferedReader br = reader instanceof BufferedReader
                ? (BufferedReader) reader : new BufferedReader(reader);
        // Instance API instead of JsonParser.parseString: the client bundled Gson predates it.
        JsonParser parser = new JsonParser();
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            JsonObject obj = parser.parse(line).getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "";
            switch (type) {
                case "meta":
                    trace.meta = Traces.GSON.fromJson(obj, TraceMeta.class);
                    trace.meta.columns = obj;
                    break;
                case "grid": {
                    TraceGrid g = Traces.GSON.fromJson(obj, TraceGrid.class);
                    trace.blockMove = Traces.decodeBoard(g.blockMove);
                    trace.blockLos = Traces.decodeBoard(g.blockLos);
                    break;
                }
                case "npc": {
                    TraceNpc n = Traces.GSON.fromJson(obj, TraceNpc.class);
                    trace.npcById.put(n.id, n);
                    break;
                }
                case "tick": {
                    TraceTick t = Traces.GSON.fromJson(obj, TraceTick.class);
                    attachColumns(t, obj);
                    trace.ticks.add(t);
                    trace.tickByT.put(t.t, t);
                    break;
                }
                case "end":
                    trace.end = Traces.GSON.fromJson(obj, TraceEnd.class);
                    break;
                default:
            }
        }
        if (trace.meta == null) {
            throw new IOException("not a wave trace: no meta line");
        }
        return trace;
    }

    private static void attachColumns(TraceTick t, JsonObject obj) {
        t.columns = obj;
        JsonElement p = obj.get("p");
        if (t.p != null && p != null && p.isJsonObject()) {
            t.p.columns = p.getAsJsonObject();
        }
        JsonElement npcs = obj.get("npcs");
        if (t.npcs != null && npcs != null && npcs.isJsonArray()) {
            JsonArray arr = npcs.getAsJsonArray();
            for (int i = 0; i < t.npcs.size() && i < arr.size(); i++) {
                t.npcs.get(i).columns = arr.get(i).getAsJsonObject();
            }
        }
    }

    /**
     * Rebuilds the arena grid the trace embeds (eroded masks and corner tiles included).
     * @return the grid
     */
    public Grid grid() {
        if (blockMove == null || blockLos == null) {
            throw new IllegalStateException("trace has no grid line");
        }
        Grid g = new Grid();
        System.arraycopy(blockMove, 0, g.blockMove, 0, Grid.SIZE);
        System.arraycopy(blockLos, 0, g.blockLos, 0, Grid.SIZE);
        g.finish();
        return g;
    }

    /**
     * The first record's tick.
     * @return the tick, 0 for an empty trace
     */
    public int firstT() {
        return ticks.isEmpty() ? 0 : ticks.get(0).t;
    }

    /**
     * The last record's tick.
     * @return the tick, 0 for an empty trace
     */
    public int lastT() {
        return ticks.isEmpty() ? 0 : ticks.get(ticks.size() - 1).t;
    }

    /**
     * The record for one NPC in a tick.
     * @param tick the tick record, or null
     * @param id the trace id
     * @return the entry, or null when absent (dead or not yet spawned)
     */
    public static TraceTick.Np np(TraceTick tick, int id) {
        if (tick == null || tick.npcs == null) {
            return null;
        }
        for (TraceTick.Np n : tick.npcs) {
            if (n.id == id) {
                return n;
            }
        }
        return null;
    }

    /**
     * Whether the record is a gap (no usable state that tick).
     * @param tick the tick record, or null
     * @return true for a gap or a missing record
     */
    public static boolean isGap(TraceTick tick) {
        return tick == null || tick.p == null;
    }

    /**
     * Events of a kind within one tick record.
     * @param tick the tick record, or null
     * @param kind the event kind
     * @return the events, never null
     */
    public static List<TraceTick.Ev> events(TraceTick tick, String kind) {
        if (tick == null || tick.ev == null) {
            return Collections.emptyList();
        }
        List<TraceTick.Ev> out = new ArrayList<>(2);
        for (TraceTick.Ev ev : tick.ev) {
            if (kind.equals(ev.e)) {
                out.add(ev);
            }
        }
        return out;
    }
}
