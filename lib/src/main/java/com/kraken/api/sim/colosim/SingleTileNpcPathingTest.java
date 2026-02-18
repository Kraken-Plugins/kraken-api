package com.kraken.api.sim.colosim;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SingleTileNpcPathingTest {

    @BeforeAll
    public static void setUp() {
        LineOfSight.remove();
        LineOfSight.reset();
        Types.Mob shaman = new Types.Mob(8, 7, 1, 8, 7, 0, null, null);
        LineOfSight._setSelected(new Types.Coordinates(shaman.x, shaman.y), shaman.type, null);
        LineOfSight.place();
    }

    @Test
    public void check1x1NpcCannotMoveDiagonallyToPathToPlayer() {
        LineOfSight._setSelected(new Types.Coordinates(7, 20), 0, null);
        LineOfSight.step(false);
        Types.Mob shaman = LineOfSight._getMobs().get(0);
        assertEquals(7, shaman.x);
        assertEquals(7, shaman.y);

        LineOfSight.step(false);
        shaman = LineOfSight._getMobs().get(0);
        assertEquals(7, shaman.x);
        assertEquals(8, shaman.y);
    }

    @Test
    public void check1x1NpcCannotMoveDiagonallyToAttackPlayer() {
        LineOfSight._setSelected(new Types.Coordinates(10, 7), 0, null);
        LineOfSight.step(false);
        Types.Mob shaman = LineOfSight._getMobs().get(0);
        assertEquals(7, shaman.x);
        assertEquals(7, shaman.y);
        assertEquals(5, shaman.cooldown);
    }
}
