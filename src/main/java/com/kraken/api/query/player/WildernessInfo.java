package com.kraken.api.query.player;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * The wilderness level and the player's combat level, with the combat level range it implies for attackers.
 */
@Getter
public class WildernessInfo {

    @Getter(AccessLevel.NONE)
    private final int combatLevel;

    private final int level;
    private final int minAttackableCombatLevel;
    private final int maxAttackableCombatLevel;

    /**
     * Creates the wilderness information and derives the attackable combat level range from it.
     *
     * @param level The wilderness level
     * @param combatLevel The player's combat level
     */
    public WildernessInfo(int level,  int combatLevel) {
        this.level = level;
        this.combatLevel = combatLevel;
        this.minAttackableCombatLevel = this.combatLevel - this.level;
        this.maxAttackableCombatLevel = this.combatLevel + this.level;
    }
}
