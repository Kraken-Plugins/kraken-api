package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.oracle.colosim.NpcType;
import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.oracle.colosim.model.Mob;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Parity harness: the colosim oracle and the engine run the same scenario tick for tick. Mobs are placed in colosim
 * coordinates; {@link #finish()} snapshots the oracle's sorted mob list into engine slots (slot i = oracle index i;
 * priorities match colosim's sort, asserted). The player is teleported identically on both sides before each tick,
 * so NPC behaviour is isolated from player pathing (which has its own tests).
 *
 * <p>Manticore extras follow colosim: "r"/"m" charged with that pattern, "ur"/"um" uncharged with the pattern known.
 * Scenarios must keep one style across all manticores: colosim makes a late charger adopt an already charging
 * manticore's style (as the live game does), while the engine's pattern is per slot data.
 */
public final class Fixture {
    public final Simulation oracle = new Simulation();
    public final ColosseumPack pack = ColosseumPack.load();
    public final MechanicsData data = pack.data();
    public Engine engine;
    public Frame frame;
    public SimState state;
    public final int[] ourLine = new int[SimState.MAX_NPCS];

    private int playerCx = 7;
    private int playerCy = 15;
    private boolean waveStart;

    public void place(NpcType type, int cx, int cy, String extra) {
        assertTrue("placeMob rejected " + type + " at " + cx + "," + cy, oracle.placeMob(cx, cy, type, extra));
    }

    public void player(int cx, int cy) {
        playerCx = cx;
        playerCy = cy;
        oracle.setPlayer(cx, cy);
        if (state != null) {
            state.playerPos = Coords.pack(cx, TestArenas.flip(cy));
        }
    }

    public void waveStart() {
        waveStart = true;
        oracle.setFromWaveStart(true);
        if (frame != null) {
            frame.waveStartGates = true;
        }
    }

    public void finish() {
        oracle.setPlayer(playerCx, playerCy);
        frame = TestArenas.frame(TestArenas.grid(oracle), pack);
        frame.waveStartGates = waveStart;
        state = new SimState();
        state.frame = frame;
        state.playerPos = Coords.pack(playerCx, TestArenas.flip(playerCy));
        List<Mob> mobs = oracle.getMobs();
        assertTrue("fixture supports up to 16 mobs", mobs.size() <= SimState.MAX_NPCS);
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            int typeIdx = data.typeIndex(keyFor(mob.getType()));
            NpcDef def = data.npcs[typeIdx];
            frame.npcTypeIdx[i] = (byte) typeIdx;
            frame.npcMaxHp[i] = (short) def.hp;
            frame.npcIndex[i] = i;
            state.npcPos[i] = Coords.pack(mob.getX(), TestArenas.flip(mob.getY()));
            state.npcHp[i] = (short) def.hp;
            state.npcFlags[i] = SimState.FLAG_ALIVE;
            state.npcSpawnTick[i] = waveStart ? 0 : SimState.SPAWN_OPEN;
            if (ColosseumPack.manticore(def) != null) {
                String extra = mob.getExtra();
                assertTrue("manticores need an explicit extra", extra != null);
                boolean uncharged = extra.startsWith("u");
                if (!uncharged) {
                    state.npcFlags[i] |= Lanes.FLAG_CHARGED;
                }
                state.npcLane[Lanes.PATTERN][i] =
                        (byte) pack.colosseum().patternId(uncharged ? extra.substring(1) : extra);
            }
        }
        frame.finishOrder();
        for (int i = 0; i < frame.npcOrderCount; i++) {
            assertEquals("engine processing order must equal the oracle's sorted order", i, frame.npcOrder[i]);
        }
        state.rebuildOccupancy();
        engine = new Engine();
        engine.beginPass();
        engine.attackListener = (slot, style, tick) -> ourLine[slot] = 1 | Math.max(style, 0) << 8;
    }

    /** Advances both simulations one tick and asserts positions and attack lines match. */
    public void stepAndAssert(String ctx) {
        Arrays.fill(ourLine, 0);
        Simulation.StepResult r = oracle.step();
        engine.advanceInPlace(state, Command.NONE);
        List<Mob> mobs = oracle.getMobs();
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            String at = ctx + " tick " + r.tick + " mob " + i + " (" + oracle.getNpcName(mob.getType()) + ")";
            assertEquals(at + " x", mob.getX(), Coords.x(state.npcPos[i]));
            assertEquals(at + " y", TestArenas.flip(mob.getY()), Coords.y(state.npcPos[i]));
            assertEquals(at + " attacked", r.line[i] & 1, ourLine[i] & 1);
            if (mob.getType() == Simulation.MANTICORE) {
                assertEquals(at + " orb style", r.line[i] >> 8 & 0xff, ourLine[i] >> 8 & 0xff);
            }
        }
    }

    public static String keyFor(int colosimType) {
        NpcType type = NpcType.fromTypeId(colosimType);
        switch (type) {
            case SERPENT_SHAMAN:
                return "serpent_shaman";
            case JAVELIN_COLOSSUS:
                return "javelin_colossus";
            case JAGUAR_WARRIOR:
                return "jaguar_warrior";
            case MANTICORE:
                return "manticore";
            case MINOTAUR:
                return "minotaur";
            case SHOCKWAVE_COLOSSUS:
                return "shockwave_colossus";
            case REINFORCEMENT_SHAMAN:
                return "serpent_shaman_reinforcement";
            default:
                throw new IllegalArgumentException("unsupported colosim type " + type);
        }
    }
}
