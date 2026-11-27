package com.kraken.api.simulation.world;

import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;

import java.util.Arrays;

/**
 * Mutable simulation state: player scalars plus NPC struct of arrays, pooled and copied with {@link #copyFrom}.
 * Everything immutable per snapshot lives on {@link Frame}, shared by reference. Sized so a copy is a few
 * arraycopies and no allocation.
 *
 * <p>Content packs keep their own per NPC state in four byte {@link #npcLane lanes} and four {@code FLAG_PACK_*}
 * bits, their world state in {@link #packLane}, and their own accumulators in {@link #accLane}; each pack documents
 * what it keeps where. Every lane starts at 0.
 */
public final class SimState {
    /** Number of NPC slots. */
    public static final int MAX_NPCS = 16;
    /** Capacity of the in flight hit ring. */
    public static final int PENDING_HITS = 32;
    /** Number of per NPC pack byte lanes. */
    public static final int NPC_LANES = 4;
    /** Number of pack world state lanes. */
    public static final int PACK_LANES = 4;
    /** Number of pack accumulator lanes. */
    public static final int ACC_LANES = 8;

    /** NPC flag: the slot holds a living NPC. */
    public static final byte FLAG_ALIVE = 1;
    /** NPC flag bit reserved for the pack's own use. */
    public static final byte FLAG_PACK_0 = 2;
    /** NPC flag: the NPC stepped this tick. */
    public static final byte FLAG_MOVED = 4;
    /** NPC flag bit reserved for the pack's own use. */
    public static final byte FLAG_PACK_1 = 8;
    /** A slot the engine spawned (a scheduled spawn, a pool slot) whose one spawn has been consumed. */
    public static final byte FLAG_SPAWNED = 16;
    /** NPC flag bit reserved for the pack's own use. */
    public static final byte FLAG_PACK_2 = 32;
    /** NPC flag bit reserved for the pack's own use. */
    public static final byte FLAG_PACK_3 = 64;

    /** Sentinel spawn tick: the slot's spawn relative gates are open (unknown anchor). */
    public static final short SPAWN_OPEN = Short.MIN_VALUE;

    /** The immutable snapshot identity this state belongs to. */
    public Frame frame;

    /** Packed region local tile ({@code x | y << 6}), or Coords.NONE. */
    public short playerPos = Coords.NONE;
    /** Player hit points in the expected damage channel. */
    public short hpExpected;
    /** Lowest player hit points the worst case channel has reached. */
    public short hpWorstFloor;
    /** Player prayer points in the expected channel. */
    public short prayerExpected;
    /** Lowest player prayer points the worst case channel has reached. */
    public short prayerWorst;
    /** Run energy in hundredths of a percent, 0 to 10000. */
    public short runEnergy100 = 10000;
    /** Special attack energy in tenths of a percent, 0 to 1000. */
    public short specEnergy10 = 1000;
    /** Overhead protection: 0 none, 1 melee, 2 missiles, 3 magic. */
    public byte overhead;
    /** Index of the worn gear set in the frame's equip sets. */
    public byte gearSet;
    /** Ticks until the player may attack again. */
    public byte attackCooldown;
    /** Ticks until the next food may be eaten. */
    public byte foodDelay;
    /** Ticks until the next potion may be drunk. */
    public byte potDelay;
    /** Ticks until the next karambwan may be eaten. */
    public byte karamDelay;
    /** Slot of the NPC the player is attacking, or -1 for none. */
    public byte targetSlot = -1;
    /** True while the player runs (two tiles per tick) instead of walking. */
    public boolean running = true;
    /** Packed click destination while pathing, or Coords.NONE. */
    public short moveDest = Coords.NONE;
    /** Path identity: click time source {@code << 12 |} destination; -1 when not pathing. */
    public int pathKey = -1;
    /** Cache hint into {@link Scratch}'s path table; revalidated against pathKey on use. */
    public short pathId = -1;
    /** Index of the next tile to walk on the current path. */
    public byte pathCursor;
    /** Stacks gained per damage instance under {@code DamageModifiers.stackPerHit}, worst case. */
    public byte hitStacksWorst;
    /** Packed venom: bit 0 active, bits 1 to 6 next damage, bits 7 to 13 ticks until it lands. */
    public int venomState;
    /** Food items left. */
    public byte foodCount;
    /** Karambwan left. */
    public byte karamCount;
    /** Brew doses left. */
    public byte brewDoses;
    /** Prayer restore doses left. */
    public byte restoreDoses;
    /** True when the next attack is a special attack. */
    public boolean specArmed;
    /** Ticks since the last special attack regeneration step. */
    public byte specRegenCounter;
    /** Prayer drain accumulated toward the next prayer point. */
    public short prayerDrainAcc;
    /** Per tick transients, reset at tick start: worst case burst and expected damage this tick. */
    public short tickBurstWorst;
    /** Expected channel damage landed on the player this tick. */
    public short tickExpectedDamage;
    /** Styles launched at the player this tick (bit per style); more than 1 bit = off tick alignment failure. */
    public byte tickStyleMask;
    /** Ranged and magic attackers (range above 1) with sight of the player this tick. */
    public byte tickLosExposure;
    /** Overhead switched on this tick (the activation tick itself does not drain). */
    public boolean prayerJustActivated;
    /** The command changed the overhead this tick: a flick that could land late live. */
    public boolean prayerSwitched;
    /** Worst damage launched at the player this tick per style (ranged, magic, melee). */
    public short tickRangedWorst;
    /** Worst magic damage launched at the player this tick. */
    public short tickMagicWorst;
    /** Worst melee damage launched at the player this tick. */
    public short tickMeleeWorst;
    /** Wave tick on which the wave cleared, or -1 while it is open. */
    public short waveClearTick = -1;

