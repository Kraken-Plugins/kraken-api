package com.kraken.api.input.mouse.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A cursor position captured during a gesture, with its time offset from the start of that gesture.
 */
@Data
@AllArgsConstructor
public class RecordedPoint {
    private int x;
    private int y;
    // Time in MS relative to the start of this specific gesture
    private long timeOffset;
}

