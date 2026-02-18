package com.kraken.api.sim.colosim.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Mob {
    private int x;
    private int y;
    private int type;
    private int spawnX;
    private int spawnY;
    private int cooldown;
    private String extra;
    private String originalExtra;
}
