package com.kraken.api.sim.colosim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PlaceTest {

    @BeforeEach
    public void setUp() {
        LineOfSight.remove();
        LineOfSight.reset();
    }

    @Test
    public void testEmptyState() {
        assertTrue(LineOfSight._getMobs().isEmpty());
    }

    @Test
    public void testDisallowPlacingMode0Player() {
        LineOfSight._setSelected(new Types.Coordinates(1, 1), 0, null);
        LineOfSight.place();
        assertTrue(LineOfSight._getMobs().isEmpty());
    }

    @Test
    public void testPlaceSingleNpc() {
        LineOfSight._setSelected(new Types.Coordinates(1, 1), 1, null);
        LineOfSight.place();
        assertEquals(1, LineOfSight._getMobs().size());
        Types.Mob mob = LineOfSight._getMobs().get(0);
        assertEquals(1, mob.x);
        assertEquals(1, mob.y);
        assertEquals(1, mob.type);
    }

    @Test
    public void testDisallowPlacingNpcsOnTopOfEachOther() {
        LineOfSight._setSelected(new Types.Coordinates(1, 1), 1, null);
        LineOfSight.place();
        LineOfSight._setSelected(new Types.Coordinates(1, 1), 1, null);
        LineOfSight.place();
        assertEquals(1, LineOfSight._getMobs().size());
    }
}
