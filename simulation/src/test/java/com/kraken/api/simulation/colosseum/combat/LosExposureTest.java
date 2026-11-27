package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The LOS-exposure integral counts one NPC-tick for every alive ranged/magic attacker
 * (range > 1) with sight and range to the player; range-1 NPCs are unblockable by
 * pillars (melee adjacency ignores the grid) and never count, and neither do attackers
 * whose ray a pillar interrupts.
 */
public class LosExposureTest {
    @Test
    public void countsRangedAttackersWithSightPerTick() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 16, 21);
        fx.equipDefault();
        fx.finish();
        assertTrue("shaman must start with sight and range",
                Los.npcCanAttack(fx.frame.grid, 16, 21, 1, 10, 16, 16));

        fx.ticks(5);
        assertEquals(5, fx.st.accLosExposureTicks);
    }

    @Test
    public void meleeAndPillarBlockedAttackersDoNotCount() {
        ScenarioFixture fx = new ScenarioFixture();
        // The SW pillar spans (8-10, 8-10): the shaman's ray along y=9 is interrupted,
        // and its naive chase wedges on the pillar's east face without ever gaining LOS.
        fx.player(6, 9);
        fx.npc(0, "serpent_shaman", 13, 9);
        fx.npc(1, "warband_berserker", 5, 9);
        fx.equipDefault();
        fx.finish();
        assertFalse("pillar must block the shaman's ray",
                Los.npcCanAttack(fx.frame.grid, 13, 9, 1, 10, 6, 9));
        assertTrue("berserker starts melee-adjacent",
                Los.meleeAdjacent(5, 9, 1, 6, 9));

        fx.ticks(6);
        assertEquals("range-1 and pillar-blocked attackers accrue no exposure",
                0, fx.st.accLosExposureTicks);
        assertFalse("the wedged shaman still has no sight", Los.npcCanAttack(fx.frame.grid,
                Coords.x(fx.st.npcPos[0]), Coords.y(fx.st.npcPos[0]), 1, 10, 6, 9));
    }
}
