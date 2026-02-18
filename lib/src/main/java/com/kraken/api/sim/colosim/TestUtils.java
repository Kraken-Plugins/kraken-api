package com.kraken.api.sim.colosim;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils {

    public static Types.MobSpec createMob(int x, int y, int type, String extra) {
        return new Types.MobSpec(x, y, type, extra);
    }

    public static void checkMove(Types.Mob mutableNpc, int x, int y, int attacked) {
        mutableNpc.x = x;
        mutableNpc.y = y;
        if (attacked == 0) {
            mutableNpc.cooldown--;
        } else {
            mutableNpc.cooldown = attacked;
        }
        // In Java, we can't easily check "toContainEqual" for objects unless equals() is overridden.
        // We'll assume we're checking the first mob or finding it.
        boolean found = false;
        for (Types.Mob mob : LineOfSight._getMobs()) {
            if (mob.x == mutableNpc.x && mob.y == mutableNpc.y && mob.type == mutableNpc.type && mob.cooldown == mutableNpc.cooldown) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Mob not found with expected state: " + mutableNpc);
    }

    public static void checkPosition(Types.Mob npc, int x, int y) {
        assertEquals(x, npc.x);
        assertEquals(y, npc.y);
    }

    public static void checkIdleStep(Types.Mob npc) {
        npc.cooldown--;
        boolean found = false;
        for (Types.Mob mob : LineOfSight._getMobs()) {
            if (mob.x == npc.x && mob.y == npc.y && mob.type == npc.type && mob.cooldown == npc.cooldown) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Mob not found with expected state: " + npc);
    }
}
