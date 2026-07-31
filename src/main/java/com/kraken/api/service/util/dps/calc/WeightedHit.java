package com.kraken.api.service.util.dps.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * One outcome of an attack: a probability and the list of hitsplats dealt in that outcome.
 */
public final class WeightedHit {

    private final double probability;
    private final List<Hitsplat> hitsplats;
    private int cachedSum = -1;

    public WeightedHit(double probability, List<Hitsplat> hitsplats) {
        this.probability = probability;
        this.hitsplats = hitsplats;
    }

    public double getProbability() {
        return probability;
    }

    public List<Hitsplat> getHitsplats() {
        return hitsplats;
    }

    public WeightedHit scale(double factor) {
        return new WeightedHit(probability * factor, hitsplats);
    }

    public WeightedHit zip(WeightedHit other) {
        List<Hitsplat> combined = new ArrayList<>(hitsplats.size() + other.hitsplats.size());
        combined.addAll(hitsplats);
        combined.addAll(other.hitsplats);
        return new WeightedHit(probability * other.probability, combined);
    }

    /**
     * Applies a hit transformer to every hitsplat in this hit, producing a distribution of
     * outcomes weighted by this hit's probability.
     * @param transformer The per-hitsplat transformer
     * @param transformInaccurate Whether missed hitsplats should also be transformed
     * @return HitDistribution the transformed outcomes
     */
    public HitDistribution transform(HitTransformer transformer, boolean transformInaccurate) {
        if (hitsplats.size() == 1) {
            return transformSplat(hitsplats.get(0), transformer, transformInaccurate).scaleProbability(probability);
        }

        // recursively zip the first hitsplat's transform with the rest
        WeightedHit head = new WeightedHit(probability, hitsplats.subList(0, 1));
        WeightedHit tail = new WeightedHit(1.0, hitsplats.subList(1, hitsplats.size()));
        return head.transform(transformer, transformInaccurate).zip(tail.transform(transformer, transformInaccurate));
    }

    private static HitDistribution transformSplat(Hitsplat splat, HitTransformer transformer, boolean transformInaccurate) {
        if (!splat.isAccurate() && !transformInaccurate) {
            HitDistribution d = new HitDistribution();
            d.addHit(new WeightedHit(1.0, singleton(splat)));
            return d;
        }
        return transformer.transform(splat);
    }

    static List<Hitsplat> singleton(Hitsplat splat) {
        List<Hitsplat> list = new ArrayList<>(1);
        list.add(splat);
        return list;
    }

    public boolean anyAccurate() {
        for (Hitsplat h : hitsplats) {
            if (h.isAccurate()) return true;
        }
        return false;
    }

    public int getSum() {
        if (cachedSum < 0) {
            int sum = 0;
            for (Hitsplat h : hitsplats) {
                sum += h.getDamage();
            }
            cachedSum = sum;
        }
        return cachedSum;
    }

    public double getExpectedValue() {
        return probability * getSum();
    }

    /**
     * Computes a unique key for the exact hitsplat sequence, used to merge identical
     * outcomes. 15 bits of damage plus an accuracy bit per splat allows four hitsplats
     * per hit before keys could collide.
     * @return long hash of the hitsplat sequence
     */
    public long getHash() {
        long acc = 0L;
        for (Hitsplat h : hitsplats) {
            acc <<= 16;
            acc |= ((long) h.getDamage() & 0x7FFF) << 1;
            acc |= h.isAccurate() ? 1L : 0L;
        }
        return acc;
    }

    @Override
    public String toString() {
        return "WeightedHit(p=" + probability + ", sum=" + getSum() + ")";
    }
}
