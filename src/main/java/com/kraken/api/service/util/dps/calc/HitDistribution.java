package com.kraken.api.service.util.dps.calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A probability distribution over the outcomes of a single attack roll.
 * Ported from the OSRS wiki DPS calculator's HitDist.ts.
 */
public class HitDistribution {

    private final List<WeightedHit> hits;

    public HitDistribution() {
        this.hits = new ArrayList<>();
    }

    public HitDistribution(List<WeightedHit> hits) {
        this.hits = hits;
    }

    public List<WeightedHit> getHits() {
        return hits;
    }

    public void addHit(WeightedHit hit) {
        hits.add(hit);
    }

    public void addHits(List<WeightedHit> newHits) {
        hits.addAll(newHits);
    }

    /**
     * Combines this distribution with another into the joint distribution of both hits landing
     * on the same attack (cross product of outcomes).
     * @param other The other distribution
     * @return HitDistribution the joint distribution
     */
    public HitDistribution zip(HitDistribution other) {
        List<WeightedHit> out = new ArrayList<>(hits.size() * other.hits.size());
        for (WeightedHit a : hits) {
            for (WeightedHit b : other.hits) {
                out.add(a.zip(b));
            }
        }
        return new HitDistribution(out);
    }

    /**
     * Applies a transformer to every hitsplat of every outcome without merging duplicates.
     * @param t The transformer
     * @param transformInaccurate Whether missed hitsplats are transformed too
     * @return HitDistribution the transformed distribution
     */
    public HitDistribution wideTransform(HitTransformer t, boolean transformInaccurate) {
        HitDistribution d = new HitDistribution();
        for (WeightedHit h : hits) {
            d.addHits(h.transform(t, transformInaccurate).getHits());
        }
        return d;
    }

    public HitDistribution transform(HitTransformer t, boolean transformInaccurate) {
        return wideTransform(t, transformInaccurate).flatten();
    }

    public HitDistribution transform(HitTransformer t) {
        return transform(t, true);
    }

    public HitDistribution scaleProbability(double factor) {
        List<WeightedHit> out = new ArrayList<>(hits.size());
        for (WeightedHit h : hits) {
            out.add(h.scale(factor));
        }
        return new HitDistribution(out);
    }

    public HitDistribution scaleDamage(int factor, int divisor) {
        List<WeightedHit> out = new ArrayList<>(hits.size());
        for (WeightedHit h : hits) {
            List<Hitsplat> splats = new ArrayList<>(h.getHitsplats().size());
            for (Hitsplat s : h.getHitsplats()) {
                splats.add(new Hitsplat((int) ((long) s.getDamage() * factor / divisor), s.isAccurate()));
            }
            out.add(new WeightedHit(h.getProbability(), splats));
        }
        return new HitDistribution(out);
    }

    /**
     * Merges the probabilities of outcomes with identical hitsplat sequences.
     * @return HitDistribution the flattened distribution
     */
    public HitDistribution flatten() {
        Map<Long, Double> probs = new HashMap<>();
        Map<Long, List<Hitsplat>> splats = new HashMap<>();
        for (WeightedHit hit : hits) {
            long hash = hit.getHash();
            Double prev = probs.get(hash);
            if (prev == null) {
                probs.put(hash, hit.getProbability());
                splats.put(hash, hit.getHitsplats());
            } else {
                probs.put(hash, prev + hit.getProbability());
            }
        }

        HitDistribution d = new HitDistribution();
        for (Map.Entry<Long, Double> e : probs.entrySet()) {
            if (e.getValue() > 0) {
                d.addHit(new WeightedHit(e.getValue(), splats.get(e.getKey())));
            }
        }
        return d;
    }

    /**
     * Converts multi-hitsplat outcomes into single cumulative damage totals. A hit counts
     * as accurate when any of its hitsplats was accurate.
     * @return HitDistribution with one hitsplat per outcome
     */
    public HitDistribution cumulative() {
        Map<Integer, Double> acc = new HashMap<>();
        for (WeightedHit hit : hits) {
            // bitwise-invert the key for fully inaccurate hits so they stay distinct from accurate totals
            int key = hit.anyAccurate() ? hit.getSum() : ~hit.getSum();
            acc.merge(key, hit.getProbability(), Double::sum);
        }

        HitDistribution d = new HitDistribution();
        for (Map.Entry<Integer, Double> e : acc.entrySet()) {
            boolean accurate = e.getKey() >= 0;
            int dmg = accurate ? e.getKey() : ~e.getKey();
            if (e.getValue() > 0) {
                d.addHit(new WeightedHit(e.getValue(), WeightedHit.singleton(new Hitsplat(dmg, accurate))));
            }
        }
        return d;
    }

    public double expectedHit() {
        double sum = 0;
        for (WeightedHit h : hits) {
            sum += h.getExpectedValue();
        }
        return sum;
    }

    public int getMax() {
        int max = 0;
        for (WeightedHit h : hits) {
            max = Math.max(max, h.getSum());
        }
        return max;
    }

    public int getMin() {
        int min = Integer.MAX_VALUE;
        for (WeightedHit h : hits) {
            min = Math.min(min, h.getSum());
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    /**
     * Creates the standard OSRS damage distribution: an accuracy roll followed by a
     * uniform damage roll between min and max inclusive.
     * @param accuracy Chance for the attack to land (0-1)
     * @param minimum Minimum damage on a successful hit
     * @param maximum Maximum damage on a successful hit
     * @return HitDistribution the linear distribution
     */
    public static HitDistribution linear(double accuracy, int minimum, int maximum) {
        HitDistribution d = new HitDistribution();
        double hitProb = accuracy / (maximum - minimum + 1);
        for (int i = minimum; i <= maximum; i++) {
            d.addHit(new WeightedHit(hitProb, WeightedHit.singleton(new Hitsplat(i))));
        }
        d.addHit(new WeightedHit(1 - accuracy, WeightedHit.singleton(Hitsplat.INACCURATE)));
        return d;
    }

    /**
     * Creates a distribution with a single successful outcome (plus the miss outcome when
     * accuracy is below 1).
     * @param accuracy Chance for the attack to land (0-1)
     * @param hitsplats The hitsplats dealt on success
     * @return HitDistribution the resulting distribution
     */
    public static HitDistribution single(double accuracy, List<Hitsplat> hitsplats) {
        HitDistribution d = new HitDistribution();
        d.addHit(new WeightedHit(accuracy, hitsplats));
        if (accuracy != 1.0) {
            d.addHit(new WeightedHit(1 - accuracy, WeightedHit.singleton(Hitsplat.INACCURATE)));
        }
        return d;
    }

    public static HitDistribution single(double accuracy, Hitsplat hitsplat) {
        return single(accuracy, WeightedHit.singleton(hitsplat));
    }
}
