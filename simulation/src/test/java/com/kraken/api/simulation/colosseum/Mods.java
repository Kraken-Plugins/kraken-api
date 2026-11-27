package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.hits.DamageModifiers;
import com.kraken.api.simulation.npc.PackRules;
import com.kraken.api.simulation.world.Frame;

/**
 * The active modifier set resolved to flat primitives, built once per frame so the tick pipeline never does tier
 * lookups. Modifier ids follow the modifier select script 4931 bitmask (community plugin); {@code mask} holds one bit
 * per id and {@code tiers} packs 2 bits per id. A field is zero or false when its modifier is inactive, so the pack
 * can read unconditionally. {@link #apply} hands the core its part: the damage model, the player's range penalty,
 * the sand damage and the max hp penalty.
 */
public final class Mods {
    public static final int MANTIMAYHEM = 0;
    public static final int REENTRY = 1;
    public static final int BEES = 2;
    public static final int VOLATILITY = 3;
    public static final int BLASPHEMY = 4;
    public static final int RELENTLESS = 5;
    public static final int QUARTET = 6;
    public static final int TOTEMIC = 7;
    public static final int DOOM = 8;
    public static final int DYNAMIC_DUO = 9;
    public static final int SOLARFLARE = 10;
    public static final int MYOPIA = 11;
    public static final int FRAILTY = 12;
    public static final int RED_FLAG = 13;
    public static final int COUNT = 14;

    public static final Mods NONE = new Mods();

    public int mask;
    public int tiers;

    public int myopiaRangeDelta;
    public int relentlessMaxHitBonus;
    /** Added to the unprayed expected damage factor (x1000); tier III replaces it. */
    public int relentlessFactorAdd1000;
    public boolean relentlessAlwaysHits;
    public int relentlessFactor1000Override;
    public int blasphemyDrainPct;
    /** 0 = Doom inactive; otherwise stacks at which the player dies. */
    public int doomCap;
    public int frailtyHpPct;

    public int beeSwarms;
    public int beeMoveEvery;
    public int beeDamage;
    public int beeExpectedDamage;
    public int beeRespawnTicks;

    public int mantimayhemTier;

    public int reentryTier;
    public int sandDamage;

    public int solarTier;
    /** Ticks between flare steps along a side, and on leaving a corner. */
    public int solarSideGap;
    public int solarCornerGap;
    public int solarDamage;
    public boolean solarDisablesPrayer;

    public boolean totemic;
    public int totemSpawnBelowPct;
    public int totemHp;
    public int totemHealPct;
    public int totemHealMax;
    public int totemFirstHeal;
    public int totemHealEvery;
    public int totemHealFlight;
    public int totemRespawnTicks;

    public int volatilityTier;
    public int volatilityRadiusDelta;
    public int volatilityMaxHit;
    public boolean volatilitySand;
    public boolean volatilityHitsNpcs;

    public boolean redFlag;
    public boolean quartet;
    public boolean dynamicDuo;

    /** The core's damage model for these modifiers. */
    public DamageModifiers damage = DamageModifiers.NONE;

    public boolean has(int id) {
        return (mask >>> id & 1) != 0;
    }

    /** Tier 1 to 3 for an active modifier, 0 otherwise. */
    public int tier(int id) {
        return has(id) ? Math.max(1, tiers >>> (id * 2) & 3) : 0;
    }

    /** Sets the frame's modifier context: the Colosseum part and the core's damage model and penalties. */
    public void apply(Frame f) {
        ColosseumFrame cf = ColosseumFrame.of(f);
        cf.mods = this;
        cf.modifierMask = mask;
        cf.modifierTiers = tiers;
        f.damage = damage;
        f.playerRangePenalty = myopiaRangeDelta;
        f.sandDamage = sandDamage;
        f.maxHpPenaltyPct = frailtyHpPct;
        // The flares step at tick start; bees and flares hurt on contact; totems spawn on damage. The end of tick
        // hook (totem ticks, reinforcements) runs on every Colosseum frame.
        f.packHooks = PackRules.HOOK_END_OF_TICK
                | (solarTier > 0 ? PackRules.HOOK_TICK_START : 0)
                | (beeSwarms > 0 || solarTier > 0 ? PackRules.HOOK_CONTACT : 0)
                | (totemic ? PackRules.HOOK_NPC_DAMAGED : 0);
    }

