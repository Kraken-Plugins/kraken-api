package com.kraken.api.simulation.colosseum.modifiers;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.Lanes;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Bees! swarms (RSProx capture analysis section 11): a 2x2 that crawls a tile toward the
 * player every 2 ticks through collision, stings every tick the player is under it for
 * unblockable damage, dies to one hit, and comes back on its add tile 55 ticks after the
 * kill.
 */
public class BeesTest {
    @Test
    public void swarmCrawlsEveryTwoTicksAndStingsWhileThePlayerIsUnderIt() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "bee_swarm", 16, 18);
        fx.mod(Mods.BEES, 1);
        fx.finish();
        fx.ticks(1);
        assertEquals("no step on the first tick", Coords.pack(16, 18), fx.st.npcPos[0]);
        fx.ticks(1);
        assertEquals(Coords.pack(16, 17), fx.st.npcPos[0]);
        fx.ticks(2);
        assertEquals("its 2x2 covers the player", Coords.pack(16, 16), fx.st.npcPos[0]);
        assertEquals("a sting on arrival: 5..10, 7 expected", 99 - 7, fx.st.hpExpected);
        fx.ticks(2);
        assertEquals("a sting every tick, prayer irrelevant", 99 - 21, fx.st.hpExpected);
    }

    @Test
    public void beesIgnoreCollisionAndBodies() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(5, 9);
        // Due east of the pillar at x 8-10, y 8-10: a walker would detour; the swarm crawls
        // straight over the pillar tiles.
        fx.npc(0, "bee_swarm", 12, 9);
        fx.mod(Mods.BEES, 1);
        fx.finish();
        fx.ticks(4);
        assertEquals("two steps straight west, onto the pillar", Coords.pack(10, 9), fx.st.npcPos[0]);
        assertFalse("no occupancy footprint", (fx.st.occupancy[9] & 1L << 10) != 0);
    }

    @Test
    public void killedSwarmRespawnsAtItsHomeFiftyFiveTicksAfterTheKill() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "bee_swarm", 16, 18);
        fx.mod(Mods.BEES, 1);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0));
        fx.ticks(2);
        assertFalse("one hit kills the 1-hp swarm", fx.st.alive(0));
        assertEquals(1, fx.st.accLane[Lanes.ACC_BEE_KILLS]);
        assertEquals("bee kills are not wave kills", 0, fx.st.accKills);
        // Killed on tick 2: back on tick 57 (capture section 11).
        fx.ticks(54);
        assertFalse(fx.st.alive(0));
        fx.ticks(1);
        assertTrue("respawns 55 ticks after the kill", fx.st.alive(0));
        assertEquals("on its add tile", Coords.pack(16, 18), fx.st.npcPos[0]);
    }
}
