package com.kraken.api.simulation.colosseum.parity;

import com.kraken.api.simulation.oracle.colosim.NpcType;
import com.kraken.api.simulation.colosseum.Fixture;
import com.kraken.api.simulation.hits.Style;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Manticore machinery parity: charge on first sight (10 ticks), 3-orb volleys on
 * consecutive ticks in pattern order (melee last below Mantimayhem III), only one volley
 * start per tick with other ready manticores pushed back 5, and 10-tick attack cycles.
 */
public class ManticoreStaggerParityTest {
    @Test
    public void twoChargedManticoresStagger() {
        Fixture fx = new Fixture();
        fx.player(16, 16);
        fx.place(NpcType.MANTICORE, 4, 15, "r");
        fx.place(NpcType.MANTICORE, 27, 18, "r");
        fx.finish();

        List<int[]> orbEvents = new ArrayList<>();
        for (int t = 0; t < 25; t++) {
            fx.stepAndAssert("stagger");
            for (int slot = 0; slot < 2; slot++) {
                if ((fx.ourLine[slot] & 1) != 0) {
                    orbEvents.add(new int[]{t, slot, fx.ourLine[slot] >> 8 & 0xff});
                }
            }
        }
        // First manticore volleys ticks 0-2 (ranged, magic, melee), second is staggered
        // to 5-7, then both repeat on their 10-tick cycles.
        int[][] expected = {
                {0, 0, Style.RANGED}, {1, 0, Style.MAGIC}, {2, 0, Style.MELEE},
                {5, 1, Style.RANGED}, {6, 1, Style.MAGIC}, {7, 1, Style.MELEE},
                {10, 0, Style.RANGED}, {11, 0, Style.MAGIC}, {12, 0, Style.MELEE},
                {15, 1, Style.RANGED}, {16, 1, Style.MAGIC}, {17, 1, Style.MELEE},
                {20, 0, Style.RANGED}, {21, 0, Style.MAGIC}, {22, 0, Style.MELEE},
        };
        assertEquals("orb event count", expected.length, orbEvents.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals("orb tick " + i, expected[i][0], orbEvents.get(i)[0]);
            assertEquals("orb slot " + i, expected[i][1], orbEvents.get(i)[1]);
            assertEquals("orb style " + i, expected[i][2], orbEvents.get(i)[2]);
        }
    }

    @Test
    public void threeManticoresOneStartPerTick() {
        Fixture fx = new Fixture();
        fx.player(16, 16);
        fx.place(NpcType.MANTICORE, 4, 15, "m");
        fx.place(NpcType.MANTICORE, 27, 18, "m");
        fx.place(NpcType.MANTICORE, 15, 28, "m");
        fx.finish();
        for (int t = 0; t < 40; t++) {
            fx.stepAndAssert("threeWay");
        }
    }

    @Test
    public void unchargedChargesOnSightThenVolleysTenLater() {
        // Uncharged manticore with a known pattern, player initially out of its 15-tile
        // range; the manticore closes distance, charges on first sight, volleys 10 later.
        Fixture fx = new Fixture();
        fx.player(30, 17);
        fx.place(NpcType.MANTICORE, 2, 16, "um");
        fx.finish();

        int firstOrbTick = -1;
        int firstOrbStyle = -1;
        for (int t = 0; t < 30; t++) {
            fx.stepAndAssert("unchargedSolo");
            if (firstOrbTick < 0 && (fx.ourLine[0] & 1) != 0) {
                firstOrbTick = t;
                firstOrbStyle = fx.ourLine[0] >> 8 & 0xff;
            }
        }
        assertEquals("magic-first pattern opens with a magic orb", Style.MAGIC, firstOrbStyle);
        // Anchor starts 29 tiles from the player; it must close to clamped Chebyshev
        // distance 15, charge, then fire 10 ticks later — parity already pinned the exact
        // tick; this guards the charge-delay property stays meaningful.
        assertEquals("charge takes 10 ticks after gaining range", true, firstOrbTick >= 10);
    }

    @Test
    public void mixedChargedAndUnchargedShareStyle() {
        Fixture fx = new Fixture();
        fx.player(16, 16);
        fx.place(NpcType.MANTICORE, 4, 15, "r");
        fx.place(NpcType.MANTICORE, 26, 14, "ur");
        fx.finish();
        for (int t = 0; t < 40; t++) {
            fx.stepAndAssert("mixedCharge");
        }
    }
}
