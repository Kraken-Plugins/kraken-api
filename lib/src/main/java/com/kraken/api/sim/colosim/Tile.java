package com.kraken.api.sim.colosim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
public class Tile {
    public int x;
    public int y;

    public Tile copy() {
        return new Tile(x, y);
    }
}
