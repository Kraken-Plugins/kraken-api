package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.Lanes;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.world.SimState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Manticore volley damage: three orbs on consecutive ticks with prayer checked per orb at
 * launch (blocking a triple = three prayers in pattern order), Mantimayhem doubling and
 * venom, and the unknown-pattern conservative branch (expected averaged over possible
 * styles, worst case maxed, melee last below Mantimayhem III).
 */
public class ManticoreVolleyTest {
    /** Charged "r" manticore: orbs ranged (36) tick 0, magic (31) tick 1, melee (31) tick 2,
     *  each landing a tick after it leaves (prayer read as it leaves). */
    private ScenarioFixture charged() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "manticore", 4, 15);
        fx.st.npcFlags[0] |= Lanes.FLAG_CHARGED;
        fx.st.npcLane[Lanes.PATTERN][0] = (byte) fx.pack.colosseum().patternId("r");
        return fx;
    }

    @Test
    public void unprayedTripleDealsExpectedPerOrb() {
        ScenarioFixture fx = charged();
        fx.finish();
        fx.ticks(3);
        assertEquals("the melee orb has not landed yet", 99 - 19, fx.st.hpExpected);
        fx.ticks(1);
        // 36*0.3 + 31*0.3 + 31*0.3 = 10 + 9 + 9
        assertEquals(99 - 28, fx.st.hpExpected);
    }

    @Test
    public void flickingInPatternOrderBlocksTheWholeTriple() {
        ScenarioFixture fx = charged();
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MISSILES));
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        fx.tick(Command.overhead(Command.OVERHEAD_MELEE));
        fx.ticks(1);
        assertEquals("perfect flick takes zero", 99, fx.st.hpExpected);
        assertEquals("each switch risks its orb in the worst channel", 99 - 36, fx.st.hpWorstFloor);
    }

    @Test
    public void wrongPrayerOnOneOrbLeaksThatOrb() {
        ScenarioFixture fx = charged();
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MISSILES));
        fx.tick(Command.NONE);
        fx.tick(Command.overhead(Command.OVERHEAD_MELEE));
        assertEquals("magic orb leaked through missiles", 99 - 9, fx.st.hpExpected);
    }

    @Test
    public void mantimayhemOneDoublesUnprayedOrbs() {
        ScenarioFixture fx = charged();
        fx.mod(Mods.MANTIMAYHEM, 1);
        fx.finish();
        fx.ticks(4);
        assertEquals("two projectiles per orb", 99 - 2 * 28, fx.st.hpExpected);
    }

    @Test
    public void mantimayhemTwoAppliesVenomOnUnprayedOrb() {
        ScenarioFixture fx = charged();
        fx.mod(Mods.MANTIMAYHEM, 2);
        fx.finish();
        fx.tick(Command.NONE);
        // Silence the manticore so the trajectory below is venom only.
        fx.st.npcFlags[0] &= ~SimState.FLAG_ALIVE;
        fx.tick(Command.NONE);
        assertTrue("venom active once the unprayed orb lands", (fx.st.venomState & 1) != 0);
        int hp = fx.st.hpExpected;
        assertEquals("tier II keeps tier I's doubling: 2 x 10 on the first orb", 99 - 20, hp);
        fx.ticks(28);
        assertEquals("venom has not proced yet", hp, fx.st.hpExpected);
        fx.ticks(1);
        assertEquals("first venom proc lands 30 ticks after application", hp - 6, fx.st.hpExpected);
    }

    @Test
    public void unknownPatternAveragesExpectedAndMaxesWorst() {
        ScenarioFixture fx = charged();
        fx.st.npcLane[Lanes.PATTERN][0] = -1;
        fx.finish();
        fx.ticks(4);
        // Orbs 1-2 unknown between ranged 36 and magic 31: expected (10+9)/2 = 9 each,
        // worst 36; the third orb is always melee below Mantimayhem III: expected 9.
        assertEquals(99 - (9 + 9 + 9), fx.st.hpExpected);
        assertTrue("worst channel assumed 36s", fx.st.hpWorstFloor <= 99 - 36);
    }

    @Test
    public void unknownPatternRespectsTheOverheadForItsKnownHalf() {
        ScenarioFixture fx = charged();
        fx.st.npcLane[Lanes.PATTERN][0] = -1;
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MISSILES));
        fx.tick(Command.NONE);
        // Orb 1 under missiles: ranged component blocked, magic leaks: (0 + 9)/2 = 4.
        assertEquals(99 - 4, fx.st.hpExpected);
    }
}
