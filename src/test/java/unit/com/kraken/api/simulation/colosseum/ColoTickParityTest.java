package unit.com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.colosseum.ColoCoords;
import com.kraken.api.simulation.colosseum.ColoFrame;
import com.kraken.api.simulation.colosseum.ColoGrid;
import com.kraken.api.simulation.colosseum.ColoNpcType;
import com.kraken.api.simulation.colosseum.ColoScratch;
import com.kraken.api.simulation.colosseum.ColoState;
import com.kraken.api.simulation.colosseum.ColoTick;
import com.kraken.api.simulation.colosseum.PlayerCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unit.com.kraken.api.simulation.colosseum.ColoTestArenas.up;

/**
 * Parity tests against the community wave simulator's verified behaviours, on identical
 * (coordinate-flipped) geometry. Colosim's 1-based "tick N" equals engine tick N-1.
 */
class ColoTickParityTest {

    private static void advance(ColoState state, ColoScratch scratch, int times) {
        for (int i = 0; i < times; i++) {
            ColoTick.advance(state, PlayerCommand.NONE, scratch);
        }
    }

    @Test
    void waveStartBlocksMovementOnFirstTick() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, true, ColoNpcType.JAVELIN_COLOSSUS);
        ColoState state = ColoTestArenas.state(frame, up(7, 8), up(20, 13));
        ColoScratch scratch = new ColoScratch();

        ColoTick.advance(state, PlayerCommand.NONE, scratch);
        assertEquals(up(20, 13), state.npcPos(0), "no movement on wave tick 0");

        ColoTick.advance(state, PlayerCommand.NONE, scratch);
        assertEquals(up(19, 12), state.npcPos(0), "diagonal step toward player on tick 1");
    }

    @Test
    void waveStartDelaysFirstAttackUntilTickThree() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, true, ColoNpcType.JAVELIN_COLOSSUS);
        ColoState state = ColoTestArenas.state(frame, up(16, 16), up(12, 12));
        ColoScratch scratch = new ColoScratch();
        ColoTestArenas.AttackRecorder recorder = new ColoTestArenas.AttackRecorder();
        scratch.attackListener = recorder;

        ColoTick.advance(state, PlayerCommand.NONE, scratch);
        assertEquals(-1, state.npcCooldown(0));

        ColoTick.advance(state, PlayerCommand.NONE, scratch);
        assertEquals(up(13, 13), state.npcPos(0));
        assertEquals(-2, state.npcCooldown(0));

        ColoTick.advance(state, PlayerCommand.NONE, scratch);
        assertEquals(-3, state.npcCooldown(0));
        assertTrue(recorder.events.isEmpty(), "no attacks before wave tick 3");

        ColoTick.advance(state, PlayerCommand.NONE, scratch);
        assertEquals(5, state.npcCooldown(0), "attack resets cooldown to attack speed");
        assertEquals(List.of(3), recorder.attackTicks(0));
    }

    @Test
    void onlyOneManticoreFiresPerTickAndOthersGetDelayed() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.MANTICORE, ColoNpcType.MANTICORE);
        ColoState state = ColoTestArenas.state(frame, up(16, 18), up(9, 17), up(10, 17));
        state.setManticoreState(0, ColoTick.PATTERN_RANGED_FIRST, true, 0);
        state.setManticoreState(1, ColoTick.PATTERN_RANGED_FIRST, true, 0);
        ColoScratch scratch = new ColoScratch();
        ColoTestArenas.AttackRecorder recorder = new ColoTestArenas.AttackRecorder();
        scratch.attackListener = recorder;

        ColoTick.advance(state, PlayerCommand.NONE, scratch);
        assertEquals(1, recorder.events.size(), "exactly one manticore may begin firing per tick");
        assertEquals(0, recorder.events.get(0)[0]);
        assertEquals(5, state.npcCooldown(1), "ready manticore is staggered by 5 ticks");
        assertEquals(2, state.manticoreOrbsRemaining(0));

        // Orbs continue on consecutive ticks: ranged, magic, melee for an "r" pattern.
        advance(state, scratch, 2);
        assertEquals(
                List.of(ColoTick.STYLE_RANGED, ColoTick.STYLE_MAGIC, ColoTick.STYLE_MELEE),
                recorder.styles(0)
        );

        // The staggered manticore fires 5 ticks after the first (engine tick 5).
        advance(state, scratch, 3);
        assertEquals(List.of(5), recorder.attackTicks(1).subList(0, 1));
    }

    @Test
    void unknownManticoreCopiesKnownPatternWhenChargingSameTick() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.MANTICORE, ColoNpcType.MANTICORE);
        ColoState state = ColoTestArenas.state(frame, up(16, 18), up(3, 19), up(9, 17));
        state.setManticoreState(0, ColoTick.PATTERN_UNKNOWN, false, 0);
        state.setManticoreState(1, ColoTick.PATTERN_MAGIC_FIRST, false, 0);
        ColoScratch scratch = new ColoScratch();

        ColoTick.advance(state, PlayerCommand.NONE, scratch);
        assertTrue(state.npcChargingStarted(0));
        assertTrue(state.npcChargingStarted(1));
        assertEquals(ColoTick.PATTERN_MAGIC_FIRST, state.manticorePattern(0), "unknown copies the known pattern");
        assertEquals(ColoTick.PATTERN_MAGIC_FIRST, state.manticorePattern(1));
        assertEquals(10, state.npcCooldown(0), "charge takes 10 ticks");
        assertEquals(10, state.npcCooldown(1));
    }

    @Test
    void stackedShamansBehindPillarMatchColosimAttackTicks() {
        // Direct port of the colosim PillarStackBehaviorTest scenario (coordinates flipped):
        // shamans at colosim (11,7),(11,8),(11,9); player at (7,9), stepping to (7,7) before
        // colosim tick 5 and back to (7,9) before colosim tick 9.
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(
                grid, false,
                ColoNpcType.SERPENT_SHAMAN, ColoNpcType.SERPENT_SHAMAN, ColoNpcType.SERPENT_SHAMAN
        );
        ColoState state = ColoTestArenas.state(frame, up(7, 9), up(11, 7), up(11, 8), up(11, 9));
        ColoScratch scratch = new ColoScratch();
        ColoTestArenas.AttackRecorder recorder = new ColoTestArenas.AttackRecorder();
        scratch.attackListener = recorder;

        for (int colosimTick = 1; colosimTick <= 9; colosimTick++) {
            if (colosimTick == 5) {
                state.teleportPlayer(up(7, 7));
            }
            if (colosimTick == 9) {
                state.teleportPlayer(up(7, 9));
            }
            ColoTick.advance(state, PlayerCommand.NONE, scratch);

            if (colosimTick == 1) {
                assertEquals(10, ColoCoords.x(state.npcPos(0)), "mob0 x after colosim tick 1");
            }
            if (colosimTick == 2) {
                assertEquals(9, ColoCoords.x(state.npcPos(0)));
            }
            if (colosimTick == 3) {
                assertEquals(8, ColoCoords.x(state.npcPos(0)));
            }
            if (colosimTick == 4) {
                assertEquals(up(7, 7), state.npcPos(0), "mob0 reaches colosim (7,7) on tick 4");
            }
            if (colosimTick == 5) {
                assertEquals(ColoTestArenas.MAP_MAX_Y - 7, ColoCoords.y(state.npcPos(1)), "mob1 y on tick 5");
                assertEquals(ColoTestArenas.MAP_MAX_Y - 8, ColoCoords.y(state.npcPos(2)), "mob2 y on tick 5");
            }
        }

        // Colosim attack ticks are 1-based: mob0 attacks ticks {4, 9}, mob1 {5}, mob2 {}.
        assertEquals(List.of(3, 8), recorder.attackTicks(0));
        assertEquals(List.of(4), recorder.attackTicks(1));
        assertEquals(List.of(), recorder.attackTicks(2));
    }
}
