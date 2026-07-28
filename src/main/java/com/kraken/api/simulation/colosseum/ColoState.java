package com.kraken.api.simulation.colosseum;

/**
 * Mutable, poolable simulation state: everything about one possible future of the arena.
 *
 * <p>Layout is deliberately flat - primitive scalars plus a few small primitive arrays - so
 * that {@link #copyFrom(ColoState)} is a handful of {@code System.arraycopy} calls and the
 * search loop allocates nothing. Immutable identity data (types, sizes, max HP, the grid)
 * lives in the shared {@link ColoFrame}.</p>
 *
 * <p>In-flight attacks are packed into a {@code long} ring: land tick (16 bits), max damage
 * (8), style (2), source/target slot (6), targeted tile for dodgeable specials (12), a
 * prayer-resolved flag and expected damage (8). See {@link ColoTick} for encoding.</p>
 */
public final class ColoState {
    /** Overhead code: no protection prayer active. */
    public static final byte OVERHEAD_NONE = 0;
    /** Overhead code: Protect from Melee. */
    public static final byte OVERHEAD_MELEE = 1;
    /** Overhead code: Protect from Missiles. */
    public static final byte OVERHEAD_MISSILES = 2;
    /** Overhead code: Protect from Magic. */
    public static final byte OVERHEAD_MAGIC = 3;

    ColoFrame frame;

    /** Ticks since wave start (0-based; tick gating keys off this). */
    int tick;

    // ----- player -----
    short playerPos;
    int playerHp;
    int prayerPoints;
    int prayerDrainCounter;
    int runEnergyUnits;
    int specEnergy;
    int specRegenCounter;
    byte overhead;
    byte gearSet;
    byte foodDelay;
    byte comboDelay;
    byte potionDelay;
    byte attackDelay;
    short moveTarget;
    boolean moveRun;
    byte attackTargetSlot = -1;

    // ----- supplies -----
    byte foodCount;
    byte comboCount;
    byte brewDoses;
    byte restoreDoses;

    // ----- npcs (parallel arrays indexed by slot; identity data in frame) -----
    final short[] npcPos = new short[ColoConstants.MAX_NPCS];
    final short[] npcHp = new short[ColoConstants.MAX_NPCS];
    final byte[] npcCooldown = new byte[ColoConstants.MAX_NPCS];
    final byte[] npcFlags = new byte[ColoConstants.MAX_NPCS];
    final byte[] npcAux = new byte[ColoConstants.MAX_NPCS];

    static final byte NPC_FLAG_ACTIVE = 1;
    static final byte NPC_FLAG_CHARGING_STARTED = 2;

    // ----- in-flight hits -----
    final long[] pendingHits = new long[ColoConstants.MAX_PENDING_HITS];
    int pendingHitCount;

    // ----- rollout aggregates for scoring -----
    int expectedDamageTaken;
    int worstCaseHpFloor;
    int damageDealt;
    int kills;
    int suppliesUsed;
    boolean playerDied;

    /**
     * Resets this instance to an empty state bound to a frame. Callers then populate player
     * and NPC fields directly (the snapshot builder does this) or via {@link #copyFrom}.
     *
     * @param frame shared frame.
     */
    public void reset(ColoFrame frame) {
        this.frame = frame;
        tick = 0;
        playerPos = ColoCoords.NONE;
        playerHp = frame.getPlayerMaxHp();
        prayerPoints = frame.getPrayerPointCap();
        prayerDrainCounter = 0;
        runEnergyUnits = 10_000;
        specEnergy = 100;
        specRegenCounter = 0;
        overhead = OVERHEAD_NONE;
        gearSet = 0;
        foodDelay = 0;
        comboDelay = 0;
        potionDelay = 0;
        attackDelay = 0;
        moveTarget = ColoCoords.NONE;
        moveRun = false;
        attackTargetSlot = -1;
        foodCount = 0;
        comboCount = 0;
        brewDoses = 0;
        restoreDoses = 0;
        java.util.Arrays.fill(npcPos, ColoCoords.NONE);
        java.util.Arrays.fill(npcHp, (short) 0);
        java.util.Arrays.fill(npcCooldown, (byte) 0);
        java.util.Arrays.fill(npcFlags, (byte) 0);
        java.util.Arrays.fill(npcAux, (byte) 0);
        pendingHitCount = 0;
        expectedDamageTaken = 0;
        worstCaseHpFloor = playerHp;
        damageDealt = 0;
        kills = 0;
        suppliesUsed = 0;
        playerDied = false;
    }

