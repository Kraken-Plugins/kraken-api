package unit.plugins.colosseumv2;

import org.junit.jupiter.api.Test;
import plugins.colosseumv2.engine.ColosseumPathing;
import plugins.colosseumv2.engine.GridCollisionMap;
import plugins.colosseumv2.engine.PrayerDecision;
import plugins.colosseumv2.engine.PrayerEngine;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unit.plugins.colosseumv2.EngineTestSupport.berserker;
import static unit.plugins.colosseumv2.EngineTestSupport.input;
import static unit.plugins.colosseumv2.EngineTestSupport.jaguar;
import static unit.plugins.colosseumv2.EngineTestSupport.openMap;

/**
 * The "corner trap": a 1x1 NPC (A) holds the pillar's southeast corner tile with the player
 * cardinal-west of it. Another melee NPC (B) wedged on the pillar's east side sits one
 * DIAGONAL tile from the player — it can neither attack (melee needs cardinal adjacency) nor
 * move (its diagonal step is the player's tile, west is the pillar, and south is occupied by
 * NPC A). The engine must never treat B as an active threat.
 *
 * <p>Geometry (scene coords, y grows north): pillar x 8-10 / y 10-12, player (10,9),
 * NPC A (11,9), NPC B (11,10).
 */
class CornerTrapPathingTest {

    private GridCollisionMap trapMap() {
        GridCollisionMap map = openMap();
        map.block(8, 10, 3, 3);
        return map;
    }

    @Test
    void diagonalMeleeNpcCannotAttack() {
        GridCollisionMap map = trapMap();

        // NPC B, one diagonal tile from the player: never attackable for a melee NPC.
        assertFalse(ColosseumPathing.canAttack(map, 11, 10, 1, 1, true, 10, 9));

        // NPC A, cardinal east of the player: attackable.
        assertTrue(ColosseumPathing.canAttack(map, 11, 9, 1, 1, true, 10, 9));
    }

    @Test
    void trappedNpcNeverProducesAnAttackForecast() {
        GridCollisionMap map = trapMap();

        List<ColosseumPathing.Mover> movers = List.of(
                new ColosseumPathing.Mover(5, 11, 9, 1, 1, true, 101),   // NPC A on the corner
                new ColosseumPathing.Mover(6, 11, 10, 1, 1, true, 101)); // NPC B, trapped

        Map<Integer, ColosseumPathing.Forecast> forecasts =
                ColosseumPathing.forwardSimulate(map, movers, null, 10, 9, 100, 30, true);

        assertTrue(forecasts.get(5).firstAttackTick > 100, "NPC A is adjacent and attacks");
        assertEquals(-1, forecasts.get(6).firstAttackTick, "trapped NPC B must never be predicted to attack");
        assertTrue(forecasts.get(6).stuck, "NPC B should be recognized as stuck");
    }

    @Test
    void blockingNpcIsExactlyWhatMakesTheTileUnreachable() {
        GridCollisionMap map = trapMap();

        // NPC B starts one tile further east at (12,10). Its diagonal step toward the player
        // targets (11,9) — NPC A's tile. With A present it must detour and end up trapped;
        // with A absent it steps to (11,9) and attacks from cardinal east.
        List<ColosseumPathing.Mover> withBlocker = List.of(
                new ColosseumPathing.Mover(5, 11, 9, 1, 1, true, 999),  // A never attacks (cooldown), just blocks
                new ColosseumPathing.Mover(6, 12, 10, 1, 1, true, 101));

        Map<Integer, ColosseumPathing.Forecast> blocked =
                ColosseumPathing.forwardSimulate(map, withBlocker, null, 10, 9, 100, 30, true);
        assertEquals(-1, blocked.get(6).firstAttackTick,
                "with NPC A on (11,9) the trap holds and B never attacks");

        List<ColosseumPathing.Mover> withoutBlocker = List.of(
                new ColosseumPathing.Mover(6, 12, 10, 1, 1, true, 101));

        Map<Integer, ColosseumPathing.Forecast> free =
                ColosseumPathing.forwardSimulate(map, withoutBlocker, null, 10, 9, 100, 30, true);
        assertTrue(free.get(6).firstAttackTick > 100,
                "without NPC A the same NPC reaches (11,9) and attacks");
    }

    @Test
    void trappedJaguarProducesNoPathPriorityEntry() {
        GridCollisionMap map = trapMap();
        PrayerEngine engine = new PrayerEngine();

        // Jaguar (2x2) wedged on the east face, SW tile diagonal to the player; berserker on
        // the corner tile. With jaguar path priority enabled there must still be no entry.
        PrayerDecision decision = null;
        for (int tick = 100; tick <= 110; tick++) {
            decision = engine.tick(input(tick, map, 10, 9)
                    .npc(berserker(5, 11, 9).build())
                    .npc(jaguar(6, 11, 10).build())
                    .build());
        }

        assertNull(decision.getPrayer(), "trapped jaguar must not trigger melee prayers");
        assertTrue(engine.getQueueView().isEmpty(), "no queue entries for unreachable NPCs");
    }
}
