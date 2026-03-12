package com.kraken.api.simulation.colosim.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NpcInfo {
    private final int npcId;
    private final int size;
    private final int range;
    private final int cooldown;
    private final String color;
}
