package com.kraken.api.service.util.dps.calc;

/**
 * Standard hitsplat transformers used to model post-roll damage effects,
 * ported from the OSRS wiki DPS calculator.
 */
public final class Transforms {

    private Transforms() {
    }

    /**
     * Clamps damage into [minimum, maximum].
     */
    public static HitTransformer flatLimit(int maximum, int minimum) {
        return h -> HitDistribution.single(1.0,
                new Hitsplat(Math.max(minimum, Math.min(h.getDamage(), maximum)), h.isAccurate()));
    }

    /**
     * Rerolls damage uniformly between offset and (maximum + offset), keeping the lower of
     * the rolled cap and the original damage. Used for effects like Verzik P1's damage cap.
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

    public static HitTransformer multiply(int numerator, int divisor) {
        return multiply(numerator, divisor, 0);
    }

    public static HitTransformer division(int divisor) {
        return multiply(1, divisor, 0);
    }

    public static HitTransformer division(int divisor, int minimum) {
        return multiply(1, divisor, minimum);
    }

    /**
     * Adds a flat amount of damage (may be negative), respecting a minimum floor.
     */
    public static HitTransformer flatAdd(int addend, int minimum) {
        return h -> HitDistribution.single(1.0,
                new Hitsplat(Math.max(minimum, h.getDamage() + addend), h.isAccurate()));
    }

    public static HitTransformer flatAdd(int addend) {
        return flatAdd(addend, 0);
    }
}
