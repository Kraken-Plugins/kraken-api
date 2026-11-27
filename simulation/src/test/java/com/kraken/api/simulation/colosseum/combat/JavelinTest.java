package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Javelin colossus toss (RSProx capture analysis section 4): four autos, then a toss that
 * picks the tile the player stands on 3 ticks later and blasts it 3 ticks after that for
 * typeless damage — dodged by leaving that tile after it is picked — and leaves molten sand
 * per Reentry tier.
 */
public class JavelinTest {
    /** Protect from Missiles already up, so the autos are blocked with no switch risk. */
    private ScenarioFixture prayedColossus() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "javelin_colossus", 10, 14);
        fx.st.overhead = 2;
        return fx;
    }

    /**
     * Praying missiles blocks the four normal throws entirely, isolating the special:
     * attacks fire on ticks 0, 5, 10, 15, 20 — the 5th (tick 20) is the sky javelin,
     * landing on tick 26.
     */
    @Test
    public void fifthAttackIsTypelessSkyJavelinOnLaunchTile() {
        ScenarioFixture fx = prayedColossus();
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MISSILES));
        fx.ticks(25);
        assertEquals("normal throws all blocked; sky not landed yet", 99, fx.st.hpExpected);
        fx.ticks(1);
        assertEquals("sky javelin ignores prayer: 40 x 0.5 expected", 99 - 20, fx.st.hpExpected);
        assertEquals("worst channel takes the full 40", 99 - 40, fx.st.hpWorstFloor);
    }

    @Test
    public void leavingThePickedTileDodgesTheToss() {
        ScenarioFixture fx = prayedColossus();
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MISSILES));
        fx.ticks(23);
        // Tossed on tick 20, its tile picked on tick 23 from (16,16); step away before 26.
        fx.tick(Command.move(Coords.pack(18, 16)));
        fx.ticks(5);
        assertEquals("dodged entirely", 99, fx.st.hpExpected);
        assertEquals(99, fx.st.hpWorstFloor);
    }

    @Test
    public void aStepBeforeTheTileIsPickedIsFollowed() {
        ScenarioFixture fx = prayedColossus();
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MISSILES));
        fx.ticks(20);
        // Moving on tick 21 puts the player on (18,16) before the pick on tick 23.
        fx.tick(Command.move(Coords.pack(18, 16)));
        fx.ticks(5);
        assertEquals("the toss lands where the player stood on tick 23", 99 - 20, fx.st.hpExpected);
    }

    @Test
    public void reentryLeavesSandAtImpactPerTier() {
        for (int tier = 1; tier <= 3; tier++) {
            ScenarioFixture fx = prayedColossus();
            fx.mod(Mods.REENTRY, tier);
            fx.finish();
            fx.tick(Command.overhead(Command.OVERHEAD_MISSILES));
            fx.ticks(23);
            fx.tick(Command.move(Coords.pack(18, 16)));
            fx.ticks(5);
            assertTrue("tier " + tier + " sand at the impact tile", Grid.get(fx.st.moltenSand, 16, 16));
            assertEquals("tier " + tier + " SW-adjacent sand from tier II",
                    tier >= 2, Grid.get(fx.st.moltenSand, 15, 15));
            assertEquals("tier " + tier + " west sand from tier III",
                    tier >= 3, Grid.get(fx.st.moltenSand, 15, 16));
            assertFalse(Grid.get(fx.st.moltenSand, 17, 16));
        }
    }

    @Test
    public void standingOnMoltenSandBurnsPerTick() {
        ScenarioFixture fx = prayedColossus();
        fx.mod(Mods.REENTRY, 1);
        fx.finish();
        Grid.set(fx.st.moltenSand, 16, 16);
        fx.tick(Command.overhead(Command.OVERHEAD_MISSILES));
        fx.ticks(3);
        assertEquals("5 sand damage per tick on the ending tile", 99 - 4 * 5, fx.st.hpExpected);
    }
}
