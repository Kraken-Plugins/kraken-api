package com.kraken.api.sim.colosim.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum NpcType {
    PLAYER(0),
    SERPENT_SHAMAN(1),
    JAVELIN_COLOSSUS(2),
    JAGUAR_WARRIOR(3),
    MANTICORE(4),
    MINOTAUR(5),
    SHOCKWAVE_COLOSSUS(6),
    REINFORCEMENT_SHAMAN(7);

    public final int typeId;

    public static NpcType fromTypeId(int typeId) {
        for (NpcType value : values()) {
            if (value.typeId == typeId) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown npc type id " + typeId);
    }
}
