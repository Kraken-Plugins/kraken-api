package com.kraken.api.service.util.dps.calc;

import com.kraken.api.service.util.dps.model.MonsterAttributeType;
import com.kraken.api.service.util.dps.model.MonsterData;

import java.util.ArrayList;
import java.util.List;

/**
 * Enchanted bolt proc effects, ported from the OSRS wiki DPS calculator's bolts.ts.
 * Special-attack (Zaryte crossbow spec) branches are omitted since the calculator
 * models normal attacks only.
 */
public final class BoltEffects {

    private BoltEffects() {
    }

    /** Inputs shared by the bolt effect transformers. */
    public static class BoltContext {
        private final int rangedLvl;
        private final int maxHit;
        private final boolean zcb;
        private final boolean kandarinDiary;
        private final MonsterData monster;

        public BoltContext(int rangedLvl, int maxHit, boolean zcb, boolean kandarinDiary, MonsterData monster) {
            this.rangedLvl = rangedLvl;
            this.maxHit = maxHit;
            this.zcb = zcb;
            this.kandarinDiary = kandarinDiary;
            this.monster = monster;
        }
    }

    private static double kandarinFactor(BoltContext ctx) {
        return ctx.kandarinDiary ? 1.1 : 1.0;
    }

    private static HitTransformer bonusDamage(double chance, int bonusDmg, boolean accurateOnly) {
        return h -> {
            if (!h.isAccurate() && accurateOnly) {
                return HitDistribution.single(1.0, h);
            }
            List<WeightedHit> hits = new ArrayList<>(2);
            hits.add(new WeightedHit(chance, WeightedHit.singleton(new Hitsplat(h.getDamage() + bonusDmg, h.isAccurate()))));
            hits.add(new WeightedHit(1 - chance, WeightedHit.singleton(new Hitsplat(h.getDamage(), h.isAccurate()))));
            return new HitDistribution(hits);
        };
    }

    public static HitTransformer opalBolts(BoltContext ctx) {
        double chance = 0.05 * kandarinFactor(ctx);
        int bonusDmg = ctx.rangedLvl / (ctx.zcb ? 9 : 10);
        return bonusDamage(chance, bonusDmg, false);
    }

    public static HitTransformer pearlBolts(BoltContext ctx) {
        double chance = 0.06 * kandarinFactor(ctx);
        int divisor = ctx.monster.hasAttribute(MonsterAttributeType.FIERY) ? 15 : 20;
        int bonusDmg = ctx.rangedLvl / (ctx.zcb ? divisor - 2 : divisor);
        return bonusDamage(chance, bonusDmg, false);
    }

    public static HitTransformer diamondBolts(BoltContext ctx) {
        double chance = 0.1 * kandarinFactor(ctx);
        int effectMax = ctx.maxHit * (ctx.zcb ? 126 : 115) / 100;
        HitDistribution effectDist = HitDistribution.linear(1.0, 0, effectMax);

        return h -> {
            HitDistribution out = new HitDistribution();
            out.addHits(effectDist.scaleProbability(chance).getHits());
            out.addHit(new WeightedHit(1 - chance, WeightedHit.singleton(new Hitsplat(h.getDamage(), h.isAccurate()))));
            return out;
        };
    }

    public static HitTransformer dragonstoneBolts(BoltContext ctx) {
        if (ctx.monster.hasAttribute(MonsterAttributeType.FIERY) || ctx.monster.hasAttribute(MonsterAttributeType.DRAGON)) {
            // immune to dragonfire
            return h -> HitDistribution.single(1.0, h);
        }

        double chance = 0.06 * kandarinFactor(ctx);
        int bonusDmg = ctx.rangedLvl * 2 / (ctx.zcb ? 9 : 10);
        return bonusDamage(chance, bonusDmg, true);
    }

    public static HitTransformer onyxBolts(BoltContext ctx) {
        if (ctx.monster.hasAttribute(MonsterAttributeType.UNDEAD)) {
            // immune to life leech
            return h -> HitDistribution.single(1.0, h);
        }

        double chance = 0.11 * kandarinFactor(ctx);
        int effectMax = ctx.maxHit * (ctx.zcb ? 132 : 120) / 100;
        HitDistribution effectDist = HitDistribution.linear(1.0, 0, effectMax);

        return h -> {
            if (!h.isAccurate()) {
                return HitDistribution.single(1.0, h);
            }
            HitDistribution out = new HitDistribution();
            out.addHits(effectDist.scaleProbability(chance).getHits());
            out.addHit(new WeightedHit(1 - chance, WeightedHit.singleton(new Hitsplat(h.getDamage(), h.isAccurate()))));
            return out;
        };
    }

    public static HitTransformer rubyBolts(BoltContext ctx) {
        double chance = 0.06 * kandarinFactor(ctx);
        int cap;
        if (DpsConstants.INFINITE_HEALTH_MONSTERS.contains(ctx.monster.getId())) {
            cap = ctx.zcb ? 66 : 60;
        } else {
            cap = ctx.zcb ? 110 : 100;
        }

        int effectDmg = ctx.monster.getCurrentHpOrFull() * (ctx.zcb ? 22 : 20) / 100;
        Hitsplat effectHit = new Hitsplat(Math.min(cap, effectDmg));

        return h -> {
            HitDistribution out = new HitDistribution();
            out.addHit(new WeightedHit(chance, WeightedHit.singleton(effectHit)));
            out.addHit(new WeightedHit(1 - chance, WeightedHit.singleton(new Hitsplat(h.getDamage(), h.isAccurate()))));
            return out;
        };
    }
}
