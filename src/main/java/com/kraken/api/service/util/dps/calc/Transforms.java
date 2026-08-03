package com.kraken.api.service.util.dps.calc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Standard hitsplat transformers used to model post-roll damage effects,
 * ported from the OSRS wiki DPS calculator.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Transforms {

    /**
     * Clamps damage into [minimum, maximum].
     * @param maximum The inclusive upper bound damage is clamped to
     * @param minimum The inclusive lower bound damage is clamped to
     * @return A transformer producing a single hitsplat with the clamped damage
     */
    public static HitTransformer flatLimit(int maximum, int minimum) {
        return h -> HitDistribution.single(1.0,
                new Hitsplat(Math.max(minimum, Math.min(h.getDamage(), maximum)), h.isAccurate()));
    }

    /**
     * Rerolls damage uniformly between offset and (maximum + offset), keeping the lower of
     * the rolled cap and the original damage. Used for effects like Verzik P1's damage cap.
     * @param maximum The exclusive-of-offset upper bound of the uniform reroll, rolled over [0, maximum]
     * @param offset The amount added to each rolled cap
     * @return A transformer producing the flattened distribution of capped damage
     */
    public static HitTransformer linearMin(int maximum, int offset) {
        return h -> {
            HitDistribution d = new HitDistribution();
            double prob = 1.0 / (maximum + 1);
            for (int i = 0; i <= maximum; i++) {
                d.addHit(new WeightedHit(prob,
                        WeightedHit.singleton(new Hitsplat(Math.min(h.getDamage(), i + offset), h.isAccurate()))));
            }
            return d.flatten();
        };
    }

    /**
     * When damage exceeds the limit, rerolls it uniformly in [offset, rollMax + offset].
     * Used for Zulrah's 50+ damage reroll.
     * @param limit The damage threshold above which the reroll triggers
     * @param rollMax The upper bound of the uniform reroll, rolled over [0, rollMax]
     * @param offset The amount added to each rolled value
     * @return A transformer that passes damage through unchanged at or below the limit, and otherwise rerolls it
     */
    public static HitTransformer cappedReroll(int limit, int rollMax, int offset) {
        return h -> {
            if (h.getDamage() <= limit) {
                return HitDistribution.single(1.0, h);
            }
            HitDistribution d = new HitDistribution();
            double prob = 1.0 / (rollMax + 1);
            for (int i = 0; i <= rollMax; i++) {
                d.addHit(new WeightedHit(prob, WeightedHit.singleton(new Hitsplat(i + offset, h.isAccurate()))));
            }
            return d.flatten();
        };
    }

    /**
     * Multiplies damage by numerator/divisor (truncating), optionally respecting a minimum.
     * @param numerator The multiplier's numerator
     * @param divisor The multiplier's divisor
     * @param minimum The damage floor to respect, or 0 to apply no floor
     * @return A transformer producing a single hitsplat with the scaled damage
     */
    public static HitTransformer multiply(int numerator, int divisor, int minimum) {
        return h -> {
            int dmg = (int) ((long) numerator * h.getDamage() / divisor);
            if (minimum != 0) {
                if (h.getDamage() >= minimum) {
                    dmg = Math.max(minimum, dmg);
                } else {
                    dmg = Math.max(h.getDamage(), dmg);
                }
            }
            return HitDistribution.single(1.0, new Hitsplat(dmg, h.isAccurate()));
        };
    }

    /**
     * Multiplies damage by numerator/divisor (truncating), with no minimum floor.
     * @param numerator The multiplier's numerator
     * @param divisor The multiplier's divisor
     * @return A transformer producing a single hitsplat with the scaled damage
     */
    public static HitTransformer multiply(int numerator, int divisor) {
        return multiply(numerator, divisor, 0);
    }

    /**
     * Divides damage by the given divisor (truncating), with no minimum floor.
     * @param divisor The divisor to scale damage by
     * @return A transformer producing a single hitsplat with the scaled damage
     */
    public static HitTransformer division(int divisor) {
        return multiply(1, divisor, 0);
    }

    /**
     * Divides damage by the given divisor (truncating), respecting a minimum floor.
     * @param divisor The divisor to scale damage by
     * @param minimum The damage floor to respect, or 0 to apply no floor
     * @return A transformer producing a single hitsplat with the scaled damage
     */
    public static HitTransformer division(int divisor, int minimum) {
        return multiply(1, divisor, minimum);
    }

    /**
     * Adds a flat amount of damage (may be negative), respecting a minimum floor.
     * @param addend The amount to add to the damage, which may be negative
     * @param minimum The damage floor to respect
     * @return A transformer producing a single hitsplat with the adjusted damage
     */
    public static HitTransformer flatAdd(int addend, int minimum) {
        return h -> HitDistribution.single(1.0,
                new Hitsplat(Math.max(minimum, h.getDamage() + addend), h.isAccurate()));
    }

    /**
     * Adds a flat amount of damage (may be negative), flooring the result at zero.
     * @param addend The amount to add to the damage, which may be negative
     * @return A transformer producing a single hitsplat with the adjusted damage
     */
    public static HitTransformer flatAdd(int addend) {
        return flatAdd(addend, 0);
    }
}
