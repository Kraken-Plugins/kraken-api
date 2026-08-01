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

    public AttackDistribution(List<HitDistribution> dists) {
        this.dists = dists;
    }

    public static AttackDistribution of(HitDistribution... distributions) {
        List<HitDistribution> list = new ArrayList<>();
        for (HitDistribution d : distributions) {
            list.add(d);
        }
        return new AttackDistribution(list);
    }

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

    public AttackDistribution transform(HitTransformer t, boolean transformInaccurate) {
        return map(d -> d.transform(t, transformInaccurate));
    }

    public AttackDistribution transform(HitTransformer t) {
        return transform(t, true);
    }

    public AttackDistribution scaleProbability(double factor) {
        return map(d -> d.scaleProbability(factor));
    }

    public AttackDistribution scaleDamage(int factor, int divisor) {
        return map(d -> d.scaleDamage(factor, divisor));
    }

    public AttackDistribution flatten() {
        return map(HitDistribution::flatten);
    }

    public int getMax() {
        int sum = 0;
        for (HitDistribution d : dists) {
            sum += d.getMax();
        }
        return sum;
    }

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
