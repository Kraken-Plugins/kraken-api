package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Jaguar triple roll (one launch, three damage instances) and warband attacks (RSProx
 * capture analysis section 2, the seven traces): each member may swing only on its tick of
 * a 6-tick cycle fixed at spawn (berserker +3, seer +4, archer +5), only from its own side
 * of the player (berserker north, archer west, seer east), and the hit lands a tick later.
 */
public class JaguarWarbandTest {
    @Test
    public void jaguarRollsThreeHitsPerLaunch() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "jaguar_warrior", 14, 16);
        fx.finish();
        fx.tick(Command.NONE);
        assertEquals("3 x (47 x 0.30 = 14)", 99 - 42, fx.st.hpExpected);
        assertEquals("worst channel stacks all three max rolls", 99 - 3 * 47, fx.st.hpWorstFloor);
    }

    @Test
    public void jaguarRollsCountDoomStacksIndividually() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "jaguar_warrior", 14, 16);
        fx.mod(Mods.DOOM, 1);
        fx.finish();
        fx.tick(Command.NONE);
        assertEquals("one Doom stack per damaging roll (A16)", 3, fx.st.hitStacksWorst);
    }

    @Test
    public void prayingMeleeBlocksTheWholeTriple() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "jaguar_warrior", 14, 16);
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MELEE));
        fx.ticks(10);
        assertEquals(99, fx.st.hpExpected);
    }

    @Test
    public void warbandAttacksOnItsCycleTickFromItsOwnSide() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        spawnedAtZero(fx, fx.npc(0, "warband_berserker", 16, 17));
        fx.finish();
        fx.ticks(4);
        assertEquals("swings on spawn + 3, the hit lands a tick later", 99, fx.st.hpExpected);
        fx.ticks(1);
        assertEquals("29 x 0.30 = 8", 99 - 8, fx.st.hpExpected);
        fx.ticks(5);
        assertEquals("nothing until the next cycle tick", 99 - 8, fx.st.hpExpected);
        fx.ticks(1);
        assertEquals("spawn + 9 swing lands on 10", 99 - 16, fx.st.hpExpected);
    }

    @Test
    public void warbandRunsToItsOwnSideBeforeAttacking() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        // West of the player is the archer's side; the berserker's is north.
        spawnedAtZero(fx, fx.npc(0, "warband_berserker", 15, 16));
        fx.finish();
        fx.ticks(2);
        assertEquals("stands two ticks after spawn", Coords.pack(15, 16), fx.st.npcPos[0]);
        fx.ticks(1);
        assertEquals("then takes its side", Coords.pack(16, 17), fx.st.npcPos[0]);
        fx.ticks(2);
        assertEquals("swings on its first cycle tick", 99 - 8, fx.st.hpExpected);
    }

    @Test
    public void aStepOnTheTickBeforeACycleTickSkipsThatCycle() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        spawnedAtZero(fx, fx.npc(0, "warband_berserker", 16, 17));
        fx.finish();
        fx.ticks(2);
        // The player steps east on tick 2: on tick 3 the berserker's side is (17,17), it is
        // not standing there, so the cycle tick passes without a swing.
        fx.tick(Command.move(Coords.pack(17, 16)));
        fx.ticks(7);
        assertEquals("tick 3 skipped, tick 9 not landed yet", 99, fx.st.hpExpected);
        fx.ticks(1);
        assertEquals("the next cycle swing", 99 - 8, fx.st.hpExpected);
    }

    @Test
    public void membersSwingOneTickApartByRole() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        spawnedAtZero(fx, fx.npc(0, "warband_berserker", 16, 17));
        spawnedAtZero(fx, fx.npc(1, "warband_archer", 15, 16));
        spawnedAtZero(fx, fx.npc(2, "warband_seer", 17, 16));
        fx.finish();
        fx.ticks(5);
        assertEquals("berserker (+3) lands on 4", 99 - 8, fx.st.hpExpected);
        fx.ticks(1);
        assertEquals("seer (+4) lands on 5: 12 x 0.30 = 3", 99 - 11, fx.st.hpExpected);
        fx.ticks(1);
        assertEquals("archer (+5) lands on 6: 14 x 0.30 = 4", 99 - 15, fx.st.hpExpected);
    }

    @Test
    public void warbandStyleTriangleUsesTheMemberDamageStyle() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        spawnedAtZero(fx, fx.npc(0, "warband_seer", 17, 16));
        fx.finish();
        // The seer deals magic damage from melee reach: protect from magic blocks it.
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(12);
        assertEquals(99, fx.st.hpExpected);
    }

    private static void spawnedAtZero(ScenarioFixture fx, int slot) {
        fx.st.npcSpawnTick[slot] = 0;
    }
}
