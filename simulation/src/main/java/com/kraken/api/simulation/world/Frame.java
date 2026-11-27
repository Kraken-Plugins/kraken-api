package com.kraken.api.simulation.world;

import com.kraken.api.simulation.data.EquipSet;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.data.PlayerData;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.hits.DamageModifiers;
import com.kraken.api.simulation.npc.PackRules;

import java.util.Arrays;

/**
 * Immutable per snapshot identity, shared by reference between all SimStates derived from one capture: arena
 * geometry, NPC slot typing, turn order, player bases, gear sets, the damage model, and the pack's own part. Mutable
 * per tick values live in {@link SimState}. Builders populate the fields, call {@link #finishOrder()}, and treat
 * the frame as read only afterwards.
 */
public final class Frame {
    /** The arena geometry. */
    public Grid grid;
    /** The pack's mechanics tables. */
    public MechanicsData data;

    /** NPC type index per slot, -1 for an empty slot. */
    public final byte[] npcTypeIdx = new byte[SimState.MAX_NPCS];
    /** Maximum hit points per slot. */
    public final short[] npcMaxHp = new short[SimState.MAX_NPCS];
    /** The game's NPC index per slot (-1 for slots the engine spawns itself); decides the turn order. */
    public final int[] npcIndex = new int[SimState.MAX_NPCS];
    /** Bit per slot: NPC uses route finding movement. */
    public short npcSmartPathBits;

    /**
     * Slot indices in the order the game processes NPCs: ascending NPC index, with slots the engine spawns itself
     * (no index yet) after every live NPC. Decides contested tiles when two NPCs step the same tick.
     */
    public final byte[] npcOrder = new byte[SimState.MAX_NPCS];
    /** Number of valid entries in {@link #npcOrder}. */
    public int npcOrderCount;

    /** Capture time anchor per slot (a respawn location). */
    public final short[] npcHomePos = new short[SimState.MAX_NPCS];

    /** Player maximum hit points. */
    public short playerMaxHp = 99;
    /** Player maximum prayer points. */
    public short playerMaxPrayer = 99;
    /** Player carried weight in kg. */
    public int playerWeight;
    /** Player agility level. */
    public int agilityLevel = 99;
    /** True when the player has a lightbearer, which speeds special attack regeneration. */
    public boolean lightbearer;

    /** Switchable gear sets (up to 4); null entries and a null array are legal (no combat). */
    public EquipSet[] equipSets;
    /** Best equip set index per NPC type by expected damage per tick; -1 = none. */
    public byte[] bestSetForType;

    /** The food the player eats, or null for none. */
    public PlayerData.ConsumableDef foodDef;
    /** The karambwan the player eats, or null for none. */
    public PlayerData.ConsumableDef karamDef;
    /** The brew the player drinks, or null for none. */
    public PlayerData.ConsumableDef brewDef;
    /** The prayer restore the player drinks, or null for none. */
    public PlayerData.ConsumableDef restoreDef;
    /** Overheal beyond max hp the food allows. */
    public int overhealCap;

    /** The damage model's modifiers; shared by reference, never mutated once set. */
    public DamageModifiers damage = DamageModifiers.NONE;
    /** Subtracted from every gear set's attack range (never below 1). */
    public int playerRangePenalty;
    /** Damage per tick of standing on molten sand. */
    public int sandDamage;
    /** Max hp reduction in percent, applied by {@link #finishPlayer}; a reduced max also disables overheal. */
    public int maxHpPenaltyPct;
    /**
     * The pack's gated world hooks this frame needs ({@code PackRules.HOOK_*}); every one unless the pack narrows it.
     * A pack clears a bit only for a frame on which that hook would do nothing.
     */
    public int packHooks = PackRules.HOOKS_ALL;

    /** Wave number, starting at 1. */
    public byte waveNumber = 1;
    /**
     * True when the wave clock is anchored (the wave start tick is known). Gate enforcement itself is per NPC and
     * spawn relative via {@code SimState.npcSpawnTick}; builders use this flag to decide whether to stamp the
     * roster's spawn ticks or leave the gates open.
     */
    public boolean waveStartGates;

    /** The content pack's part of the frame, or null. */
    public PackFrame pack;

    /** Creates an empty frame: every NPC slot untyped, unindexed and without a home tile. */
    public Frame() {
        Arrays.fill(npcTypeIdx, (byte) -1);
        Arrays.fill(npcIndex, -1);
        Arrays.fill(npcHomePos, Coords.NONE);
    }

