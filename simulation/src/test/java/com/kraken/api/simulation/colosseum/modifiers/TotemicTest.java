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
 * Totemic (RSProx capture analysis section 11): a wave-holding enemy dropping to half HP
 * spawns a 1-hp totem on the tile SW of its SW tile that heals it 30% of max (at most 75) at
 * spawn + 7 and every 6 after; each heal is in flight 6 ticks and fizzles if the totem dies
 * first; totem kills are utility kills, not wave kills.
 */
public class TotemicTest {
    private ScenarioFixture wounded() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.totemPoolSlot(1);
        fx.st.npcHp[0] = 70;
        fx.mod(Mods.TOTEMIC, 1);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        return fx;
    }

    @Test
    public void totemSpawnsWhenTheEnemyDropsToHalf() {
        ScenarioFixture fx = wounded();
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(2);
        assertFalse("70 hp is above half of 125", fx.st.alive(1));
        fx.ticks(1);
        assertTrue("50/125 after the hit spawns the totem", fx.st.alive(1));
        assertEquals(1, fx.st.packLane[Lanes.ALIVE_TOTEMS]);
        assertTrue("beside the beneficiary",
                Math.abs(Coords.x(fx.st.npcPos[1]) - 10) <= 2
                        && Math.abs(Coords.y(fx.st.npcPos[1]) - 16) <= 2);
    }

    @Test
    public void totemHealsThirtyPercentOnItsCadence() {
        ScenarioFixture fx = wounded();
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(2);
        // The hit lands on tick 3 (shaman at 50) and the totem spawns; stop attacking so
        // the heal is isolated. The cast comes on spawn + 7 = 10, in flight for 6: lands 16.
        fx.st.targetSlot = -1;
        fx.ticks(13);
        assertEquals(50, fx.st.npcHp[0]);
        fx.ticks(1);
        assertEquals("30% of 125 = 37", 87, fx.st.npcHp[0]);
        assertTrue(fx.st.accLane[Lanes.ACC_TOTEM_TICKS] > 0);
    }

    @Test
    public void killingTheTotemFizzlesTheInFlightHeal() {
        ScenarioFixture fx = wounded();
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(2);
        fx.st.targetSlot = -1;
        fx.ticks(1);
        assertTrue("totem up on tick 3", fx.st.alive(1));
        assertEquals("on the tile SW of the shaman", Coords.pack(9, 15), fx.st.npcPos[1]);
        // The heal is cast on tick 10; attack the totem on tick 11: the kill lands on 14 and
        // the heal, mid-flight, fizzles at its tick-16 landing.
        fx.ticks(7);
        fx.tick(Command.attack(1));
        fx.ticks(6);
        assertFalse("1-hp totem dies to one hit", fx.st.alive(1));
        assertEquals("in-flight heal fizzled with its totem", 50, fx.st.npcHp[0]);
        assertEquals(1, fx.st.accLane[Lanes.ACC_TOTEM_KILLS]);
        assertEquals("totem kills are not wave kills", 0, fx.st.accKills);
    }
}
