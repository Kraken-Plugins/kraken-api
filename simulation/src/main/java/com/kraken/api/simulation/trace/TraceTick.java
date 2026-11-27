package com.kraken.api.simulation.trace;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * One tick record: the world after wave tick {@code t} finished processing. Boxed fields are optional on the wire
 * (Gson omits nulls). A tick with no {@code p} is a gap (live capture failed that tick); the replayer free runs
 * across gaps and the diff skips them. Each record, player and NPC entry also carries the pack's own columns, read
 * and written through {@link #columns} and the {@link TracePack}.
 */
public final class TraceTick {
    /** Line type tag, always "tick". */
    public String type = "tick";
    /** Wave tick: 0 = the wave start message tick. Sim traces may start at -1 (the pre gate build state). */
    public int t;
    /** Live only: absolute client tick. */
    public Integer ct;
    /** Player state; absent on a gap tick. */
    public Pl p;
    /** The living NPCs at the end of the tick. */
    public List<Np> npcs;
    /** Events of the tick. */
    public List<Ev> ev;

    /** The whole record when read. */
    public transient JsonObject columns;

    /** Player state at the end of the tick. */
    public static final class Pl {
        /** Tile as [x, y], region local. */
        public int[] xy;
        /** Hit points at the end of the tick. */
        public int hp;
        /** Prayer points at the end of the tick. */
        public int pr;
        /** Run energy in hundredths of a percent (0 to 10000). */
        public int run;
        /** Special energy in tenths of a percent (0 to 1000). */
        public int spec;
        /** Overhead protection: 0 none, 1 melee, 2 missiles, 3 magic. */
        public int oh;
        /** Trace id of the NPC the player is interacting with. */
        public Integer tgt;
        /** Current click destination while pathing. */
        public int[] dest;
        /** Live only: current player animation id. */
        public Integer anim;
        /** Present (true) while venomed. */
        public Boolean venom;

        /** The whole entry when read. */
        public transient JsonObject columns;
    }

    /** One living NPC at the end of the tick. */
    public static final class Np {
        /** Trace id of the NPC ({@link TraceNpc#id}). */
        public int id;
        /** Footprint anchor tile as [x, y], region local. */
        public int[] xy;
        /** Hit points at the end of the tick. */
        public int hp;
        /** Ticks to next attack. Live: tracker estimate; sim: exact model value. */
        public int cd;
        /**
         * Los.npcCanAttack from this NPC toward the player's recorded tile (sight and range; melee adjacency for
         * range 1), the same function on both sides, so it matches whenever positions match.
         */
        public boolean los;
        /** Live only: current animation id (-1 omitted). */
        public Integer anim;
        /** The state's four pack lanes for the NPC, opaque to the core; omitted while all are zero. */
        public int[] lanes;

        /** The whole entry when read. */
        public transient JsonObject columns;

        /**
         * A pack column of this entry.
         * @param name the column
         * @return its value, or null when absent
         */
        public JsonElement column(String name) {
            return columns == null ? null : columns.get(name);
        }
    }

    /** One event observed (live) or produced by the model (sim) during the tick. */
    public static final class Ev {
        /** The event kind ({@code Traces.EV_*}, or a pack's own). */
        public String e;
        /** Trace id of the subject NPC. */
        public Integer id;
        /** ranged, magic, melee, typeless, or "?". */
        public String style;
        /** Live only: animation id behind an atk or patk. */
        public Integer anim;
        /** atk: true when the launch is a tile targeted special. */
        public Boolean spec;
        /** psplat, nsplat, nheal: hitsplat amount (0 = miss). */
        public Integer d;
        /** heal: amount cast. */
        public Integer amt;
        /** phit, heal: source NPC trace id. */
        public Integer src;
        /** heal: target NPC trace id. */
        public Integer tgt;
        /** phit: expected channel damage booked. */
        public Integer exp;
        /** phit: worst channel damage booked. */
        public Integer worst;
        /** Tile events: the tile. */
        public int[] xy;
        /** Tile attack events: wave tick the impact lands. */
        public Integer land;
        /** gap: why this tick has no state. */
        public String reason;
    }
}
