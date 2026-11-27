package com.kraken.api.simulation.colosseum.parity;

import com.kraken.api.simulation.oracle.colosim.NpcType;
import com.kraken.api.simulation.colosseum.Fixture;
import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Spawn-relative action gates (RSProx capture analysis section 5e and the seven live traces:
 * 22 of 22 monsters, 21 of 21 warband members): every gate offset counts from the NPC's
 * spawn-visible tick (wave roster = tick 0). A monster first steps on tick 1 even with the
 * player in range and sight, sight holds it from tick 2, and it attacks from tick 3. Warband
 * members stand two ticks and first step on tick 2 (npcs.json gateMoveTick override). This
 * is colosim's move-at-1, which the traces bear out.
 */
public class WaveStartGateParityTest {

    private static void step(Fixture fx) {
        Arrays.fill(fx.ourLine, 0);
        fx.engine.advanceInPlace(fx.state, Command.NONE);
    }

    @Test
    public void monsterStepsOnTickOneHoldsOnSightFromTwoAttacksOnThree() {
        // A shaman already in range and sight at spawn: it holds tick 0, steps on tick 1
        // (canMove without the sight hold), holds from tick 2 and attacks on tick 3.
        Fixture fx = new Fixture();
        fx.player(16, 16);
        fx.place(NpcType.SERPENT_SHAMAN, 12, 16, null);
        fx.waveStart();
        fx.finish();

        step(fx);
        assertEquals("held on tick 0", 12, Coords.x(fx.state.npcPos[0]));
        step(fx);
        assertEquals("stepped on tick 1 despite sight", 13, Coords.x(fx.state.npcPos[0]));
        assertEquals("no attack before tick 3", 0, fx.ourLine[0] & 1);
        int posAfter1 = fx.state.npcPos[0];
        step(fx);
        assertEquals("holds from tick 2", posAfter1, fx.state.npcPos[0]);
        assertEquals("no attack before tick 3", 0, fx.ourLine[0] & 1);
        step(fx);
        assertEquals(posAfter1, fx.state.npcPos[0]);
        assertTrue("attacks from tick 3", (fx.ourLine[0] & 1) != 0);
    }

    @Test
    public void warbandFirstStepsOnTickTwo() {
        // colosim has no warband type — engine-only scenario with the wave-roster stamp.
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "warband_berserker", 10, 16);
        fx.st.npcSpawnTick[0] = 0;
        fx.finish();
        int spawn = fx.st.npcPos[0];

        fx.tick(Command.NONE);
        assertEquals("held on tick 0", spawn, fx.st.npcPos[0]);
        fx.tick(Command.NONE);
        assertEquals("held on tick 1 (warband move gate is 2)", spawn, fx.st.npcPos[0]);
        fx.tick(Command.NONE);
        assertTrue("stepped on tick 2", fx.st.npcPos[0] != spawn);
    }

    @Test
    public void lateSpawnGatesCountFromItsOwnSpawnTick() {
        // A reinforcement visible at T: idle T, a step at T+1 even with clear sight, held
        // from T+2, first attack at T+3.
        Fixture fx = new Fixture();
        fx.player(16, 16);
        fx.place(NpcType.SERPENT_SHAMAN, 12, 16, null);
        fx.finish();
        fx.state.tick = 5;
        fx.state.npcSpawnTick[0] = 5;

        step(fx);
        assertEquals("idle on its spawn tick", 12, Coords.x(fx.state.npcPos[0]));
        assertEquals("no attack on its spawn tick", 0, fx.ourLine[0] & 1);
        step(fx);
        assertEquals("steps at spawn+1 despite sight", 13, Coords.x(fx.state.npcPos[0]));
        int pos = fx.state.npcPos[0];
        step(fx);
        assertEquals("holds at spawn+2", pos, fx.state.npcPos[0]);
        assertEquals("no attack at spawn+2", 0, fx.ourLine[0] & 1);
        step(fx);
        assertEquals("holds at spawn+3", pos, fx.state.npcPos[0]);
        assertTrue("first attack at spawn+3", (fx.ourLine[0] & 1) != 0);
    }
}
