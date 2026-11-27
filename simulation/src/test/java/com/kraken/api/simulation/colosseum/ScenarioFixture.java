package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.EquipSet;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.util.Arrays;

/**
 * Engine only scenario harness for the combat suite: real arena grid, engine coordinates, a live player (99 hp and
 * prayer, supplies), optional modifiers and gear sets. No colosim oracle: combat mechanics have no oracle; scenarios
 * assert the encoded contract from npcs.json and modifiers.json.
 */
public final class ScenarioFixture {
    public final ColosseumPack pack = ColosseumPack.load();
    public final MechanicsData data = pack.data();
    public final Grid arena;
    public final Frame frame;
    public final SimState st;
    public Engine engine;

    public ScenarioFixture() {
        this(TestArenas.grid(new Simulation()));
    }

    /** A scenario on the region local arena ({@link TestArenas#liveGrid}), for mechanics whose data is written in
     *  live tiles; the player starts on the arena centre. */
    public static ScenarioFixture live() {
        ScenarioFixture fx = new ScenarioFixture(TestArenas.liveGrid(new Simulation()));
        fx.player(16 + TestArenas.LIVE_DX, 16 + TestArenas.LIVE_DY);
        return fx;
    }

    private ScenarioFixture(Grid arena) {
        this.arena = arena;
        frame = TestArenas.frame(arena, pack);
        st = new SimState();
        st.frame = frame;
        st.playerPos = Coords.pack(16, 16);
        st.hpExpected = 99;
        st.prayerExpected = 99;
        st.foodCount = 10;
        st.karamCount = 10;
        st.brewDoses = 8;
        st.restoreDoses = 8;
    }

    public void player(int x, int y) {
        st.playerPos = Coords.pack(x, y);
    }

    /** The frame's modifiers. */
    public Mods mods() {
        return ColosseumFrame.mods(frame);
    }

    public void mods(int mask, int tiers) {
        pack.mods(mask, tiers).apply(frame);
    }

    public void mod(int id, int tier) {
        Mods m = mods();
        mods(m.mask | 1 << id, m.tiers | Mods.tierBits(id, tier));
    }

    public int npc(int slot, String key, int x, int y) {
        TestArenas.addNpc(frame, st, slot, data, key, x, y);
        frame.npcHomePos[slot] = st.npcPos[slot];
        return slot;
    }

    /** A slot that spawns at the reinforcement tick instead of wave start. */
    public int reinforcementSlot(int slot, String key) {
        TestArenas.addNpc(frame, st, slot, data, key, 0, 0);
        st.npcFlags[slot] = 0;
        st.npcPos[slot] = Coords.NONE;
        ColosseumFrame.of(frame).reinforcementBits |= (short) (1 << slot);
        return slot;
    }

    /** A reserved Totemic totem slot the pack may spawn on demand. */
    public int totemPoolSlot(int slot) {
        TestArenas.addNpc(frame, st, slot, data, "healing_totem", 0, 0);
        st.npcFlags[slot] = 0;
        st.npcPos[slot] = Coords.NONE;
        ColosseumFrame.of(frame).totemPoolBits |= (short) (1 << slot);
        return slot;
    }

    /** The shared tribrid loadout. */
    public void equipDefault() {
        frame.equipSets = Loadouts.tribrid(data);
    }

    /** One gear set dealing the same expected damage to every type; returns its index. */
    public int equip(int setIdx, int style, int speed, int range, int damage) {
        if (frame.equipSets == null) {
            frame.equipSets = new EquipSet[4];
        }
        EquipSet set = new EquipSet(style, speed, range, data.npcs.length);
        Arrays.fill(set.expectedDamage, damage);
        Arrays.fill(set.maxHit, damage * 2);
        frame.equipSets[setIdx] = set;
        return setIdx;
    }

    public void finish() {
        frame.finishPlayer(data);
        frame.finishEquip(data);
        frame.finishOrder();
        st.rebuildOccupancy();
        st.resetAccumulators();
        engine = new Engine();
        engine.beginPass();
    }

    public void tick(long cmd) {
        engine.advanceInPlace(st, cmd);
    }

    public void ticks(int n) {
        for (int i = 0; i < n; i++) {
            engine.advanceInPlace(st, 0L);
        }
    }
}
