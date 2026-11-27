package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.player.Command;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Baseline NPC damage contract: launch-resolved prayer (A3), dual channels (expected 30%
 * of max, worst full max, A9), projectile land delays, the minotaur's checked-at-landing
 * melee, and the attack-tick alignment metric.
 */
public class NpcDamageTest {
    /** Shaman at distance 6: magic delay 1+(1+6)/3 = 3 ticks; 28 max hit -> 8 expected. */
    @Test
    public void shamanHitLandsAfterMagicDelayWithDualChannels() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.finish();
        fx.ticks(3);
        assertEquals("nothing lands before the projectile arrives", 99, fx.st.hpExpected);
        fx.ticks(1);
        assertEquals("expected channel moves hp by maxHit x 0.30", 99 - 8, fx.st.hpExpected);
        assertEquals("worst channel floors hp by the full max hit", 99 - 28, fx.st.hpWorstFloor);
        assertEquals(8, fx.st.accExpectedDamageTaken);
    }

    @Test
    public void protectionPrayerAtLaunchBlocksEverything() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.st.overhead = 3;
        fx.finish();
        fx.ticks(21);
        assertEquals("prayed launches never enter the ring", 99, fx.st.hpExpected);
        assertEquals(99, fx.st.hpWorstFloor);
    }

    @Test
    public void aPrayerSwitchedOnTheHitTickRisksTheHitInTheWorstChannel() {
        // Blocked in the expected channel, but a flick sent for that very tick lands late
        // if the click does: the survival floor carries the hit's worst.
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.finish();
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(5);
        assertEquals(99, fx.st.hpExpected);
        assertEquals(99 - 28, fx.st.hpWorstFloor);
    }

    @Test
    public void prayingAfterLaunchDoesNothingForLaunchResolvedHits() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.finish();
        fx.tick(Command.NONE);
        // The orb is in flight; praying now must not save the hit (prayer resolved at launch).
        fx.tick(Command.overhead(Command.OVERHEAD_MAGIC));
        fx.ticks(4);
        assertEquals(99 - 8, fx.st.hpExpected);
    }

    /** The minotaur's melee lands one tick late and is prayer-checked at landing (A3). */
    @Test
    public void minotaurMeleeMustBePrayedAtTheSwingAndTheLanding() {
        // The capture's one sample read the prayer at the swing, the wiki at landing: until a
        // recording settles it, only Protect from Melee on both ticks blocks.
        ScenarioFixture late = new ScenarioFixture();
        late.player(16, 16);
        late.npc(0, "minotaur", 17, 16);
        late.finish();
        late.tick(Command.NONE);
        late.tick(Command.overhead(Command.OVERHEAD_MELEE));
        assertEquals("prayed only at landing", 99 - 22, late.st.hpExpected);

        ScenarioFixture dropped = new ScenarioFixture();
        dropped.player(16, 16);
        dropped.npc(0, "minotaur", 17, 16);
        dropped.finish();
        dropped.tick(Command.overhead(Command.OVERHEAD_MELEE));
        dropped.tick(Command.overhead(Command.OVERHEAD_OFF));
        assertEquals("prayed only at the swing", 99 - 22, dropped.st.hpExpected);

        ScenarioFixture both = new ScenarioFixture();
        both.player(16, 16);
        both.npc(0, "minotaur", 17, 16);
        both.st.overhead = 1;
        both.finish();
        both.tick(Command.NONE);
        both.tick(Command.NONE);
        assertEquals(99, both.st.hpExpected);
        assertEquals(99, both.st.hpWorstFloor);
    }

    @Test
    public void minotaurMeleeUnprayedLandsOneTickLate() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "minotaur", 17, 16);
        fx.finish();
        fx.tick(Command.NONE);
        assertEquals(99, fx.st.hpExpected);
        fx.tick(Command.NONE);
        assertEquals("74 x 0.30 = 22 lands the tick after launch", 99 - 22, fx.st.hpExpected);
        assertEquals(99 - 74, fx.st.hpWorstFloor);
    }

    /** Two styles launched the same tick is an off-tick alignment failure the eval counts. */
    @Test
    public void simultaneousStylesCountAlignmentEvents() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.npc(1, "warband_berserker", 16, 17);
        fx.st.npcSpawnTick[1] = 0;
        fx.finish();
        // Shaman launches on ticks 0, 5, 10, 15...; the berserker, spawned on tick 0 on its
        // north side, swings on its cycle ticks 3, 9, 15 — tick 15 collides.
        fx.ticks(16);
        assertTrue("magic + melee on tick 15 must register", fx.st.accAlignmentEvents >= 1);
    }
}
