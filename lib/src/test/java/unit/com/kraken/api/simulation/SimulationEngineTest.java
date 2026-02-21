package com.kraken.api.simulation;

import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {
    private static final int BASE_X = 3200;
    private static final int BASE_Y = 3200;
    private static final int PLANE = 0;

    private final SimulationEngine engine = new SimulationEngine();

    @Test
    void npcMovesDiagonallyTowardPlayerWhenOpen() {
        SimulationNpcSnapshot npc = new SimulationNpcSnapshot(
                7, 1000, "Test NPC", new WorldPoint(BASE_X + 1, BASE_Y + 1, PLANE), 1, 1, true, false
        );
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 4, BASE_Y + 4, PLANE),
                Collections.singletonList(npc),
                flags -> {}
        );

        SimulationState state = snapshot.createState();
        engine.simulateTick(state, SimulationAction.WAIT);

        assertEquals(new WorldPoint(BASE_X + 2, BASE_Y + 2, PLANE), state.getNpcWorldPoint(0));
    }

    @Test
    void npcFallsBackToCardinalWhenDiagonalBlocked() {
        SimulationNpcSnapshot npc = new SimulationNpcSnapshot(
                7, 1000, "Test NPC", new WorldPoint(BASE_X + 1, BASE_Y + 1, PLANE), 1, 1, true, false
        );
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 4, BASE_Y + 4, PLANE),
                Collections.singletonList(npc),
                flags -> flags[2][2] = CollisionDataFlag.BLOCK_MOVEMENT_FULL
        );

        SimulationState state = snapshot.createState();
        engine.simulateTick(state, SimulationAction.WAIT);

        assertEquals(new WorldPoint(BASE_X + 2, BASE_Y + 1, PLANE), state.getNpcWorldPoint(0));
    }

    @Test
    void lineOfSightUsesCopiedCollisionFlags() {
        SimulationNpcSnapshot npc = new SimulationNpcSnapshot(
                7, 1000, "Ranger", new WorldPoint(BASE_X + 1, BASE_Y + 1, PLANE), 1, 8, true, true
        );

        SimulationSnapshot openSnapshot = snapshot(
                new WorldPoint(BASE_X + 4, BASE_Y + 1, PLANE),
                Collections.singletonList(npc),
                flags -> {}
        );
        SimulationState openState = openSnapshot.createState();
        assertTrue(engine.hasNpcLineOfSightToPlayer(openState, 0));

        SimulationSnapshot blockedSnapshot = snapshot(
                new WorldPoint(BASE_X + 4, BASE_Y + 1, PLANE),
                Collections.singletonList(npc),
                flags -> flags[2][1] = CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL
        );
        SimulationState blockedState = blockedSnapshot.createState();
        assertFalse(engine.hasNpcLineOfSightToPlayer(blockedState, 0));
    }

    @Test
    void stateCopyIsIndependentForTreeSearch() {
        SimulationNpcSnapshot npc = new SimulationNpcSnapshot(
                7, 1000, "Test NPC", new WorldPoint(BASE_X + 5, BASE_Y + 5, PLANE), 1, 1, true, false
        );
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 3, BASE_Y + 3, PLANE),
                Collections.singletonList(npc),
                flags -> {}
        );

        SimulationState root = snapshot.createState();
        SimulationState child = root.copy();
        engine.simulateTick(child, SimulationAction.NORTH);

        assertNotEquals(root.getPlayerWorldPoint(), child.getPlayerWorldPoint());
        assertEquals(new WorldPoint(BASE_X + 3, BASE_Y + 3, PLANE), root.getPlayerWorldPoint());
    }

    @Test
    void decisionTreeFindsActionableBestMove() {
        SimulationNpcSnapshot npc = new SimulationNpcSnapshot(
                42, 2000, "Melee NPC", new WorldPoint(BASE_X + 3, BASE_Y + 5, PLANE), 1, 1, true, false
        );
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 3, BASE_Y + 3, PLANE),
                Collections.singletonList(npc),
                flags -> {}
        );
        SimulationState state = snapshot.createState();

        DecisionTreeSearch search = new DecisionTreeSearch(engine, 1024);
        DecisionTreeSearch.Result result = search.search(
                state,
                1,
                (s, depth) -> Arrays.asList(SimulationAction.WAIT, SimulationAction.NORTH, SimulationAction.SOUTH),
                s -> Math.abs(s.getNpcX(0) - s.getPlayerX()) + Math.abs(s.getNpcY(0) - s.getPlayerY())
        );

        assertNotNull(result);
        assertEquals(SimulationAction.SOUTH, result.getBestAction());
        assertEquals(new WorldPoint(BASE_X + 3, BASE_Y + 2, PLANE), result.getBestPlayerWorldPoint());
        assertTrue(result.getExploredNodes() > 0);
    }

    @Test
    void prayerSwitchPreventsIncomingDamage() {
        SimulationNpcSnapshot npc = new SimulationNpcSnapshot(
                7,
                3000,
                "Mage",
                new WorldPoint(BASE_X + 1, BASE_Y + 1, PLANE),
                1,
                10,
                4,
                NpcAttackStyle.MAGIC,
                12,
                true,
                true
        );

        SimulationPlayerSnapshot player = new SimulationPlayerSnapshot(
                50,
                99,
                null,
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap()
        );

        SimulationSnapshot noPrayerSnapshot = snapshot(
                new WorldPoint(BASE_X + 4, BASE_Y + 1, PLANE),
                player,
                Collections.singletonList(npc),
                flags -> {}
        );
        SimulationState noPrayerState = noPrayerSnapshot.createState();
        engine.simulateTick(noPrayerState, SimulationAction.WAIT);
        assertEquals(38, noPrayerState.getPlayerHitpoints());

        SimulationPlayerSnapshot protectedPlayer = new SimulationPlayerSnapshot(
                50,
                99,
                Prayer.PROTECT_FROM_MAGIC,
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap()
        );
        SimulationSnapshot withPrayerSnapshot = snapshot(
                new WorldPoint(BASE_X + 4, BASE_Y + 1, PLANE),
                protectedPlayer,
                Collections.singletonList(npc),
                flags -> {}
        );
        SimulationState withPrayerState = withPrayerSnapshot.createState();
        engine.simulateTick(withPrayerState, SimulationAction.WAIT);
        assertEquals(50, withPrayerState.getPlayerHitpoints());
    }

    @Test
    void eatActionConsumesFoodAndHealsPlayer() {
        SimulationPlayerSnapshot player = new SimulationPlayerSnapshot(
                30,
                99,
                null,
                Map.of(385, 2),
                Collections.emptySet(),
                Map.of(385, 20)
        );
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 3, BASE_Y + 3, PLANE),
                player,
                Collections.emptyList(),
                flags -> {}
        );

        SimulationState state = snapshot.createState();
        engine.simulateTick(state, SimulationAction.eat(385, 20));

        assertEquals(50, state.getPlayerHitpoints());
        assertEquals(1, state.getInventoryItemCount(385));
    }

    @Test
    void recommendProtectionPrayerUsesNpcCombatStyleAndSpeed() {
        SimulationNpcSnapshot ranger = new SimulationNpcSnapshot(
                1,
                1001,
                "Ranger",
                new WorldPoint(BASE_X + 2, BASE_Y + 2, PLANE),
                1,
                8,
                5,
                NpcAttackStyle.RANGED,
                12,
                true,
                true
        );
        SimulationNpcSnapshot mager = new SimulationNpcSnapshot(
                2,
                1002,
                "Mager",
                new WorldPoint(BASE_X + 2, BASE_Y + 4, PLANE),
                1,
                8,
                4,
                NpcAttackStyle.MAGIC,
                10,
                true,
                true
        );

        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 4, BASE_Y + 3, PLANE),
                Arrays.asList(ranger, mager),
                flags -> {}
        );
        SimulationState state = snapshot.createState();
        state.setNpcAttackCooldown(0, 3);
        state.setNpcAttackCooldown(1, 0);

        Prayer recommended = engine.recommendProtectionPrayer(state);
        assertEquals(Prayer.PROTECT_FROM_MAGIC, recommended);
    }

    private SimulationSnapshot snapshot(
            WorldPoint playerPoint,
            List<SimulationNpcSnapshot> npcs,
            Consumer<int[][]> flagsMutator
    ) {
        return snapshot(playerPoint, SimulationPlayerSnapshot.empty(), npcs, flagsMutator);
    }

    private SimulationSnapshot snapshot(
            WorldPoint playerPoint,
            SimulationPlayerSnapshot player,
            List<SimulationNpcSnapshot> npcs,
            Consumer<int[][]> flagsMutator
    ) {
        int[][] flags = new int[8][8];
        if (flagsMutator != null) {
            flagsMutator.accept(flags);
        }
        return new SimulationSnapshot(0, PLANE, BASE_X, BASE_Y, flags, playerPoint, player, npcs);
    }
}
