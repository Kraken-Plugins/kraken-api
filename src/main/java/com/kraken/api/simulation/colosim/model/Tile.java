package com.kraken.api.simulation.colosim.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Tile {
    private int x;
    private int y;

    public Tile copy() {
        return new Tile(x, y);
    }
}