    /**
     * Copies another state into this instance (pool-friendly branch copy).
     *
     * @param other source state.
     */
    public void copyFrom(ColoState other) {
        frame = other.frame;
        tick = other.tick;
        playerPos = other.playerPos;
        playerHp = other.playerHp;
        prayerPoints = other.prayerPoints;
        prayerDrainCounter = other.prayerDrainCounter;
        runEnergyUnits = other.runEnergyUnits;
        specEnergy = other.specEnergy;
        specRegenCounter = other.specRegenCounter;
        overhead = other.overhead;
        gearSet = other.gearSet;
        foodDelay = other.foodDelay;
        comboDelay = other.comboDelay;
        potionDelay = other.potionDelay;
        attackDelay = other.attackDelay;
        moveTarget = other.moveTarget;
        moveRun = other.moveRun;
        attackTargetSlot = other.attackTargetSlot;
        foodCount = other.foodCount;
        comboCount = other.comboCount;
        brewDoses = other.brewDoses;
        restoreDoses = other.restoreDoses;
        System.arraycopy(other.npcPos, 0, npcPos, 0, ColoConstants.MAX_NPCS);
        System.arraycopy(other.npcHp, 0, npcHp, 0, ColoConstants.MAX_NPCS);
        System.arraycopy(other.npcCooldown, 0, npcCooldown, 0, ColoConstants.MAX_NPCS);
        System.arraycopy(other.npcFlags, 0, npcFlags, 0, ColoConstants.MAX_NPCS);
        System.arraycopy(other.npcAux, 0, npcAux, 0, ColoConstants.MAX_NPCS);
        System.arraycopy(other.pendingHits, 0, pendingHits, 0, other.pendingHitCount);
        pendingHitCount = other.pendingHitCount;
        expectedDamageTaken = other.expectedDamageTaken;
        worstCaseHpFloor = other.worstCaseHpFloor;
        damageDealt = other.damageDealt;
        kills = other.kills;
        suppliesUsed = other.suppliesUsed;
        playerDied = other.playerDied;
    }

    // ----- read accessors (used by planner, overlays and tests) -----

    /** @return shared frame. */
    public ColoFrame frame() {
        return frame;
    }

    /** @return ticks since wave start. */
    public int tick() {
        return tick;
    }

    /** @return packed player position. */
    public short playerPos() {
        return playerPos;
    }

    /** @return player hitpoints (expected-value tracking). */
    public int playerHp() {
        return playerHp;
    }

    /** @return player prayer points. */
    public int prayerPoints() {
        return prayerPoints;
    }

    /** @return run energy in internal units, 0-10000. */
    public int runEnergyUnits() {
        return runEnergyUnits;
    }

    /** @return special attack energy, 0-100. */
    public int specEnergy() {
        return specEnergy;
    }

    /** @return active overhead code (OVERHEAD_*). */
    public byte overhead() {
        return overhead;
    }

    /** @return current gear set index. */
    public byte gearSet() {
        return gearSet;
    }

    /** @return current attack cooldown in ticks. */
    public byte attackDelay() {
        return attackDelay;
    }

    /** @return queued movement destination or {@link ColoCoords#NONE}. */
    public short moveTarget() {
        return moveTarget;
    }

    /** @return current attack target slot, -1 when none. */
    public byte attackTargetSlot() {
        return attackTargetSlot;
    }

    /** @return remaining primary food count. */
    public byte foodCount() {
        return foodCount;
    }

