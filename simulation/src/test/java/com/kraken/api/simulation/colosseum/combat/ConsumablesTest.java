package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.colosseum.Mods;
import com.kraken.api.simulation.player.Command;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Consumable rules: the food + karambwan + potion same-tick combo (food first), per-item
 * delays and attack pushback, tick-eating (P1 heals before P3 hits), the brew XOR restore
 * potion category, and anglerfish overheal with its Frailty disable.
 */
public class ConsumablesTest {
    @Test
    public void comboEatHealsAllThreeCategoriesInOneTick() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.st.hpExpected = 40;
        fx.finish();
        fx.tick(Command.consume(Command.CONSUME_FOOD | Command.CONSUME_KARAMBWAN | Command.CONSUME_BREW));
        assertEquals("22 + 18 + 16 in one tick", 96, fx.st.hpExpected);
        assertEquals(9, fx.st.foodCount);
        assertEquals(9, fx.st.karamCount);
        assertEquals(7, fx.st.brewDoses);
        // Food +3 and karambwan +2 pushed the cooldown to 5 in P1; the eat tick's own
        // housekeeping already counted one down, so the next attack fires 5 ticks out.
        assertEquals(4, fx.st.attackCooldown);
    }

    @Test
    public void perCategoryDelaysBlockRepeatsForThreeTicks() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.st.hpExpected = 20;
        fx.finish();
        fx.tick(Command.consume(Command.CONSUME_FOOD));
        assertEquals(42, fx.st.hpExpected);
        fx.tick(Command.consume(Command.CONSUME_FOOD));
        fx.tick(Command.consume(Command.CONSUME_FOOD));
        assertEquals("food delay swallows the repeats", 42, fx.st.hpExpected);
        fx.tick(Command.consume(Command.CONSUME_FOOD));
        assertEquals("edible again on the third tick after", 64, fx.st.hpExpected);
    }

    @Test
    public void tickEatingHealsBeforeTheHitLands() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 10, 16);
        fx.st.hpExpected = 30;
        fx.finish();
        fx.ticks(3);
        assertEquals(30, fx.st.hpExpected);
        // The orb lands this tick; eating now heals first (the classic tick-eat).
        fx.tick(Command.consume(Command.CONSUME_FOOD));
        assertEquals(30 + 22 - 8, fx.st.hpExpected);
        assertEquals("floor credits the heal before the worst roll", 30 + 22 - 28, fx.st.hpWorstFloor);
    }

    @Test
    public void brewAndRestoreShareThePotionCategory() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.st.prayerExpected = 40;
        fx.finish();
        fx.tick(Command.consume(Command.CONSUME_RESTORE));
        assertEquals("super restore gives 31 prayer", 71, fx.st.prayerExpected);
        fx.st.hpExpected = 40;
        fx.tick(Command.consume(Command.CONSUME_BREW));
        assertEquals("brew blocked by the shared potion delay", 40, fx.st.hpExpected);
        fx.ticks(2);
        fx.tick(Command.consume(Command.CONSUME_BREW));
        assertEquals(56, fx.st.hpExpected);
    }

    @Test
    public void anglerfishOverhealsAboveMaxUnlessFrail() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.finish();
        fx.tick(Command.consume(Command.CONSUME_FOOD));
        assertEquals("anglerfish overheals to 121", 121, fx.st.hpExpected);

        ScenarioFixture frail = new ScenarioFixture();
        frail.mod(Mods.FRAILTY, 1);
        frail.st.hpExpected = 89;
        frail.finish();
        assertEquals("Frailty I cuts max hp 10%", 89, frail.frame.playerMaxHp);
        frail.tick(Command.consume(Command.CONSUME_FOOD));
        assertEquals("overhealing disabled under Frailty", 89, frail.st.hpExpected);
    }
}
