package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.Lanes;
import com.kraken.api.simulation.colosseum.ScenarioFixture;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Minotaur heal predicate (A6): with no player in melee reach at cooldown expiry it heals
 * the most-damaged NPC below 75% max HP within center distance 7 and line of sight, never
 * another minotaur; 30 HP per cycle on the attack cooldown.
 */
public class MinotaurHealTest {
    @Test
    public void healsWoundedAllyWhenPlayerOutOfReach() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(2, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.npc(1, "minotaur", 12, 15);
        fx.st.npcHp[0] = 50;
        fx.finish();
        fx.ticks(1);
        assertEquals("a heal fills the bar", 125, fx.st.npcHp[0]);
        assertEquals(1, fx.st.accLane[Lanes.ACC_MINOTAUR_HEALS]);
        fx.st.npcHp[0] = 50;
        fx.ticks(9);
        assertEquals("a heal holds the minotaur 10 ticks", 50, fx.st.npcHp[0]);
        fx.ticks(1);
        assertEquals(125, fx.st.npcHp[0]);
    }

    @Test
    public void picksTheMostDamagedQualifyingAlly() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(2, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.npc(1, "warband_berserker", 10, 18);
        fx.npc(2, "minotaur", 12, 15);
        fx.st.npcHp[0] = 30;
        fx.st.npcHp[1] = 20;
        fx.finish();
        fx.ticks(1);
        assertEquals("shaman missing 95 beats berserker missing 28", 125, fx.st.npcHp[0]);
        assertEquals(20, fx.st.npcHp[1]);
    }

    @Test
    public void healRequiresCenterDistanceWithinSeven() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(2, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.npc(1, "minotaur", 20, 24);
        fx.st.npcHp[0] = 50;
        fx.finish();
        fx.ticks(1);
        assertEquals("center distance 10 is out of heal range", 50, fx.st.npcHp[0]);
    }

    @Test
    public void healRequiresLineOfSightBetweenCenters() {
        ScenarioFixture fx = new ScenarioFixture();
        // Healer south-west of the pillar, wounded shaman due east: the pillar row at y8
        // blocks the center-to-center ray; the control run without the pillar in the way
        // heals (same distance, path shifted south).
        fx.player(2, 2);
        fx.npc(0, "serpent_shaman", 12, 8);
        fx.npc(1, "minotaur", 5, 8);
        fx.st.npcHp[0] = 50;
        fx.finish();
        fx.ticks(1);
        assertEquals("pillar blocks the heal ray", 50, fx.st.npcHp[0]);

        ScenarioFixture control = new ScenarioFixture();
        control.player(2, 2);
        control.npc(0, "serpent_shaman", 12, 4);
        control.npc(1, "minotaur", 5, 8);
        control.st.npcHp[0] = 50;
        control.finish();
        control.ticks(1);
        assertEquals("clear ray heals", 125, control.st.npcHp[0]);
    }

    @Test
    public void neverHealsAnotherMinotaur() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(2, 16);
        fx.npc(0, "minotaur", 16, 12);
        fx.npc(1, "minotaur", 12, 15);
        fx.st.npcHp[0] = 100;
        fx.finish();
        fx.ticks(1);
        assertEquals(100, fx.st.npcHp[0]);
        assertEquals(0, fx.st.accLane[Lanes.ACC_MINOTAUR_HEALS]);
    }
}
