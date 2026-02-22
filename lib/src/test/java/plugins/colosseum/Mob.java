package plugins.colosseum;

import lombok.AllArgsConstructor;
import net.runelite.api.Prayer;

@AllArgsConstructor
public enum Mob {
    SERPENT_SHAMAN(12811, 10, 5, 28, Prayer.PROTECT_FROM_MAGIC),
    JAVELIN_COLOSSUS(12817, 15, 5, 54, Prayer.PROTECT_FROM_MISSILES),
    JAGUAR_WARRIOR(12810, 1,  5,47, Prayer.PROTECT_FROM_MELEE),
    MANTICORE(12818, 15, 10, 98, null),
    MINOTAUR(12812, 1, 5, 74, Prayer.PROTECT_FROM_MELEE),
    SHOCKWAVE_COLOSSUS(12819, 15, 5, 56, Prayer.PROTECT_FROM_MAGIC),
    REINFORCEMENT_SHAMAN(12811, 10, 5, 28, Prayer.PROTECT_FROM_MAGIC);

    private final int id;
    private final int attackRange;
    private final int attackSpeed;
    private final int maxHit;
    private Prayer prayer;
}
