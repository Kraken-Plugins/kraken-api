package com.kraken.api.service.util.dps;

import com.kraken.api.service.util.dps.calc.DpsCalculator;
import com.kraken.api.service.util.dps.model.AttackStyle;
import com.kraken.api.service.util.dps.model.Loadout;
import lombok.Builder;
import lombok.Value;

/**
 * The outcome of a DPS calculation for one loadout against one monster.
 */
@Value
@Builder
public class DpsResult {

    /** Expected damage per second. */
    double dps;

    /** Expected damage per attack. */
    double expectedDamagePerAttack;

    /** Highest possible hit after all effects. */
    int maxHit;

    /** Chance for an attack to land (0-1). */
    double accuracy;

    /** The player's max attack roll. */
    int maxAttackRoll;

    /** The NPC's max defence roll against this style. */
    int npcDefenceRoll;

    /** Attack speed in game ticks. */
    int attackSpeedTicks;

    /** Average number of hits to kill the monster. */
    double expectedHitsToKill;

    /** Average time to kill the monster, in seconds. */
    double expectedTimeToKillSeconds;

    /** The attack style used. */
    AttackStyle style;

    /** The (sanitized) loadout that was evaluated. */
    Loadout loadout;

    /** The monster's npc id. */
    int monsterId;

    /** The monster's name. */
    String monsterName;

    /**
     * Builds a result snapshot from a calculator.
     * @param calc The calculator to summarize
     * @return DpsResult the summary
     */
    public static DpsResult from(DpsCalculator calc) {
        return DpsResult.builder()
                .dps(calc.getDps())
                .expectedDamagePerAttack(calc.getExpectedDamage())
                .maxHit(calc.getMax())
                .accuracy(calc.getHitChance())
                .maxAttackRoll(calc.getMaxAttackRoll())
                .npcDefenceRoll(calc.getNPCDefenceRoll())
                .attackSpeedTicks(calc.getAttackSpeed())
                .expectedHitsToKill(calc.getHtk())
                .expectedTimeToKillSeconds(calc.getTtk())
                .style(calc.getStyle())
                .loadout(calc.getPlayer())
                .monsterId(calc.getMonster().getId())
                .monsterName(calc.getMonster().getName())
                .build();
    }
}
