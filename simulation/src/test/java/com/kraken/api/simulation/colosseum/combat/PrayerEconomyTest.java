package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.player.Command;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Prayer and special-attack economy: drain effect 12 vs resistance 2 x bonus + 60 with a
 * free activation tick, offensive prayer drain from the gear set, Blasphemy coupling,
 * forced overhead drop at zero prayer, and spec regeneration (10% per 50 ticks, 25 with
 * a Lightbearer).
 */
public class PrayerEconomyTest {
    @Test
    public void overheadDrainsOnePointPerFiveTicksAtZeroBonus() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MELEE));
        assertEquals("activation tick does not drain", 99, fx.st.prayerExpected);
        fx.ticks(4);
        assertEquals(99, fx.st.prayerExpected);
        fx.ticks(1);
        assertEquals("12 effect vs 60 resistance: one point per 5 ticks", 98, fx.st.prayerExpected);
        fx.ticks(5);
        assertEquals(97, fx.st.prayerExpected);
    }

    @Test
    public void offensivePrayerFromTheGearSetAddsDrain() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.frame.equipSets[0].offensiveDrain = 24;
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MELEE));
        fx.ticks(5);
        assertEquals("36 effect drains 3 points per 5 ticks", 96, fx.st.prayerExpected);
    }

    @Test
    public void blasphemyDrainsPrayerByPctOfDamageTaken() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.mod(Mods.BLASPHEMY, 2);
        fx.finish();
        fx.ticks(4);
        assertEquals(99 - 8, fx.st.hpExpected);
        assertEquals("40% of the 8 damage as prayer", 99 - 3, fx.st.prayerExpected);
    }

    @Test
    public void overheadDropsAtZeroPrayer() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.st.prayerExpected = 1;
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MELEE));
        assertEquals(1, fx.st.overhead);
        fx.ticks(5);
        assertEquals(0, fx.st.prayerExpected);
        assertEquals("prayers cannot stay up at zero points", 0, fx.st.overhead);
    }

    @Test
    public void specRegeneratesTenPercentPerFiftyTicks() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.st.specEnergy10 = 500;
        fx.finish();
        fx.ticks(49);
        assertEquals(500, fx.st.specEnergy10);
        fx.ticks(1);
        assertEquals(600, fx.st.specEnergy10);

        ScenarioFixture lb = new ScenarioFixture();
        lb.st.specEnergy10 = 500;
        lb.frame.lightbearer = true;
        lb.finish();
        lb.ticks(25);
        assertEquals("Lightbearer halves the cycle", 600, lb.st.specEnergy10);
    }
}