    /** Packed footprint anchor tile per slot. */
    public final short[] npcPos = new short[MAX_NPCS];
    /** Hit points per slot. */
    public final short[] npcHp = new short[MAX_NPCS];
    /** Ticks until the next attack per slot. */
    public final byte[] npcCooldown = new byte[MAX_NPCS];
    /** The {@code FLAG_*} bits per slot. */
    public final byte[] npcFlags = new byte[MAX_NPCS];
    /** Per NPC pack state, {@code npcLane[lane][slot]}; each pack documents its lanes. */
    public final byte[][] npcLane = new byte[NPC_LANES][MAX_NPCS];
    /**
     * Wave tick each NPC became visible: the anchor for its spawn relative action gates (first step at
     * +gateMoveTick, hold on sight and attack at +gateLosTick and +gateAttackTick); {@link #SPAWN_OPEN} when no
     * anchor is known and the gates do not apply.
     */
    public final short[] npcSpawnTick = new short[MAX_NPCS];
    /**
     * Ticks until a dead NPC's body is deleted, counted down on its turn; while above zero the corpse still blocks
     * other NPCs. Zero for the living.
     */
    public final byte[] npcCorpse = new byte[MAX_NPCS];

    /**
     * Dynamic NPC footprint bitboard, maintained incrementally as NPCs move. NPCs that move through others
     * (passThrough) neither block nor are blocked, so they are never on it.
     */
    public final long[] occupancy = new long[Grid.SIZE];
    /** Molten sand, the only board that mutates mid wave. */
    public final long[] moltenSand = new long[Grid.SIZE];

    /** In flight hits in the {@code Hits} encoding; the first {@link #pendingCount} are live. */
    public final long[] pendingHits = new long[PENDING_HITS];
    /** Number of in flight hits. */
    public byte pendingCount;

    /** Pack world state (hazard clocks, counters); each pack documents its lanes. */
    public final int[] packLane = new int[PACK_LANES];
    /** Wave relative tick index: number of completed simulated ticks since wave start. */
    public int tick;

    /** Expected channel damage the player has taken. */
    public int accExpectedDamageTaken;
    /** Damage the player has dealt to NPCs. */
    public int accDamageDealt;
    /** Kills of NPCs that hold the wave open. */
    public int accKills;
    /** Prayer points drained by protection and offensive prayers. */
    public int accPrayerDrained;
    /** Packed dose counts: food | karambwan {@code << 8} | brew {@code << 16} | restore {@code << 24}. */
    public int accSuppliesUsed;
    /** True once the expected channel hit points reached zero. */
    public boolean died;
    /** Wave tick on which the player died; meaningful when {@link #died}. */
    public short deathTick;
    /** Ticks on which attacks of two or more styles launched at the player. */
    public short accAlignmentEvents;
    /** NPC ticks where a living ranged or magic attacker (range above 1) had sight of the player. */
    public short accLosExposureTicks;
    /**
     * Per tick, the worst launched damage one protection prayer could not cover (the sum over styles minus the
     * largest), summed: what a stack that is not off ticked risks.
     */
    public int accResidualWorst;
    /** {@code MechanicsData.killValue} of every NPC killed. */
    public int accKillValue;
    /** Damage dealt to NPCs x1000, weighted per type by {@code MechanicsData.damageWeight1000}. */
    public int accThreatDamage1000;
    /** Hitpoints NPCs were healed x1000, weighted the same way: damage undone. */
    public int accThreatHealed1000;
    /** Pack and planner accumulators, zeroed with the rest by {@link #resetAccumulators}. */
    public final int[] accLane = new int[ACC_LANES];

