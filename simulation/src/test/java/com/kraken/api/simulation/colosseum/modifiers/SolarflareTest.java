package com.kraken.api.simulation.colosseum.modifiers;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.ColosseumData;
import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.colosseum.Lanes;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.colosseum.Solar;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Solarflare (RSProx capture analysis section 11): four flares, one per pillar, each
 * circling the 5x5 square round its pillar clockwise from the corner tile nearest the
 * arena centre; a step every 2 ticks along a side with 7 ticks on a corner at I, every 2 at
 * II, every tick with 2 on a corner at III. Contact damages the player's ending tile and III
 * knocks the protection prayer off.
 */
public class SolarflareTest {
    @Test
    public void flaresStartOnTheInnerCornersAndCircleClockwise() {
        ColosseumData col = ColosseumPack.load().colosseum();
        // Capture section 1.2: world 1819,3101 1819,3112 1830,3101 1830,3112.
        assertEquals(Coords.pack(27, 29), col.solarRings[0][0]);
        assertEquals(Coords.pack(27, 40), col.solarRings[1][0]);
        assertEquals(Coords.pack(38, 29), col.solarRings[2][0]);
        assertEquals(Coords.pack(38, 40), col.solarRings[3][0]);
        // South-west pillar: from its north-east corner clockwise means heading south.
        assertEquals(Coords.pack(27, 28), col.solarRings[0][1]);
        assertEquals(Coords.pack(27, 25), col.solarRings[0][4]);
        assertEquals(Coords.pack(23, 25), col.solarRings[0][8]);
        assertEquals(Coords.pack(23, 29), col.solarRings[0][12]);
        // North-west pillar: from its south-east corner clockwise means heading west.
        assertEquals(Coords.pack(26, 40), col.solarRings[1][1]);
    }

    @Test
    public void tierTwoStepsEveryTwoTicks() {
        ScenarioFixture fx = ScenarioFixture.live();
        fx.mod(Mods.SOLARFLARE, 2);
        Lanes.setSolar(fx.st, Solar.start(fx.mods()));
        fx.finish();
        fx.ticks(20);
        assertEquals("10 steps in 20 ticks", 10, Solar.index(Lanes.solar(fx.st)));
    }

    @Test
    public void tierThreeStepsEveryTickAndWaitsTwoOnCorners() {
        ScenarioFixture fx = ScenarioFixture.live();
        fx.mod(Mods.SOLARFLARE, 3);
        Lanes.setSolar(fx.st, Solar.start(fx.mods()));
        fx.finish();
        fx.ticks(2);
        assertEquals("leaves the start corner after 2", 1, Solar.index(Lanes.solar(fx.st)));
        fx.ticks(3);
        assertEquals("one a tick along the side to the next corner", 4, Solar.index(Lanes.solar(fx.st)));
        fx.ticks(1);
        assertEquals("holds the corner", 4, Solar.index(Lanes.solar(fx.st)));
        fx.ticks(1);
        assertEquals(5, Solar.index(Lanes.solar(fx.st)));
    }

    @Test
    public void tierOneWaitsSevenTicksOnCorners() {
        ScenarioFixture fx = ScenarioFixture.live();
        fx.mod(Mods.SOLARFLARE, 1);
        Lanes.setSolar(fx.st, Solar.start(fx.mods()));
        fx.finish();
        fx.ticks(6);
        assertEquals("still on the start corner", 0, Solar.index(Lanes.solar(fx.st)));
        fx.ticks(1);
        assertEquals(1, Solar.index(Lanes.solar(fx.st)));
        fx.ticks(6);
        assertEquals("three more side steps, 2 apart", 4, Solar.index(Lanes.solar(fx.st)));
        fx.ticks(6);
        assertEquals("held at the corner", 4, Solar.index(Lanes.solar(fx.st)));
        fx.ticks(1);
        assertEquals(5, Solar.index(Lanes.solar(fx.st)));
    }

    @Test
    public void anyFlareDamagesAndTierThreeDropsPrayer() {
        ScenarioFixture fx = ScenarioFixture.live();
        fx.mod(Mods.SOLARFLARE, 3);
        Lanes.setSolar(fx.st, Solar.start(fx.mods()));
        int tile = fx.pack.colosseum().solarRings[3][1];
        fx.player(Coords.x(tile), Coords.y(tile));
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        assertEquals("no flare on the tile yet", 99, fx.st.hpExpected);
        assertEquals(3, fx.st.overhead);
        fx.ticks(1);
        assertTrue(Solar.on(fx.pack.colosseum(), Lanes.solar(fx.st), tile));
        assertEquals("18 contact damage at tier III", 99 - 18, fx.st.hpExpected);
        assertEquals("prayer knocked off on contact", 0, fx.st.overhead);
    }
}
