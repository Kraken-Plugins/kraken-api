package com.kraken.api.sim.colosim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class NpcInfo {
    public final int npcId;
    public final int size;
    public final int range;
    public final int cooldown;
    public final String color;
}