    /**
     * Whether a slot's NPC is alive.
     * @param slot the slot
     * @return true while alive
     */
    public boolean alive(int slot) {
        return (npcFlags[slot] & FLAG_ALIVE) != 0;
    }

    /**
     * True while a dead NPC's body is still in the scene and blocking.
     * @param slot the slot
     * @return true while a corpse
     */
    public boolean corpse(int slot) {
        return npcCorpse[slot] > 0;
    }

    /**
     * Overwrites this state with another, allocating nothing.
     * @param o the state to copy
     */
    public void copyFrom(SimState o) {
        frame = o.frame;
        playerPos = o.playerPos;
        hpExpected = o.hpExpected;
        hpWorstFloor = o.hpWorstFloor;
        prayerExpected = o.prayerExpected;
        prayerWorst = o.prayerWorst;
        runEnergy100 = o.runEnergy100;
        specEnergy10 = o.specEnergy10;
        overhead = o.overhead;
        gearSet = o.gearSet;
        attackCooldown = o.attackCooldown;
        foodDelay = o.foodDelay;
        potDelay = o.potDelay;
        karamDelay = o.karamDelay;
        targetSlot = o.targetSlot;
        running = o.running;
        moveDest = o.moveDest;
        pathKey = o.pathKey;
        pathId = o.pathId;
        pathCursor = o.pathCursor;
        hitStacksWorst = o.hitStacksWorst;
        venomState = o.venomState;
        foodCount = o.foodCount;
        karamCount = o.karamCount;
        brewDoses = o.brewDoses;
        restoreDoses = o.restoreDoses;
        specArmed = o.specArmed;
        specRegenCounter = o.specRegenCounter;
        prayerDrainAcc = o.prayerDrainAcc;
        tickBurstWorst = o.tickBurstWorst;
        tickExpectedDamage = o.tickExpectedDamage;
        tickStyleMask = o.tickStyleMask;
        tickLosExposure = o.tickLosExposure;
        prayerJustActivated = o.prayerJustActivated;
        prayerSwitched = o.prayerSwitched;
        tickRangedWorst = o.tickRangedWorst;
        tickMagicWorst = o.tickMagicWorst;
        tickMeleeWorst = o.tickMeleeWorst;
        waveClearTick = o.waveClearTick;
        System.arraycopy(o.npcPos, 0, npcPos, 0, MAX_NPCS);
        System.arraycopy(o.npcHp, 0, npcHp, 0, MAX_NPCS);
        System.arraycopy(o.npcCooldown, 0, npcCooldown, 0, MAX_NPCS);
        System.arraycopy(o.npcFlags, 0, npcFlags, 0, MAX_NPCS);
        for (int lane = 0; lane < NPC_LANES; lane++) {
            System.arraycopy(o.npcLane[lane], 0, npcLane[lane], 0, MAX_NPCS);
        }
        System.arraycopy(o.npcSpawnTick, 0, npcSpawnTick, 0, MAX_NPCS);
        System.arraycopy(o.npcCorpse, 0, npcCorpse, 0, MAX_NPCS);
        System.arraycopy(o.occupancy, 0, occupancy, 0, Grid.SIZE);
        System.arraycopy(o.moltenSand, 0, moltenSand, 0, Grid.SIZE);
        System.arraycopy(o.pendingHits, 0, pendingHits, 0, PENDING_HITS);
        pendingCount = o.pendingCount;
        System.arraycopy(o.packLane, 0, packLane, 0, PACK_LANES);
        tick = o.tick;
        accExpectedDamageTaken = o.accExpectedDamageTaken;
        accDamageDealt = o.accDamageDealt;
        accKills = o.accKills;
        accPrayerDrained = o.accPrayerDrained;
        accSuppliesUsed = o.accSuppliesUsed;
        died = o.died;
        deathTick = o.deathTick;
        accAlignmentEvents = o.accAlignmentEvents;
        accLosExposureTicks = o.accLosExposureTicks;
        accResidualWorst = o.accResidualWorst;
        accKillValue = o.accKillValue;
        accThreatDamage1000 = o.accThreatDamage1000;
        accThreatHealed1000 = o.accThreatHealed1000;
        System.arraycopy(o.accLane, 0, accLane, 0, ACC_LANES);
    }