    public static Mods resolve(ModifierData d, int mask, int tiers) {
        Mods m = new Mods();
        m.mask = mask;
        m.tiers = tiers;
        if (m.has(MYOPIA)) {
            m.myopiaRangeDelta = d.myopia.rangeDelta[m.tier(MYOPIA) - 1];
        }
        if (m.has(RELENTLESS)) {
            int t = m.tier(RELENTLESS);
            m.relentlessMaxHitBonus = d.relentless.maxHitBonus[t - 1];
            m.relentlessFactorAdd1000 = d.relentless.expectedFactorAdd1000[t - 1];
            m.relentlessAlwaysHits = t == 3;
            m.relentlessFactor1000Override = d.relentless.tier3ExpectedFactor1000;
        }
        if (m.has(BLASPHEMY)) {
            m.blasphemyDrainPct = d.blasphemy.drainPctOfDamage[m.tier(BLASPHEMY) - 1];
        }
        if (m.has(DOOM)) {
            m.doomCap = d.doom.stackCap[m.tier(DOOM) - 1];
        }
        if (m.has(FRAILTY)) {
            m.frailtyHpPct = d.frailty.hpReductionPct[m.tier(FRAILTY) - 1];
        }
        if (m.has(BEES)) {
            int t = m.tier(BEES);
            m.beeSwarms = d.bees.swarms[t - 1];
            m.beeMoveEvery = d.bees.moveEveryTicks;
            m.beeDamage = d.bees.contactMaxDamage[t - 1];
            m.beeExpectedDamage = d.bees.contactExpectedDamage[t - 1];
            m.beeRespawnTicks = d.bees.respawnTicks;
        }
        m.mantimayhemTier = m.tier(MANTIMAYHEM);
        if (m.has(REENTRY)) {
            m.reentryTier = m.tier(REENTRY);
            m.sandDamage = d.reentry.sandDamagePerTick;
        }
        if (m.has(SOLARFLARE)) {
            int t = m.tier(SOLARFLARE);
            m.solarTier = t;
            m.solarSideGap = d.solarflare.sideGapTicks[t - 1];
            m.solarCornerGap = d.solarflare.cornerGapTicks[t - 1];
            m.solarDamage = d.solarflare.contactDamage[t - 1];
            m.solarDisablesPrayer = t == 3 && d.solarflare.tier3DisablesPrayers;
        }
        if (m.has(TOTEMIC)) {
            m.totemic = true;
            m.totemSpawnBelowPct = d.totemic.spawnBelowHpPct;
            m.totemHp = d.totemic.totemHp;
            m.totemHealPct = d.totemic.healPctOfMax;
            m.totemHealMax = d.totemic.healMax;
            m.totemFirstHeal = d.totemic.firstHealTicks;
            m.totemHealEvery = d.totemic.healEveryTicks;
            m.totemHealFlight = d.totemic.healFlightTicks;
            m.totemRespawnTicks = d.totemic.respawnTicks;
        }
        if (m.has(VOLATILITY)) {
            int t = m.tier(VOLATILITY);
            m.volatilityTier = t;
            m.volatilityRadiusDelta = d.volatility.radiusBeyondSize[t - 1];
            m.volatilityMaxHit = d.volatility.blastMaxHit;
            m.volatilitySand = t == 3 && d.volatility.tier3LeavesSand;
            m.volatilityHitsNpcs = d.volatility.damagesNpcs;
        }
        if (m.has(VOLATILITY) && m.volatilitySand) {
            m.sandDamage = Math.max(m.sandDamage, d.reentry.sandDamagePerTick);
        }
        m.redFlag = m.has(RED_FLAG);
        m.quartet = m.has(QUARTET);
        m.dynamicDuo = m.has(DYNAMIC_DUO);
        DamageModifiers dm = new DamageModifiers();
        dm.npcMaxHitBonus = m.relentlessMaxHitBonus;
        dm.unprayedFactorAdd1000 = m.relentlessFactorAdd1000;
        dm.unprayedFactorOverride1000 = m.relentlessAlwaysHits ? m.relentlessFactor1000Override : -1;
        dm.prayerDrainPctOfDamage = m.blasphemyDrainPct;
        dm.stackPerHit = m.doomCap > 0;
        m.damage = dm;
        return m;
    }

    /** Packs a tier into the 2 bit lane for an id; convenience for tests and the adapter. */
    public static int tierBits(int id, int tier) {
        return (tier & 3) << (id * 2);
    }
}