    /** @return remaining combo food count. */
    public byte comboCount() {
        return comboCount;
    }

    /** @return remaining brew doses. */
    public byte brewDoses() {
        return brewDoses;
    }

    /** @return remaining restore doses. */
    public byte restoreDoses() {
        return restoreDoses;
    }

    /**
     * @param slot npc slot.
     * @return true when the npc is alive and simulated.
     */
    public boolean npcActive(int slot) {
        return (npcFlags[slot] & NPC_FLAG_ACTIVE) != 0;
    }

    /**
     * @param slot npc slot.
     * @return packed south-west anchor position.
     */
    public short npcPos(int slot) {
        return npcPos[slot];
    }

    /**
     * @param slot npc slot.
     * @return npc hitpoints (expected-value tracking).
     */
    public int npcHp(int slot) {
        return npcHp[slot];
    }

    /**
     * @param slot npc slot.
     * @return npc attack cooldown; the npc can act when this is at or below zero.
     */
    public int npcCooldown(int slot) {
        return npcCooldown[slot];
    }

    /**
     * @param slot npc slot.
     * @return true when a manticore in this slot has started (or finished) charging.
     */
    public boolean npcChargingStarted(int slot) {
        return (npcFlags[slot] & NPC_FLAG_CHARGING_STARTED) != 0;
    }

    /**
     * @param slot npc slot.
     * @return manticore pattern code (see {@link ColoTick} PATTERN_* constants), 0 unknown.
     */
    public int manticorePattern(int slot) {
        return npcAux[slot] & 7;
    }

    /**
     * @param slot npc slot.
     * @return orbs still to launch from an in-progress manticore triple, 0-3.
     */
    public int manticoreOrbsRemaining(int slot) {
        return (npcAux[slot] >> 3) & 3;
    }

    /**
     * @param slot npc slot.
     * @return javelin colossus auto attacks thrown since the last sky javelin, 0-4.
     */
    public int javelinAutosSinceSpecial(int slot) {
        return npcAux[slot] & 7;
    }

    /** @return total expected damage taken across the simulated ticks. */
    public int expectedDamageTaken() {
        return expectedDamageTaken;
    }

    /** @return lowest "hp minus same-tick worst-case burst" seen across the rollout. */
    public int worstCaseHpFloor() {
        return worstCaseHpFloor;
    }

    /** @return total expected damage dealt to NPCs. */
    public int damageDealt() {
        return damageDealt;
    }

    /** @return NPCs killed during the rollout. */
    public int kills() {
        return kills;
    }

    /** @return supplies consumed during the rollout. */
    public int suppliesUsed() {
        return suppliesUsed;
    }

    /** @return true when the player died in this future. */
    public boolean playerDied() {
        return playerDied;
    }

    /** @return count of npcs still alive. */
    public int aliveNpcCount() {
        int count = 0;
        for (int slot = 0; slot < frame.getNpcSlotCount(); slot++) {
            if (npcActive(slot)) {
                count++;
            }
        }
        return count;
    }

    /** @return number of in-flight hits. */
    public int pendingHitCount() {
        return pendingHitCount;
    }

    /**
     * @param index pending hit index, 0 to {@link #pendingHitCount()} - 1.
     * @return packed pending hit.
     */
    public long pendingHit(int index) {
        return pendingHits[index];
    }

    // ----- mutation helpers used by the snapshot builder -----

    /**
     * Activates an npc slot with full identity coming from the frame.
     *
     * @param slot slot index.
     * @param pos packed south-west anchor.
     * @param hp current hitpoints.
     * @param cooldown current attack cooldown.
     */
    public void activateNpc(int slot, short pos, int hp, int cooldown) {
        npcPos[slot] = pos;
        npcHp[slot] = (short) Math.max(1, hp);
        npcCooldown[slot] = (byte) clampByte(cooldown);
        npcFlags[slot] = NPC_FLAG_ACTIVE;
        npcAux[slot] = 0;
    }

