package com.kraken.api.simulation.colosseum;

/** A type's "colosseum" block in npcs.json: the per behaviour settings, at most one of them set. */
public final class ColosseumNpc {
    public Manticore manticore;
    public Javelin javelin;
    public Minotaur minotaur;
    public Warband warband;

    public static final class Manticore {
        public int chargeTicks = 10;
        public int staggerTicks = 5;
        public int maxHitRanged = 36;
        public int maxHitMagic = 31;
        public int maxHitMelee = 31;
        /** Charge animation ids: the first orb leaves {@link #chargeToThrowTicks} later. */
        public int[] volleyTelegraphAnims = {};
        public int chargeToThrowTicks = 6;
        /** Throw animation ids, played on the tick the first orb leaves. */
        public int[] volleyThrowAnims = {};
    }

    public static final class Javelin {
        public int specialEvery = 5;
        public int landDelayTicks = 6;
        /** Ticks from the tile pick (map animations 1446 and 2674/2677 on the player's tile) to the blast: the toss
         *  picks its tile landDelayTicks - shadowToLandTicks after the animation. */
        public int shadowToLandTicks = 3;
        public int skyMaxHit = 40;
        /** Share of the toss's max taken as its expected damage, x1000. */
        public int skyExpectedFactor1000 = 500;
    }

    public static final class Minotaur {
        public int healRange = 7;
        public int healBelowPct = 75;
        /** A heal fills the target's bar and holds the minotaur this many ticks. */
        public int healHoldTicks = 10;
    }

    /** Fremennik warband member: attacks only on its tick of a cycle fixed at spawn, and only from its own side of
     *  the player. */
    public static final class Warband {
        /** First possible attack at spawn + cycleOffset, then every attackSpeed ticks. */
        public int cycleOffset = 3;
        public int slotDx;
        public int slotDy;
        /** Side taken when its own is statically blocked (south for every member). */
        public int fallbackDx = 0;
        public int fallbackDy = -1;
    }
}
