package com.kraken.api.sim.colosim;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FromWaveStartTest {

    @BeforeAll
    public static void setUp() {
        LineOfSight.setFromWaveStart(true);
    }

    @AfterAll
    public static void tearDown() {
        LineOfSight.setFromWaveStart(false);
    }

    @Test
    public void testMovementChecks() {
        LineOfSight.remove();
        Types.Mob javelin = new Types.Mob(20, 13, 2, 20, 13, 0, null, null);

        // place single javelin
        LineOfSight._setSelected(new Types.Coordinates(javelin.x, javelin.y), javelin.type, null);
        LineOfSight.place();
        // Check if mob is placed correctly (simplified check)
        assertEquals(1, LineOfSight._getMobs().size());

        // check npc doesn't move on first tick
        LineOfSight._setSelected(new Types.Coordinates(7, 8), 0, null);
        LineOfSight.step(false);
        Types.Mob mob = LineOfSight._getMobs().get(0);
        assertEquals(20, mob.x);
        assertEquals(13, mob.y);

        // check npc moves on second tick
        LineOfSight.step(false);
        mob = LineOfSight._getMobs().get(0);
        assertEquals(19, mob.x);
        assertEquals(12, mob.y);
    }

    @Test
    public void testAttackDelayChecks() {
        LineOfSight.remove();
        Types.Mob javelin = new Types.Mob(12, 12, 2, 12, 12, 0, null, null);

        // place single javelin
        LineOfSight._setSelected(new Types.Coordinates(javelin.x, javelin.y), javelin.type, null);
        LineOfSight.place();
        LineOfSight._setSelected(new Types.Coordinates(16, 16), 0, null);
        assertEquals(1, LineOfSight._getMobs().size());

        // check npc doesn't attack on first tick
        LineOfSight.step(false);
        Types.Mob mob = LineOfSight._getMobs().get(0);
        assertTrue(mob.cooldown <= 0); // Should be idle/cooling down

        // check npc doesn't attack on second tick
        LineOfSight.step(false);
        mob = LineOfSight._getMobs().get(0);
        // it moves for one tick but doesn't attack
        assertEquals(13, mob.x);
        assertEquals(13, mob.y);

        // check npc doesn't attack on third tick
        LineOfSight.step(false);
        mob = LineOfSight._getMobs().get(0);
        assertTrue(mob.cooldown <= 0);

        // check npc fires on the fourth tick
        LineOfSight.step(false);
        mob = LineOfSight._getMobs().get(0);
        assertEquals(5, mob.cooldown);
    }
}