    /**
     * Sets a javelin colossus's auto-attack counter (from live tracking); the fifth attack
     * after four autos is the sky javelin special.
     *
     * @param slot slot index.
     * @param autos auto attacks since the last special, 0-4.
     */
    public void setJavelinAutos(int slot, int autos) {
        npcAux[slot] = (byte) ((npcAux[slot] & ~7) | (autos & 7));
    }

    /**
     * Marks a manticore slot's charge state and attack pattern (from live tracking).
     *
     * @param slot slot index.
     * @param patternCode pattern code, 0 when unknown (see {@link ColoTick}).
     * @param chargingStarted true when the charge has begun.
     * @param orbsRemaining orbs still to be launched from an in-progress triple.
     */
    public void setManticoreState(int slot, int patternCode, boolean chargingStarted, int orbsRemaining) {
        npcAux[slot] = (byte) ((patternCode & 7) | ((orbsRemaining & 3) << 3));
        if (chargingStarted) {
            npcFlags[slot] |= NPC_FLAG_CHARGING_STARTED;
        } else {
            npcFlags[slot] &= ~NPC_FLAG_CHARGING_STARTED;
        }
    }

    /**
     * Sets player vitals; used by the snapshot builder.
     *
     * @param pos packed player position.
     * @param hp hitpoints.
     * @param prayer prayer points.
     * @param runEnergy run energy units 0-10000.
     * @param spec special attack energy 0-100.
     * @param overheadCode active overhead (OVERHEAD_*).
     * @param gearSetIndex current gear set index.
     */
    public void setPlayer(short pos, int hp, int prayer, int runEnergy, int spec, byte overheadCode, int gearSetIndex) {
        playerPos = pos;
        playerHp = hp;
        worstCaseHpFloor = hp;
        prayerPoints = prayer;
        runEnergyUnits = Math.max(0, Math.min(10_000, runEnergy));
        specEnergy = Math.max(0, Math.min(100, spec));
        overhead = overheadCode;
        gearSet = (byte) gearSetIndex;
    }

    /**
     * Sets supply counts; used by the snapshot builder.
     *
     * @param food primary food count.
     * @param combo combo food count.
     * @param brews brew doses.
     * @param restores restore doses.
     */
    public void setSupplies(int food, int combo, int brews, int restores) {
        foodCount = (byte) clampByte(food);
        comboCount = (byte) clampByte(combo);
        brewDoses = (byte) clampByte(brews);
        restoreDoses = (byte) clampByte(restores);
    }

    /**
     * Sets consumption/attack cooldowns; used by the snapshot builder to carry live timers.
     *
     * @param food ticks until food can be eaten.
     * @param combo ticks until combo food can be eaten.
     * @param potion ticks until a potion can be sipped.
     * @param attack ticks until the player can attack.
     */
    public void setCooldowns(int food, int combo, int potion, int attack) {
        foodDelay = (byte) clampByte(food);
        comboDelay = (byte) clampByte(combo);
        potionDelay = (byte) clampByte(potion);
        attackDelay = (byte) clampByte(attack);
    }

    /**
     * Sets the wave tick; used by the snapshot builder.
     *
     * @param tick ticks since wave start.
     */
    public void setTick(int tick) {
        this.tick = Math.max(0, tick);
    }

    /**
     * Moves the player instantly without touching vitals; used by tests and by the snapshot
     * builder when re-syncing position.
     *
     * @param pos packed position.
     */
    public void teleportPlayer(short pos) {
        this.playerPos = pos;
    }

    /**
     * Sets the player's current attack interaction; used by the snapshot builder so plans
     * know an engagement is already in progress and no fresh attack click is needed.
     *
     * @param slot npc slot the player is attacking, or -1 for none.
     */
    public void setAttackTarget(int slot) {
        this.attackTargetSlot = (byte) (slot >= 0 && slot < ColoConstants.MAX_NPCS ? slot : -1);
    }

    private static int clampByte(int value) {
        return Math.max(0, Math.min(127, value));
    }
}
