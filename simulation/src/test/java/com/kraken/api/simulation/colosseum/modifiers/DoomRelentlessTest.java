package com.kraken.api.simulation.colosseum.modifiers;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.player.Command;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Doom stack accrual per damaging instance (A16), and Relentless damage-channel
 * adjustments (+max hit, higher expected factor, tier III accuracy skip).
 */
public class DoomRelentlessTest {
    @Test
    public void doomCountsOneStackPerLandedInstance() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.mod(Mods.DOOM, 3);
        fx.finish();
        fx.ticks(4);
        assertEquals("one worst-channel stack per landed instance", 1, fx.st.hitStacksWorst);
        fx.ticks(5);
        assertEquals(2, fx.st.hitStacksWorst);
    }

    @Test
    public void prayedHitsAddNoDoomStacks() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.mod(Mods.DOOM, 1);
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(12);
        assertEquals("a blocked hit is not a damage instance", 0, fx.st.hitStacksWorst);
    }

    @Test
    public void relentlessBumpsBothChannels() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.mod(Mods.RELENTLESS, 1);
        fx.finish();
        fx.ticks(4);
        assertEquals("worst 28+1, expected 29 x 0.35", 99 - 10, fx.st.hpExpected);
        assertEquals(99 - 29, fx.st.hpWorstFloor);
    }

    @Test
    public void relentlessThreeSkipsAccuracy() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.mod(Mods.RELENTLESS, 3);
        fx.finish();
        fx.ticks(4);
        assertEquals("worst 28+6, expected = half of max (always hits)", 99 - 17, fx.st.hpExpected);
        assertEquals(99 - 34, fx.st.hpWorstFloor);
    }

    @Test
    public void frailtyCutsMaxHpByTier() {
        for (int tier = 1; tier <= 3; tier++) {
            ScenarioFixture fx = new ScenarioFixture();
            fx.mod(Mods.FRAILTY, tier);
            fx.finish();
            int expected = 99 * (100 - new int[]{10, 20, 40}[tier - 1]) / 100;
            assertEquals("tier " + tier, expected, fx.frame.playerMaxHp);
        }
    }

    @Test
    public void doomProximityIsVisibleToTheGateChannel() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "jaguar_warrior", 14, 16);
        fx.mod(Mods.DOOM, 3);
        fx.finish();
        fx.ticks(2);
        assertTrue("triple rolls push worst doom toward the tier-III cap of 5",
                fx.st.hitStacksWorst >= 3);
        assertEquals(5, fx.mods().doomCap);
    }
}
