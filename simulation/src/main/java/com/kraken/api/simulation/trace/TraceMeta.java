package com.kraken.api.simulation.trace;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * First line of a wave trace: run context shared by every later line. Serialized with Gson verbatim, so field names
 * here are the wire format; the pack's own meta columns ({@link #columns}) sit beside them on the same line.
 */
public final class TraceMeta {
    /** Line type tag, always "meta". */
    public String type = "meta";
    /** Trace schema version the writer used. */
    public int schema = Traces.SCHEMA;
    /** "live" (recorded from the game client), "replay" (engine re run of a live trace), or "sim" (synthetic run). */
    public String source;
    /** The content pack that wrote the trace; absent in schema 1 traces. */
    public String pack;
    /** Wave number the trace covers. */
    public int wave;
    /** True when tick 0 is the wave start message tick, so the spawn gates apply. */
    public boolean anchored;
    /** Client tick of the wave start message; -1 in sim traces and mid wave enables. */
    public int waveStartClientTick = -1;
    /** Player maximum hit points for the wave. */
    public int playerMaxHp;
    /** Player maximum prayer points for the wave. */
    public int playerMaxPrayer;
    /** Player agility level, which drives run drain and regeneration. */
    public int agility;
    /** True when the player had a lightbearer, which speeds special attack regeneration. */
    public boolean lightbearer;
    /** Pipeline default first step gate in ticks after an NPC's spawn visible tick. */
    public int gateMoveTick;
    /** Pipeline gate for holding on sight, in ticks after the spawn visible tick. */
    public int gateLosTick;
    /** Pipeline gate for the first attack, in ticks after the spawn visible tick. */
    public int gateAttackTick;
    /** Time the trace was created, in epoch milliseconds. */
    public long createdMs;
    /** Replay only: file name of the live trace this replay was driven by. */
    public String replayOf;
    /** Replay only: channels copied from the live trace each tick instead of simulated; the diff skips them. */
    public List<String> forced;

    /** The whole line when read; the pack's columns to add when written. */
    public transient JsonObject columns = new JsonObject();
}
