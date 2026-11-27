package com.kraken.api.simulation.trace;

import java.util.List;

/**
 * Second line of a wave trace: the static arena collision, one 16 hex digit long per row (row index = y from the
 * south, bit x = tile x from the west). Makes every trace self contained: the replayer rebuilds its grid from this.
 */
public final class TraceGrid {
    /** Line type tag, always "grid". */
    public String type = "grid";
    /** Movement blocker rows, south row first, each a 16 hex digit long. */
    public List<String> blockMove;
    /** Sight blocker rows in the same encoding. */
    public List<String> blockLos;
}
