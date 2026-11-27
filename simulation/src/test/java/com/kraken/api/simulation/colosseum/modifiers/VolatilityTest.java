package com.kraken.api.simulation.colosseum.modifiers;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Grid;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Volatility blasts as a body is deleted (the kill + 4 for a shaman; RSProx capture
 * analysis section 11): radius size + 1 (tier I) around the dying footprint, typeless damage
 * to a player inside it, chained NPC-to-NPC explosions, and tier III molten sand on the
 * corpse's centre.
 */
public class VolatilityTest {
    @Test
    public void playerInBlastRadiusTakesTypelessDamage() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(12, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.st.npcHp[0] = 10;
        fx.mod(Mods.VOLATILITY, 1);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(2);
        assertFalse(fx.st.alive(0));
        assertEquals("no blast on the kill", 99, fx.st.hpExpected);
        fx.ticks(4);
        assertEquals("15 expected blast through the prayer as the body is deleted (kill + 4)",
                99 - 15, fx.st.hpExpected);
        assertEquals(99 - 30, fx.st.hpWorstFloor);
    }

    @Test
    public void playerOutsideBlastRadiusIsSafe() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.st.npcHp[0] = 10;
        fx.mod(Mods.VOLATILITY, 1);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(8);
        assertFalse(fx.st.alive(0));
        assertEquals("distance 6 is outside radius 2", 99, fx.st.hpExpected);
    }

    @Test
    public void blastsChainThroughAdjacentEnemies() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(20, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.npc(1, "serpent_shaman", 12, 16);
        fx.st.npcHp[0] = 10;
        fx.st.npcHp[1] = 12;
        fx.mod(Mods.VOLATILITY, 1);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(4);
        assertFalse("shot target dead", fx.st.alive(0));
        assertTrue("neighbour alive until the body is deleted", fx.st.alive(1));
        fx.ticks(4);
        assertFalse("neighbour chained by the 15-damage blast", fx.st.alive(1));
        assertEquals(2, fx.st.accKills);
    }

    @Test
    public void tierThreeLeavesSandAtTheCorpse() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.st.npcHp[0] = 10;
        fx.mod(Mods.VOLATILITY, 3);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(6);
        assertFalse("no sand before the body is deleted", Grid.get(fx.st.moltenSand, 10, 16));
        fx.ticks(1);
        assertTrue("molten sand at the corpse center", Grid.get(fx.st.moltenSand, 10, 16));
    }
}
