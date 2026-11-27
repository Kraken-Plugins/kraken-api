package com.kraken.api.simulation.colosseum;

import java.util.Map;

/**
 * Raw per tier modifier parameters from colosseum/modifiers.json
 * (https://oldschool.runescape.wiki/w/Fortis_Colosseum/Modifiers). Consumed once by {@link Mods#resolve}; the pack
 * only ever reads the resolved {@link Mods}.
 */
public final class ModifierData {
    public Myopia myopia;
    public Relentless relentless;
    public Blasphemy blasphemy;
    public Doom doom;
    public Frailty frailty;
    public Bees bees;
    public Reentry reentry;
    public Solarflare solarflare;
    public Totemic totemic;
    public Volatility volatility;
    public Map<String, String> src;

    public static final class Myopia {
        public int[] rangeDelta;
    }

    public static final class Relentless {
        public int[] maxHitBonus;
        public int[] expectedFactorAdd1000;
        public int tier3ExpectedFactor1000;
    }

    public static final class Blasphemy {
        public int[] drainPctOfDamage;
    }

    public static final class Doom {
        public int[] stackCap;
    }

    public static final class Frailty {
        public int[] hpReductionPct;
    }

    public static final class Bees {
        public int[] swarms;
        public int moveEveryTicks;
        public int[] contactMaxDamage;
        public int[] contactExpectedDamage;
        public int respawnTicks;
    }

    public static final class Reentry {
        public int sandDamagePerTick;
    }

    public static final class Solarflare {
        public int[] sideGapTicks;
        public int[] cornerGapTicks;
        public int[] contactDamage;
        public boolean tier3DisablesPrayers;
    }

    public static final class Totemic {
        public int spawnBelowHpPct;
        public int totemHp;
        public int healPctOfMax;
        public int healMax;
        public int firstHealTicks;
        public int healEveryTicks;
        public int healFlightTicks;
        public int respawnTicks;
    }

    public static final class Volatility {
        public int[] radiusBeyondSize;
        public int blastMaxHit;
        public boolean tier3LeavesSand;
        public boolean damagesNpcs;
    }
}
