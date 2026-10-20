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

        /**
         * Creates the context shared by the bolt effect transformers.
         * @param rangedLvl The player's effective ranged level
         * @param maxHit The attack's maximum hit before bolt effects are applied
         * @param zcb Whether the Zaryte crossbow is equipped, which changes the bonus divisors and effect caps
         * @param kandarinDiary Whether the Kandarin diary bonus applies, which raises each proc chance by 10%
         * @param monster The monster being attacked
         */
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

    /**
     * Creates the transformer for opal bolts. Each hitsplat has a 5% chance, raised by the Kandarin diary bonus, to gain a bonus equal to the ranged level divided by 10, or by 9 with the Zaryte crossbow.
     * @param ctx The shared bolt context
     * @return HitTransformer the opal bolt effect
     */
    public static HitTransformer opalBolts(BoltContext ctx) {
        double chance = 0.05 * kandarinFactor(ctx);
        int bonusDmg = ctx.rangedLvl / (ctx.zcb ? 9 : 10);
        return bonusDamage(chance, bonusDmg, false);
    }

    /**
     * Creates the transformer for pearl bolts. Each hitsplat has a 6% chance, raised by the Kandarin diary bonus, to gain a bonus equal to the ranged level divided by 20, or by 15 against fiery monsters. With the Zaryte crossbow the divisor is 2 lower.
     * @param ctx The shared bolt context
     * @return HitTransformer the pearl bolt effect
     */
    public static HitTransformer pearlBolts(BoltContext ctx) {
        double chance = 0.06 * kandarinFactor(ctx);
        int divisor = ctx.monster.hasAttribute(MonsterAttributeType.FIERY) ? 15 : 20;
        int bonusDmg = ctx.rangedLvl / (ctx.zcb ? divisor - 2 : divisor);
        return bonusDamage(chance, bonusDmg, false);
    }

    /**
     * Creates the transformer for diamond bolts. Each hitsplat has a 10% chance, raised by the Kandarin diary bonus, to be replaced by a hit that deals a uniform random 0 to 115% of the maximum hit, or 126% with the Zaryte crossbow. Otherwise the original hitsplat is kept.
     * @param ctx The shared bolt context
     * @return HitTransformer the diamond bolt effect
     */
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

    /**
     * Creates the transformer for dragonstone bolts. Monsters with the fiery or dragon attribute are immune, so their hitsplats are unchanged. Otherwise each accurate hitsplat has a 6% chance, raised by the Kandarin diary bonus, to gain a bonus equal to twice the ranged level divided by 10, or by 9 with the Zaryte crossbow. Missed hitsplats are not changed.
     * @param ctx The shared bolt context
     * @return HitTransformer the dragonstone bolt effect
     */
    public static HitTransformer dragonstoneBolts(BoltContext ctx) {
        if (ctx.monster.hasAttribute(MonsterAttributeType.FIERY) || ctx.monster.hasAttribute(MonsterAttributeType.DRAGON)) {
            // immune to dragonfire
            return h -> HitDistribution.single(1.0, h);
        }

        double chance = 0.06 * kandarinFactor(ctx);
        int bonusDmg = ctx.rangedLvl * 2 / (ctx.zcb ? 9 : 10);
        return bonusDamage(chance, bonusDmg, true);
    }

    /**
     * Creates the transformer for onyx bolts. Undead monsters are immune. Otherwise each accurate hitsplat has an 11% chance, raised by the Kandarin diary bonus, to be replaced by a hit that deals a uniform random 0 to 120% of the maximum hit, or 132% with the Zaryte crossbow. Missed hitsplats are not changed.
     * @param ctx The shared bolt context
     * @return HitTransformer the onyx bolt effect
     */
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

    /**
     * Creates the transformer for ruby bolts. Each hitsplat has a 6% chance, raised by the Kandarin diary bonus, to be replaced by a fixed hit equal to 20% of the monster's current health, or 22% with the Zaryte crossbow. That hit is capped at 100, or 110 with the Zaryte crossbow, and at 60 or 66 for monsters with infinite health. When the current health is unknown, the monster's maximum health is used.
     * @param ctx The shared bolt context
     * @return HitTransformer the ruby bolt effect
     */
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
