package com.kraken.api.sim.colosim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
public class Mob {
    public int x;
    public int y;
    public int type;
    public int spawnX;
    public int spawnY;
    public int cooldown;
    public String extra;
    public String originalExtra;
}
