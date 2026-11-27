package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Player combat: cadence and hit delays (+1 because NPCs process first), kill credit and
 * the wave-clear check, spec energy and multiplier, Myopia range cuts, melee adjacency,
 * and the Venator-style bounce.
 */
public class PlayerCombatTest {
    /** Ranged at distance 6: delay 1+(3+6)/6 = 2, +1 receiver rule = 3 -> lands tick 3. */
    @Test
    public void rangedHitLandsAfterDelayOnCadence() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(2);
        assertEquals(125, fx.st.npcHp[0]);
        fx.ticks(1);
        assertEquals("first hit lands tick 3", 105, fx.st.npcHp[0]);
        fx.ticks(4);
        assertEquals("second attack on tick 4 lands tick 7", 85, fx.st.npcHp[0]);
        assertEquals(40, fx.st.accDamageDealt);
    }

    @Test
    public void killCreditsAndWaveClear() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.st.npcHp[0] = 15;
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(3);
        assertEquals(1, fx.st.accKills);
        assertEquals("damage capped at remaining hp", 15, fx.st.accDamageDealt);
        assertEquals("wave cleared the tick the last enemy died", 3, fx.st.waveClearTick);
        assertEquals("interaction dropped on death", -1, fx.st.targetSlot);
    }

    @Test
    public void specDoublesOneAttackAndConsumesEnergy() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.spec() | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(7);
        assertEquals("40 spec + 20 normal", 125 - 60, fx.st.npcHp[0]);
        assertEquals(500, fx.st.specEnergy10);
    }

    @Test
    public void myopiaCutsPlayerAttackRange() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.mod(Mods.MYOPIA, 3);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        // Range 10 - 6 = 4 cannot reach distance 6, so the attack click runs two tiles in
        // and attacks on arrival, as the game does.
        assertEquals("ran in to distance 4", Coords.pack(14, 16), fx.st.playerPos);
        assertEquals("attacked on the arrival tick", 1, fx.st.pendingCount);
        fx.ticks(6);
        assertTrue(fx.st.npcHp[0] < 125);
    }

    @Test
    public void meleeNeedsCardinalAdjacencyToTheFootprint() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "minotaur", 17, 17);
        fx.equip(0, Style.MELEE, 5, 1, 30);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MELEE));
        assertEquals("diagonal corner cannot melee", 225, fx.st.npcHp[0]);
        // The minotaur steps to a cardinal side next tick; the melee then connects.
        fx.ticks(2);
        assertEquals(195, fx.st.npcHp[0]);

        ScenarioFixture adj = new ScenarioFixture();
        adj.player(16, 16);
        adj.npc(0, "minotaur", 17, 16);
        adj.equip(0, Style.MELEE, 5, 1, 30);
        adj.finish();
        adj.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MELEE));
        adj.ticks(1);
        assertEquals("melee lands next tick", 195, adj.st.npcHp[0]);
    }

    @Test
    public void venatorBounceHitsASecondNearbyTarget() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.npc(1, "serpent_shaman", 12, 17);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.frame.equipSets[0].bounce = true;
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(3);
        assertEquals(105, fx.st.npcHp[0]);
        assertEquals("half-damage ricochet", 115, fx.st.npcHp[1]);
    }

    @Test
    public void playerUnderLargeNpcCannotShootIt() {
        ScenarioFixture fx = new ScenarioFixture();
        // Pin the colossus in place: this test is about the player-side overlap rule,
        // not the NPC's escape walk.
        fx.data.flags.underfootEscape = false;
        fx.npc(0, "javelin_colossus", 15, 15);
        fx.player(16, 16);
        fx.equip(0, Style.RANGED, 4, 10, 20);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MISSILES));
        fx.ticks(2);
        assertTrue("no hit launched from under the footprint", fx.st.pendingCount == 0);
    }
}
