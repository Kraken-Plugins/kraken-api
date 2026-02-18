package com.kraken.api.sim.colosim;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TrapMeleeTest {

    @BeforeAll
    public static void setUp() {
        LineOfSight.remove();
        LineOfSight.reset();
    }

    @Test
    public void testEmptyState() {
        assertTrue(LineOfSight._getMobs().isEmpty());
    }

    @Test
    public void testPlaceSingleMeleeNpc() {
        Types.Mob meleer = new Types.Mob(8, 13, 3, 8, 13, 0, null, null);
        LineOfSight._setSelected(new Types.Coordinates(meleer.x, meleer.y), meleer.type, null);
        LineOfSight.place();
        assertEquals(1, LineOfSight._getMobs().size());
        Types.Mob mob = LineOfSight._getMobs().get(0);
        assertEquals(8, mob.x);
        assertEquals(13, mob.y);
    }

    @Test
    public void testCheckNpcMovesTowardsPlayer() {
        LineOfSight._setSelected(new Types.Coordinates(7, 8), 0, null);
        LineOfSight.step(false);
        Types.Mob meleer = LineOfSight._getMobs().get(0);
        assertEquals(7, meleer.x);
        assertEquals(12, meleer.y);
    }

    @Test
    public void testCheckNpcGetsStuckOnPillar() {
        LineOfSight.step(false);
        Types.Mob meleer = LineOfSight._getMobs().get(0);
        assertEquals(7, meleer.x);
        assertEquals(12, meleer.y);
    }

    @Test
    public void testCheckNpcSlidesAcrossPillar() {
        LineOfSight._setSelected(new Types.Coordinates(11, 7), 0, null);
        LineOfSight.step(false);
        Types.Mob meleer = LineOfSight._getMobs().get(0);
        assertEquals(8, meleer.x);
        assertEquals(12, meleer.y);
    }

    @Test
    public void testCheckNpcComesAroundPillar() {
        LineOfSight.step(false);
        Types.Mob meleer = LineOfSight._getMobs().get(0);
        assertEquals(9, meleer.x);
        assertEquals(12, meleer.y);

        LineOfSight.step(false);
        meleer = LineOfSight._getMobs().get(0);
        assertEquals(10, meleer.x);
        assertEquals(12, meleer.y);

        LineOfSight.step(false);
        meleer = LineOfSight._getMobs().get(0);
        assertEquals(11, meleer.x);
        assertEquals(11, meleer.y);

        LineOfSight.step(false);
        meleer = LineOfSight._getMobs().get(0);
        assertEquals(11, meleer.x);
        assertEquals(10, meleer.y);

        LineOfSight.step(false);
        meleer = LineOfSight._getMobs().get(0);
        assertEquals(11, meleer.x);
        assertEquals(9, meleer.y);
        assertEquals(5, meleer.cooldown);
    }

    @Test
    public void testCheckNpcGetsCornerTrapped() {
        LineOfSight._setSelected(new Types.Coordinates(10, 7), 0, null);
        LineOfSight.step(false);
        Types.Mob meleer = LineOfSight._getMobs().get(0);
        assertEquals(11, meleer.x);
        assertEquals(9, meleer.y);
        // Cooldown should decrement
        assertEquals(4, meleer.cooldown);
    }

    @Test
    public void testCheckNpcIsStillCornerTrappedAfterMovingNorth() {
        LineOfSight._setSelected(new Types.Coordinates(10, 6), 0, null);
        LineOfSight.step(false);
        Types.Mob meleer = LineOfSight._getMobs().get(0);
        assertEquals(11, meleer.x);
        assertEquals(8, meleer.y);
    }
}
