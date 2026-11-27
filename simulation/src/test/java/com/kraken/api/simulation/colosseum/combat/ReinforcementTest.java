package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reinforcement clock (RSProx capture analysis section 1): the reinforcement composition
 * spawns on wave tick 66, all from the north gate (y 48) when the player's y is 34 or more
 * at the start of that tick, else the south gate (y 20), on distinct x of 31 to 33 in
 * region-local tiles. It holds the wave open, and never spawns once the wave cleared
 * (arriving exactly at the clear dies automatically in the real game).
 */
public class ReinforcementTest {
    @Test
    public void reinforcementsSpawnOnTick66AtTheNorthGateWhenThePlayerIsNorth() {
        ScenarioFixture fx = ScenarioFixture.live();
        fx.player(33, 40);
        fx.npc(0, "serpent_shaman", 33, 30);
        fx.reinforcementSlot(1, "jaguar_warrior");
        fx.reinforcementSlot(2, "serpent_shaman_reinforcement");
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(65);
        assertFalse("nothing before the clock", fx.st.alive(1));
        fx.ticks(1);
        assertTrue(fx.st.alive(1));
        assertEquals("spawn tick stamped", 66, fx.st.npcSpawnTick[1]);
        assertEquals("jaguar at the north gate", Coords.pack(31, 48), fx.st.npcPos[1]);
        assertEquals("shaman beside it on its own x", Coords.pack(33, 48), fx.st.npcPos[2]);
        assertEquals("full hp on arrival", 125, fx.st.npcHp[1]);
    }

    @Test
    public void theGateFollowsThePlayersRowAtThirtyFour() {
        ScenarioFixture north = ScenarioFixture.live();
        north.player(20, 34);
        north.npc(0, "serpent_shaman", 33, 30);
        north.reinforcementSlot(1, "jaguar_warrior");
        north.finish();
        north.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        north.ticks(66);
        assertEquals("y 34 already counts as north", 48, Coords.y(north.st.npcPos[1]));
    }

    @Test
    public void reinforcementStepsOnTheTickAfterItAppears() {
        // Live traces and the capture: a reinforcement visible at T first steps at T+1 —
        // its action gates count from its own spawn tick.
        ScenarioFixture fx = ScenarioFixture.live();
        fx.player(33, 40);
        fx.npc(0, "serpent_shaman", 33, 30);
        fx.reinforcementSlot(1, "jaguar_warrior");
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(66);
        assertTrue(fx.st.alive(1));
        int spawnPos = fx.st.npcPos[1];
        fx.ticks(1);
        assertTrue("first step at spawn+1", fx.st.npcPos[1] != spawnPos);
    }

    @Test
    public void southGateWhenThePlayerIsSouth() {
        ScenarioFixture fx = ScenarioFixture.live();
        fx.player(33, 33);
        fx.npc(0, "serpent_shaman", 33, 40);
        fx.reinforcementSlot(1, "jaguar_warrior");
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(66);
        assertTrue(fx.st.alive(1));
        assertEquals("spawned at the south gate", Coords.pack(31, 20), fx.st.npcPos[1]);
    }

    @Test
    public void clearedWaveNeverSpawnsItsReinforcement() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.st.npcHp[0] = 15;
        fx.reinforcementSlot(1, "jaguar_warrior");
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(3);
        assertEquals(3, fx.st.waveClearTick);
        fx.ticks(70);
        assertFalse("reinforcements arriving as the wave clears die automatically",
                fx.st.alive(1));
    }

    @Test
    public void pendingReinforcementDoesNotHoldTheWaveOpen() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.st.npcHp[0] = 15;
        fx.reinforcementSlot(1, "jaguar_warrior");
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(3);
        assertEquals("wave clears when the last live enemy dies", 3, fx.st.waveClearTick);
    }

    @Test
    public void spawnedReinforcementHoldsTheWaveOpen() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(17, 28);
        fx.npc(0, "serpent_shaman", 17, 22);
        fx.reinforcementSlot(1, "jaguar_warrior");
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(70);
        assertTrue(fx.st.alive(1));
        // Kill the shaman after the reinforcement arrived: the jaguar holds the wave.
        fx.st.npcHp[0] = 1;
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.frame.finishEquip(fx.data);
        fx.tick(Command.attack(0));
        fx.ticks(4);
        assertFalse(fx.st.alive(0));
        assertEquals("wave still open while the jaguar lives", -1, fx.st.waveClearTick);
    }
}
