package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Player-under-footprint escape (pipeline.json underfootEscape): an NPC covering the
 * player walks one cardinal step per tick until clear, never attacks while overlapped,
 * and resumes normal chase/attack once free. The in-game direction is random; the model's
 * deterministic hash stand-in must still be reproducible run-to-run.
 */
public class EscapeStepTest {

    private static boolean covers(int pos, int size, int px, int py) {
        int x = Coords.x(pos);
        int y = Coords.y(pos);
        return px >= x && px <= x + size - 1 && py >= y && py <= y + size - 1;
    }

    @Test
    public void overlappedNpcWalksCardinalStepsAndNeverAttacks() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        // 2x2 footprint anchored at (15,15) covers the player's tile.
        fx.npc(0, "jaguar_warrior", 15, 15);
        fx.finish();
        assertTrue(covers(fx.st.npcPos[0], 2, 16, 16));

        int prev = fx.st.npcPos[0];
        boolean everMoved = false;
        for (int t = 0; t < 12; t++) {
            boolean wasOverlapped = covers(prev, 2, 16, 16);
            fx.tick(Command.NONE);
            int cur = fx.st.npcPos[0];
            int dx = Math.abs(Coords.x(cur) - Coords.x(prev));
            int dy = Math.abs(Coords.y(cur) - Coords.y(prev));
            assertTrue("one cardinal step or a stand-still per tick", dx + dy <= 1);
            if (wasOverlapped) {
                assertEquals("no attacks while the footprint covers the player",
                        99, fx.st.hpExpected);
            }
            everMoved |= cur != prev;
            prev = cur;
        }
        assertTrue("the escape walk actually moves", everMoved);
    }

    @Test
    public void escapeWalkEndsTheOverlapAndCombatResumes() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "jaguar_warrior", 15, 15);
        fx.finish();
        boolean clearedAndHit = false;
        for (int t = 0; t < 40 && !clearedAndHit; t++) {
            fx.tick(Command.NONE);
            clearedAndHit = !covers(fx.st.npcPos[0], 2, 16, 16) && fx.st.hpExpected < 99;
        }
        assertTrue("once clear of the player the jaguar attacks again", clearedAndHit);
    }

    @Test
    public void escapeWalkIsDeterministic() {
        short[][] runs = new short[2][20];
        for (int run = 0; run < 2; run++) {
            ScenarioFixture fx = new ScenarioFixture();
            fx.player(16, 16);
            fx.npc(0, "jaguar_warrior", 15, 15);
            fx.finish();
            for (int t = 0; t < 20; t++) {
                fx.tick(Command.NONE);
                runs[run][t] = fx.st.npcPos[0];
            }
        }
        assertArrayEquals("identical states advance identically", runs[0], runs[1]);
    }
}
