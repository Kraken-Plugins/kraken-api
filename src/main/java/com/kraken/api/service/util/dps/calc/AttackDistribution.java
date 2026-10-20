package com.kraken.api.service.util.dps.calc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The full outcome distribution of one attack, potentially made of several independent
 * hit distributions (e.g. the three scythe hitsplats or both dark bow arrows).
 */
public class AttackDistribution {

    private final List<HitDistribution> dists;
    private HitDistribution singleHitsplat;

    /**
     * Creates an attack from its independent hit distributions. The list is stored without copying.
     * @param dists The hit distributions that make up the attack
     */
    public AttackDistribution(List<HitDistribution> dists) {
        this.dists = dists;
    }

    /**
     * Creates an attack from its independent hit distributions.
     * @param distributions The hit distributions that make up the attack
     * @return AttackDistribution the attack, with the distributions in the order given
     */
    public static AttackDistribution of(HitDistribution... distributions) {
        List<HitDistribution> list = new ArrayList<>();
        for (HitDistribution d : distributions) {
            list.add(d);
        }
        return new AttackDistribution(list);
    }

    /**
     * Gets the independent hit distributions that make up this attack.
     * @return the distributions in the order they were given
     */
    public List<HitDistribution> getDists() {
        return dists;
    }

    /**
     * Collapses all hit distributions into a single distribution of total damage per attack.
     * @return HitDistribution over the attack's cumulative damage
     */
    public HitDistribution getSingleHitsplat() {
        if (singleHitsplat == null) {
            HitDistribution acc = dists.get(0).cumulative();
            for (int i = 1; i < dists.size(); i++) {
                acc = acc.zip(dists.get(i)).cumulative();
            }
            singleHitsplat = acc;
        }
        return singleHitsplat;
    }

    /**
     * Applies a transformer to every hit distribution in this attack.
     * @param t The transformer applied to each hitsplat
     * @param transformInaccurate Whether missed hitsplats are transformed too
     * @return AttackDistribution the transformed attack
     */
    public AttackDistribution transform(HitTransformer t, boolean transformInaccurate) {
        return map(d -> d.transform(t, transformInaccurate));
    }

    /**
     * Applies a transformer to every hit distribution in this attack, including missed hitsplats.
     * @param t The transformer applied to each hitsplat
     * @return AttackDistribution the transformed attack
     */
    public AttackDistribution transform(HitTransformer t) {
        return transform(t, true);
    }

    /**
     * Multiplies the probability of every outcome in every hit distribution by a factor.
     * @param factor The factor applied to each probability
     * @return AttackDistribution the scaled attack
     */
    public AttackDistribution scaleProbability(double factor) {
        return map(d -> d.scaleProbability(factor));
    }

    /**
     * Scales the damage of every hitsplat in every hit distribution by a ratio, using integer arithmetic.
     * @param factor The numerator of the damage multiplier
     * @param divisor The denominator of the damage multiplier
     * @return AttackDistribution the attack with scaled damage
     */
    public AttackDistribution scaleDamage(int factor, int divisor) {
        return map(d -> d.scaleDamage(factor, divisor));
    }

    /**
     * Merges outcomes with identical hitsplat sequences in every hit distribution.
     * @return AttackDistribution the flattened attack
     */
    public AttackDistribution flatten() {
        return map(HitDistribution::flatten);
    }

    /**
     * Gets the highest total damage this attack can deal, summed over its hit distributions.
     * @return int the sum of each distribution's maximum damage
     */
    public int getMax() {
        int sum = 0;
        for (HitDistribution d : dists) {
            sum += d.getMax();
        }
        return sum;
    }

    /**
     * Gets the expected damage of this attack, summed over its hit distributions.
     * @return double the expected damage per attack
     */
    public double getExpectedDamage() {
        double sum = 0;
        for (HitDistribution d : dists) {
            sum += d.expectedHit();
        }
        return sum;
    }

    /**
     * Builds a histogram of total damage per attack, indexed by damage value.
     * @return double[] where index i holds the probability of dealing exactly i damage
     */
    public double[] asHistogram() {
        HitDistribution dist = getSingleHitsplat();
        double[] hist = new double[dist.getMax() + 1];
        for (WeightedHit h : dist.getHits()) {
            hist[h.getSum()] += h.getProbability();
        }
        return hist;
    }

    private AttackDistribution map(UnaryOperator<HitDistribution> mapper) {
        List<HitDistribution> out = new ArrayList<>(dists.size());
        for (HitDistribution d : dists) {
            out.add(mapper.apply(d));
        }
        return new AttackDistribution(out);
    }
}