    /** Zeroes the per rollout aggregates; called on the copy a rollout starts from. */
    public void resetAccumulators() {
        accExpectedDamageTaken = 0;
        accDamageDealt = 0;
        accKills = 0;
        accPrayerDrained = 0;
        accSuppliesUsed = 0;
        died = false;
        deathTick = 0;
        accAlignmentEvents = 0;
        accLosExposureTicks = 0;
        accResidualWorst = 0;
        accKillValue = 0;
        accThreatDamage1000 = 0;
        accThreatHealed1000 = 0;
        Arrays.fill(accLane, 0);
        hpWorstFloor = hpExpected;
        prayerWorst = prayerExpected;
    }

    /** Zeroes and re marks the occupancy board from the footprints of every body blocking NPC alive or a corpse. */
    public void rebuildOccupancy() {
        Arrays.fill(occupancy, 0L);
        for (int i = 0; i < MAX_NPCS; i++) {
            if (frame.npcTypeIdx[i] < 0 || !(alive(i) || corpse(i)) || frame.def(i).noBody) {
                continue;
            }
            setFootprint(i, frame.def(i).size);
        }
    }

    /**
     * Marks a slot's footprint on the occupancy board (nothing for pass through types).
     * @param slot the slot
     * @param size the footprint size
     */
    public void setFootprint(int slot, int size) {
        if (frame.def(slot).passThrough) {
            return;
        }
        int pos = npcPos[slot];
        int x = Coords.x(pos);
        int y = Coords.y(pos);
        long mask = ((1L << size) - 1) << x;
        for (int r = 0; r < size; r++) {
            occupancy[y + r] |= mask;
        }
    }

    /**
     * Clears a slot's footprint from the occupancy board (nothing for pass through types).
     * @param slot the slot
     * @param size the footprint size
     */
    public void clearFootprint(int slot, int size) {
        if (frame.def(slot).passThrough) {
            return;
        }
        int pos = npcPos[slot];
        int x = Coords.x(pos);
        int y = Coords.y(pos);
        long mask = ((1L << size) - 1) << x;
        for (int r = 0; r < size; r++) {
            occupancy[y + r] &= ~mask;
        }
    }

    /**
     * FNV-1a over the gameplay relevant fields; used by determinism tests and transposition.
     * @return the hash
     */
    public long stateHash() {
        long h = 0xcbf29ce484222325L;
        h = mix(h, playerPos);
        h = mix(h, hpExpected);
        h = mix(h, hpWorstFloor);
        h = mix(h, prayerExpected);
        h = mix(h, runEnergy100);
        h = mix(h, specEnergy10);
        h = mix(h, overhead);
        h = mix(h, gearSet);
        h = mix(h, attackCooldown);
        h = mix(h, foodDelay | potDelay << 8 | karamDelay << 16);
        h = mix(h, targetSlot);
        h = mix(h, running ? 1 : 0);
        h = mix(h, moveDest);
        h = mix(h, pathKey);
        h = mix(h, pathCursor);
        h = mix(h, tick);
        h = mix(h, hitStacksWorst);
        h = mix(h, venomState);
        h = mix(h, foodCount | karamCount << 8 | brewDoses << 16 | restoreDoses << 24);
        h = mix(h, (specArmed ? 1 : 0) | specRegenCounter << 1 | prayerDrainAcc << 9);
        h = mix(h, waveClearTick);
        for (int i = 0; i < MAX_NPCS; i++) {
            h = mix(h, npcPos[i]);
            h = mix(h, npcHp[i]);
            h = mix(h, npcCooldown[i]);
            h = mix(h, npcFlags[i]);
            for (int lane = 0; lane < NPC_LANES; lane++) {
                h = mix(h, npcLane[lane][i]);
            }
            h = mix(h, npcSpawnTick[i]);
            h = mix(h, npcCorpse[i]);
        }
        for (int i = 0; i < pendingCount; i++) {
            h = mix(h, (int) pendingHits[i]);
            h = mix(h, (int) (pendingHits[i] >>> 32));
        }
        for (int lane = 0; lane < PACK_LANES; lane++) {
            h = mix(h, packLane[lane]);
        }
        return h;
    }

    private static long mix(long h, int v) {
        return (h ^ v) * 0x100000001b3L;
    }
}
