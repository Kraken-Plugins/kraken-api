package com.kraken.api.service.util.dps.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A set of player skill values. Used both for base levels and for boosts
 * (the delta added on top of base levels by potions, e.g. +19 attack from a super combat).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSkills {
    @Builder.Default
    private int attack = 99;
    @Builder.Default
    private int strength = 99;
    @Builder.Default
    private int defence = 99;
    @Builder.Default
    private int ranged = 99;
    @Builder.Default
    private int magic = 99;
    @Builder.Default
    private int hitpoints = 99;
    @Builder.Default
    private int prayer = 99;
    @Builder.Default
    private int mining = 99;

    /**
     * Creates a zeroed skill set, useful for representing "no boosts".
     * @return PlayerSkills with every value set to 0
     */
    public static PlayerSkills none() {
        return new PlayerSkills(0, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Creates a maxed (99) skill set.
     * @return PlayerSkills with every combat skill set to 99
     */
    public static PlayerSkills maxed() {
        return PlayerSkills.builder().build();
    }
}
