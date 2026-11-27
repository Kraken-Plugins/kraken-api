package com.kraken.api.simulation.trace;

import com.google.gson.JsonObject;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * A content pack's part of the wave trace format: the columns it adds to records, how a replay forces its observed
 * state, and its own diff channels. The core writes and reads every generic column; a pack overrides what it needs.
 * An instance may carry live only extras for the recorder it is installed on.
 */
public interface TracePack {
    /** No pack columns. */
    TracePack NONE = new TracePack() {
    };

    /**
     * The meta's {@code pack} field.
     * @return the pack's name, or null
     */
    default String packName() {
        return null;
    }

    /**
     * Adds the pack's meta columns.
     * @param frame the frame the trace starts from
     * @param meta the meta line's object
     */
    default void writeMeta(Frame frame, JsonObject meta) {
    }

    /**
     * Adds the pack's columns to a tick record.
     * @param frame the frame
     * @param st the state after the tick
     * @param tick the record's object
     */
    default void writeTick(Frame frame, SimState st, JsonObject tick) {
    }

    /**
     * Adds the pack's columns to the player entry.
     * @param frame the frame
     * @param st the state after the tick
     * @param player the entry's object
     */
    default void writePlayer(Frame frame, SimState st, JsonObject player) {
    }

    /**
     * Adds the pack's columns to one NPC entry.
     * @param frame the frame
     * @param st the state after the tick
     * @param slot the NPC's slot
     * @param traceIdOfSlot the trace id bound to a slot, or -1
     * @param npc the entry's object
     */
    default void writeNpc(Frame frame, SimState st, int slot, IntUnaryOperator traceIdOfSlot, JsonObject npc) {
    }

    /**
     * Sets up the pack's part of a replay frame from a trace's meta (modifiers, damage model).
     * @param meta the meta line's object
     * @param frame the replay frame, grid and data set
     */
    default void readMeta(JsonObject meta, Frame frame) {
    }

    /**
     * Sets the pack's world state at the start of a replay (hazard clocks).
     * @param live the live trace
     * @param st the replay state, frame set
     */
    default void startReplay(WaveTrace live, SimState st) {
    }

    /**
     * The channels the pack's replay forcing mirrors from the live trace, for the replay meta's {@code forced}.
     * @return the channel names
     */
    default List<String> forcedChannels() {
        return Collections.emptyList();
    }

    /**
     * Forces the pack's player columns from a live record, wherever the replay forces the player.
     * @param st the replay state
     * @param p the live player entry
     */
    default void forcePlayer(SimState st, TraceTick.Pl p) {
    }

    /**
     * Forces the observation derived state of one living NPC from a live record, each live tick before the advance.
     * @param st the replay state
     * @param slot the NPC's slot
     * @param np the live entry
     * @param slotById replay slots by trace id
     */
    default void forceObserved(SimState st, int slot, TraceTick.Np np, Map<Integer, Integer> slotById) {
    }

    /**
     * Sets the pack's lanes and flags for an NPC appearing in the live trace; the core already placed it with all
     * lanes zero and the alive and spawned flags.
     * @param st the replay state
     * @param slot the NPC's slot
     * @param np the live entry
     */
    default void forceSpawn(SimState st, int slot, TraceTick.Np np) {
    }

    /**
     * Whether two traces' metas describe the same run context (the diff warns when not).
     * @param a the first meta line's object
     * @param b the second meta line's object
     * @return true when the pack's context columns agree
     */
    default boolean sameContext(JsonObject a, JsonObject b) {
        return true;
    }

    /**
     * The unit an NPC type's attack events group into for the diff (a volley), or null to compare launches one by
     * one.
     * @param def the type, or null when unresolvable
     * @param decl the declaration
     * @return the unit's label such as "volley starts", or null
     */
    default String attackGroup(NpcDef def, TraceNpc decl) {
        return null;
    }

    /**
     * Groups one NPC's attack events for the diff, when {@link #attackGroup} names a unit.
     * @param def the type, or null when unresolvable
     * @param launches (tick, style, anim or -1) per attack event, in tick order
     * @return the grouped events, in the same shape
     */
    default List<int[]> groupAttacks(NpcDef def, List<int[]> launches) {
        return launches;
    }

    /**
     * Prints the pack's diff channels, after the attack channel.
     * @param diff the running diff
     */
    default void diffChannels(TraceDiff diff) {
    }

    /**
     * Prints the pack's advisory lines, first in the advisory section.
     * @param diff the running diff
     */
    default void diffAdvisory(TraceDiff diff) {
    }

    /**
     * Whether the range tool ignores an attack animation (one that is not where the attack is judged from).
     * @param def the type
     * @param anim the animation id
     * @return true to skip the sample
     */
    default boolean skipRangeSample(NpcDef def, int anim) {
        return false;
    }
}
