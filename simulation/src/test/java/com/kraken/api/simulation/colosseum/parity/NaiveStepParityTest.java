package com.kraken.api.simulation.colosseum.parity;

import com.kraken.api.simulation.oracle.colosim.NpcType;
import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.colosseum.Fixture;
import com.kraken.api.simulation.colosseum.TestArenas;
import com.kraken.api.simulation.geom.Grid;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Naive NPC stepping parity: randomized placements of mixed footprint sizes with scripted
 * player teleports, compared tick-for-tick against the oracle (positions and attack
 * lines). Covers diagonal preference, the x-then-y fallback, the size-1 corner rule,
 * large-NPC collision, mob-vs-mob contention in processing order, the player-overlap
 * slide, and pillar stacking. Directed cases pin the classic shapes explicitly.
 */
public class NaiveStepParityTest {
    private static final NpcType[] TYPES = {
            NpcType.SERPENT_SHAMAN, NpcType.JAVELIN_COLOSSUS, NpcType.JAGUAR_WARRIOR,
            NpcType.MINOTAUR, NpcType.SHOCKWAVE_COLOSSUS, NpcType.REINFORCEMENT_SHAMAN,
            NpcType.MANTICORE,
    };

    @Test
    public void randomizedScenarios() {
        for (long seed = 1; seed <= 60; seed++) {
            runScenario(seed);
        }
    }

    private void runScenario(long seed) {
        Random rng = new Random(seed);
        Fixture fx = new Fixture();
        // The oracle freezes an NPC whose footprint covers the player; live play walks
        // it out (underfootEscape, live-verified). Parity here pins the naive stepper
        // only, so the escape deviation is switched off; EscapeStepTest covers it.
        fx.data.flags.underfootEscape = false;
        Grid grid = TestArenas.grid(fx.oracle);
        String manticoreStyle = rng.nextBoolean() ? "r" : "m";

        int[] p = openTileNear(fx.oracle, rng, 17, 17, 12);
        fx.player(p[0], p[1]);

        int mobCount = 2 + rng.nextInt(5);
        List<int[]> placed = new ArrayList<>();
        for (int m = 0; m < mobCount; m++) {
            NpcType type = TYPES[rng.nextInt(TYPES.length)];
            int size = sizeOf(type);
            for (int attempt = 0; attempt < 200; attempt++) {
                int x = 1 + rng.nextInt(33);
                int cy = 1 + rng.nextInt(32);
                if (!TestArenas.colosimLegalStatic(fx.oracle, grid, x, cy, size)
                        || overlapsAny(placed, x, cy, size)
                        || overlaps(x, cy, size, p[0], p[1], 1)) {
                    continue;
                }
                String extra = type == NpcType.MANTICORE
                        ? (rng.nextBoolean() ? manticoreStyle : "u" + manticoreStyle) : null;
                fx.place(type, x, cy, extra);
                placed.add(new int[]{x, cy, size});
                break;
            }
        }
        fx.finish();

        for (int t = 0; t < 40; t++) {
            if (rng.nextInt(10) < 3) {
                int[] next = openTileNear(fx.oracle, rng, fx.oracle.getPlayer().getX(),
                        fx.oracle.getPlayer().getY(), 4);
                fx.player(next[0], next[1]);
            }
            fx.stepAndAssert("seed " + seed);
        }
    }

    /** Random unblocked colosim tile within the box, falling back to the box centre. */
    private static int[] openTileNear(Simulation sim, Random rng, int cx, int cy, int radius) {
        for (int attempt = 0; attempt < 200; attempt++) {
            int x = cx - radius + rng.nextInt(2 * radius + 1);
            int y = cy - radius + rng.nextInt(2 * radius + 1);
            if (x >= 0 && x <= 34 && y >= 0 && y <= TestArenas.MAX_CY && !TestArenas.colosimBlocked(sim, x, y)) {
                return new int[]{x, y};
            }
        }
        return new int[]{cx, cy};
    }

    private static boolean overlapsAny(List<int[]> placed, int x, int cy, int size) {
        for (int[] p : placed) {
            if (overlaps(x, cy, size, p[0], p[1], p[2])) {
                return true;
            }
        }
        return false;
    }

    /** AABB overlap in colosim coordinates (footprints span [x, x+s-1] x [cy-s+1, cy]). */
    private static boolean overlaps(int x, int cy, int s, int x2, int cy2, int s2) {
        return !(x > x2 + s2 - 1 || x + s - 1 < x2 || cy - s + 1 > cy2 || cy < cy2 - s2 + 1);
    }

    private static int sizeOf(NpcType type) {
        switch (type) {
            case JAVELIN_COLOSSUS:
            case MINOTAUR:
            case SHOCKWAVE_COLOSSUS:
            case MANTICORE:
                return 3;
            case JAGUAR_WARRIOR:
                return 2;
            default:
                return 1;
        }
    }

    @Test
    public void playerOverlapSlide() {
        // Jaguar one corner off the player: its diagonal step would cover the player
        // tile, so the y component cancels and it slides to a cardinal side. The
        // minotaur approaches from range for large-footprint coverage of the same rule.
        Fixture fx = new Fixture();
        fx.player(16, 16);
        fx.place(NpcType.JAGUAR_WARRIOR, 17, 15, null);
        fx.place(NpcType.MINOTAUR, 10, 20, null);
        fx.finish();
        for (int t = 0; t < 12; t++) {
            fx.stepAndAssert("playerOverlapSlide");
        }
    }

    @Test
    public void sizeOneCornerRuleAtPillar() {
        // Shamans approaching diagonally past a pillar corner must not cut it.
        Fixture fx = new Fixture();
        fx.player(13, 13);
        fx.place(NpcType.SERPENT_SHAMAN, 6, 6, null);
        fx.place(NpcType.SERPENT_SHAMAN, 7, 5, null);
        fx.finish();
        for (int t = 0; t < 20; t++) {
            fx.stepAndAssert("cornerRule");
        }
    }

    @Test
    public void largeNpcContention() {
        // Two minotaurs and a jaguar converge on the same player; processing order
        // decides the contested tiles.
        Fixture fx = new Fixture();
        fx.player(17, 17);
        fx.place(NpcType.MINOTAUR, 8, 17, null);
        fx.place(NpcType.MINOTAUR, 26, 19, null);
        fx.place(NpcType.JAGUAR_WARRIOR, 17, 28, null);
        fx.finish();
        for (int t = 0; t < 25; t++) {
            fx.stepAndAssert("contention");
        }
    }

    @Test
    public void pillarStackAndStepOut() {
        // Ranged mobs stack behind a pillar (no route-finding), then the player steps
        // out and they gain LOS.
        Fixture fx = new Fixture();
        fx.player(7, 12);
        fx.place(NpcType.SHOCKWAVE_COLOSSUS, 14, 5, null);
        fx.place(NpcType.SERPENT_SHAMAN, 13, 8, null);
        fx.place(NpcType.JAVELIN_COLOSSUS, 17, 12, null);
        fx.finish();
        for (int t = 0; t < 15; t++) {
            fx.stepAndAssert("stack");
        }
        fx.player(13, 16);
        for (int t = 0; t < 10; t++) {
            fx.stepAndAssert("stepOut");
        }
    }
}
