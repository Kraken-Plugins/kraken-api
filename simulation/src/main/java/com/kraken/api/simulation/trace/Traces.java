package com.kraken.api.simulation.trace;

import com.google.gson.Gson;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.PipelineFlags;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.world.Frame;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared wave trace codecs: the Gson instance every line goes through, the core event kinds, style names, and the
 * bitboard hex encoding. docs/SIMULATION.md documents the wire format these produce.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Traces {
    /** Schema 2 adds the meta's {@code pack} and the NPC entries' {@code lanes}; schema 1 traces read the same. */
    public static final int SCHEMA = 2;
    /** The Gson instance every trace line is read and written with. */
    public static final Gson GSON = new Gson();

    /** An NPC attack launch. */
    public static final String EV_ATK = "atk";
    /** A player attack launch. */
    public static final String EV_PLAYER_ATK = "patk";
    /** A hitsplat on the player. */
    public static final String EV_PLAYER_SPLAT = "psplat";
    /** A hit booked against the player in the damage channels. */
    public static final String EV_PLAYER_HIT = "phit";
    /** A hitsplat on an NPC. */
    public static final String EV_NPC_SPLAT = "nsplat";
    /** A healing hitsplat on an NPC. */
    public static final String EV_NPC_HEAL_SPLAT = "nheal";
    /** An NPC casting a heal on another NPC. */
    public static final String EV_HEAL_CAST = "heal";
    /** An NPC dying. */
    public static final String EV_DIE = "die";
    /** A dead NPC's body being removed. */
    public static final String EV_GONE = "gone";
    /** A tile targeted launch (wire name from the first pack that had one, the javelin toss). */
    public static final String EV_TILE_ATTACK = "jspec";
    /** A tile targeted launch's tile, observed live as it is picked. */
    public static final String EV_TILE_PICK = "shadow";
    /** A molten sand tile. */
    public static final String EV_SAND = "sand";
    /** A tick whose state could not be captured. */
    public static final String EV_GAP = "gap";

    /**
     * The type index for a trace declaration: by key, falling back to the recorded game id.
     * @param data the tables
     * @param decl the declaration
     * @return the type index, or -1 when unresolvable
     */
    public static int typeIndex(MechanicsData data, TraceNpc decl) {
        try {
            return data.typeIndex(decl.key);
        } catch (IllegalArgumentException byKey) {
            return data.typeIndexForNpcId(decl.npcId);
        }
    }

    /**
     * Style code to wire name.
     * @param code a {@link Style} code, or -1 for unknown
     * @return the name; "?" for unknown
     */
    public static String styleName(int code) {
        switch (code) {
            case Style.RANGED:
                return "ranged";
            case Style.MAGIC:
                return "magic";
            case Style.MELEE:
                return "melee";
            case Style.TYPELESS:
                return "typeless";
            default:
                return "?";
        }
    }

    /**
     * Wire name to style code.
     * @param name the wire name
     * @return the {@link Style} code, or -1 for unknown
     */
    public static int styleCode(String name) {
        if ("typeless".equals(name)) {
            return Style.TYPELESS;
        }
        return Style.parse(name);
    }

    /**
     * A packed tile as an x, y pair.
     * @param pos the packed tile
     * @return the pair
     */
    public static int[] xy(int pos) {
        return new int[]{Coords.x(pos), Coords.y(pos)};
    }

    /**
     * An x, y pair as a packed tile.
     * @param xy the pair
     * @return the packed tile
     */
    public static int pack(int[] xy) {
        return xy[0] | xy[1] << 6;
    }

    /**
     * Whether a footprint anchored at {@code swAnchor} covers {@code tile}.
     * @param swAnchor the footprint's x, y
     * @param size the footprint's size
     * @param tile the tile's x, y
     * @return true when covered
     */
    public static boolean covers(int[] swAnchor, int size, int[] tile) {
        return tile[0] >= swAnchor[0] && tile[0] < swAnchor[0] + size
                && tile[1] >= swAnchor[1] && tile[1] < swAnchor[1] + size;
    }

    /**
     * One zero padded 16 hex digit string per row, y = 0 (south) first.
     * @param rows the board
     * @return the rows
     */
    public static List<String> encodeBoard(long[] rows) {
        List<String> out = new ArrayList<>(rows.length);
        for (long row : rows) {
            out.add(String.format("%016x", row));
        }
        return out;
    }

    /**
     * Decodes {@link #encodeBoard}.
     * @param rows the rows
     * @return the board
     */
    public static long[] decodeBoard(List<String> rows) {
        long[] out = new long[Grid.SIZE];
        for (int y = 0; y < rows.size() && y < out.length; y++) {
            out[y] = Long.parseUnsignedLong(rows.get(y), 16);
        }
        return out;
    }

    /**
     * Meta line seeded from a captured or built frame, with the pack's columns; the caller sets source specific
     * fields.
     * @param frame the frame
     * @param source "live", "replay" or "sim"
     * @param pack the pack's trace hooks
     * @return the meta
     */
    public static TraceMeta metaFrom(Frame frame, String source, TracePack pack) {
        TraceMeta m = new TraceMeta();
        m.source = source;
        m.pack = pack.packName();
        m.wave = frame.waveNumber;
        m.anchored = frame.waveStartGates;
        m.playerMaxHp = frame.playerMaxHp;
        m.playerMaxPrayer = frame.playerMaxPrayer;
        m.agility = frame.agilityLevel;
        m.lightbearer = frame.lightbearer;
        PipelineFlags flags = frame.data.flags;
        m.gateMoveTick = flags.gateMoveTick;
        m.gateLosTick = flags.gateLosTick;
        m.gateAttackTick = flags.gateAttackTick;
        m.createdMs = System.currentTimeMillis();
        pack.writeMeta(frame, m.columns);
        return m;
    }
}
