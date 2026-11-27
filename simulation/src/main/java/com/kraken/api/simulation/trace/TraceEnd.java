package com.kraken.api.simulation.trace;

/** Final line of a wave trace. */
public final class TraceEnd {
    /** Line type tag, always "end". */
    public String type = "end";
    /** Last wave tick covered (events flushed after the last full record land here). */
    public int t;
    /** "clear", "death", or "aborted". */
    public String result;
    /** Free text detail behind the result; may be absent. */
    public String reason;
}
