package com.kraken.api.input.mouse.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A point on a {@link NormalizedPath}, in unit coordinates relative to the path's start and length.
 */
@Data
@AllArgsConstructor
public class UnitPoint {
    // Normalized coordinates (0.0 to 1.0 on X axis, Y is deviation)
    private double x;
    private double y;

    // Normalized time (0.0 = start, 1.0 = end)
    private double t;
}