    /**
     * Copies another frame's identity into this one (arrays by value, loaded data, geometry and damage model by
     * reference, the pack part by value). Lets a prediction store keep a frame alive after the snapshot buffer it
     * came from is recycled for a later capture.
     * @param o the frame to copy
     */
    public void copyFrom(Frame o) {
        grid = o.grid;
        data = o.data;
        System.arraycopy(o.npcTypeIdx, 0, npcTypeIdx, 0, SimState.MAX_NPCS);
        System.arraycopy(o.npcMaxHp, 0, npcMaxHp, 0, SimState.MAX_NPCS);
        System.arraycopy(o.npcIndex, 0, npcIndex, 0, SimState.MAX_NPCS);
        System.arraycopy(o.npcOrder, 0, npcOrder, 0, SimState.MAX_NPCS);
        System.arraycopy(o.npcHomePos, 0, npcHomePos, 0, SimState.MAX_NPCS);
        npcSmartPathBits = o.npcSmartPathBits;
        npcOrderCount = o.npcOrderCount;
        playerMaxHp = o.playerMaxHp;
        playerMaxPrayer = o.playerMaxPrayer;
        playerWeight = o.playerWeight;
        agilityLevel = o.agilityLevel;
        lightbearer = o.lightbearer;
        equipSets = o.equipSets;
        bestSetForType = o.bestSetForType;
        foodDef = o.foodDef;
        karamDef = o.karamDef;
        brewDef = o.brewDef;
        restoreDef = o.restoreDef;
        overhealCap = o.overhealCap;
        damage = o.damage;
        playerRangePenalty = o.playerRangePenalty;
        sandDamage = o.sandDamage;
        maxHpPenaltyPct = o.maxHpPenaltyPct;
        packHooks = o.packHooks;
        waveNumber = o.waveNumber;
        waveStartGates = o.waveStartGates;
        if (o.pack == null) {
            pack = null;
        } else if (pack != null && pack.getClass() == o.pack.getClass()) {
            pack.copyFrom(o.pack);
        } else {
            pack = o.pack.copy();
        }
    }

    /**
     * Populates the consumable defs from the data tables (the player.json defaults) and applies the max hp penalty
     * and the overheal cap. Call after the damage model and the boosted bases are set.
     * @param data the loaded tables
     */
    public void finishPlayer(MechanicsData data) {
        PlayerData pd = data.player;
        foodDef = pd.consumable("food", pd.defaultFood);
        karamDef = pd.consumable("karambwan", pd.defaultKarambwan);
        brewDef = pd.defaultBrew != null ? pd.consumable("potion", pd.defaultBrew) : firstPotion(pd, false);
        restoreDef = pd.defaultRestore != null ? pd.consumable("potion", pd.defaultRestore) : firstPotion(pd, true);
        if (maxHpPenaltyPct > 0) {
            playerMaxHp = (short) (playerMaxHp * (100 - maxHpPenaltyPct) / 100);
            overhealCap = 0;
        } else {
            overhealCap = foodDef != null ? foodDef.overhealCap : 0;
        }
    }

    private static PlayerData.ConsumableDef firstPotion(PlayerData pd, boolean restore) {
        if (pd.consumables == null) {
            return null;
        }
        for (PlayerData.ConsumableDef c : pd.consumables) {
            if ("potion".equals(c.category) && (restore ? c.prayerRestore > 0 : c.heal > 0)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Recomputes {@link #bestSetForType} by expected damage per tick; call after equipSets change.
     * @param data the loaded tables
     */
    public void finishEquip(MechanicsData data) {
        bestSetForType = new byte[data.npcs.length];
        Arrays.fill(bestSetForType, (byte) -1);
        if (equipSets == null) {
            return;
        }
        for (int t = 0; t < data.npcs.length; t++) {
            int best = -1;
            int bestScore = -1;
            for (int s = 0; s < equipSets.length; s++) {
                EquipSet set = equipSets[s];
                if (set == null) {
                    continue;
                }
                int score = set.expectedDamage[t] * 1000 / set.attackSpeed;
                if (score > bestScore) {
                    bestScore = score;
                    best = s;
                }
            }
            bestSetForType[t] = (byte) best;
        }
    }

    /**
     * Whether a slot route finds.
     * @param slot the slot
     * @return true for a smart path slot
     */
    public boolean smartPath(int slot) {
        return (npcSmartPathBits >> slot & 1) != 0;
    }

    /**
     * Marks a slot as route finding.
     * @param slot the slot
     */
    public void setSmartPath(int slot) {
        npcSmartPathBits |= (short) (1 << slot);
    }

    /**
     * The type of a slot.
     * @param slot the slot
     * @return its definition
     */
    public NpcDef def(int slot) {
        return data.npcs[npcTypeIdx[slot]];
    }

    /** Rebuilds {@link #npcOrder} from the occupied slots; call after slot assignment changes. */
    public void finishOrder() {
        int n = 0;
        for (int i = 0; i < SimState.MAX_NPCS; i++) {
            if (npcTypeIdx[i] < 0) {
                continue;
            }
            int key = orderKey(i);
            int j = n;
            while (j > 0 && orderKey(npcOrder[j - 1]) > key) {
                npcOrder[j] = npcOrder[j - 1];
                j--;
            }
            npcOrder[j] = (byte) i;
            n++;
        }
        npcOrderCount = n;
    }

    private int orderKey(int slot) {
        return npcIndex[slot] >= 0 ? npcIndex[slot] : (1 << 20) + slot;
    }
}
