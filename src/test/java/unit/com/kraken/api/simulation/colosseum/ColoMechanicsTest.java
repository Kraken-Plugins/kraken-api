package unit.com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.colosseum.ColoConstants;
import com.kraken.api.simulation.colosseum.ColoCoords;
import com.kraken.api.simulation.colosseum.ColoFrame;
import com.kraken.api.simulation.colosseum.ColoGrid;
import com.kraken.api.simulation.colosseum.ColoNpcType;
import com.kraken.api.simulation.colosseum.ColoScratch;
import com.kraken.api.simulation.colosseum.ColoState;
import com.kraken.api.simulation.colosseum.ColoTick;
import com.kraken.api.simulation.colosseum.PlayerCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unit.com.kraken.api.simulation.colosseum.ColoTestArenas.up;

/**
 * Mechanics tests for engine behaviours beyond the community simulator's scope: prayer
 * timing, consumables, sky javelins, minotaur healing, warband route-finding and the
 * player's movement/combat model.
 */
class ColoMechanicsTest {

    private static void advanceTimes(ColoState state, ColoScratch scratch, int times) {
        for (int i = 0; i < times; i++) {
            ColoTick.advance(state, PlayerCommand.NONE, scratch);
        }
    }

    @Test
    void prayerAtLaunchBlocksShamanAttackButLatePrayerDoesNot() {
        ColoGrid grid = ColoTestArenas.openArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.SERPENT_SHAMAN);

        // Praying magic on the launch tick: the whole attack is nullified.
        ColoState prayed = ColoTestArenas.state(frame, ColoCoords.pack(10, 10), ColoCoords.pack(15, 10));
        ColoScratch scratch = new ColoScratch();
        ColoTick.advance(prayed, PlayerCommand.withOverhead(PlayerCommand.NONE, PlayerCommand.OVERHEAD_MAGIC), scratch);
        advanceTimes(prayed, scratch, 4);
        assertEquals(0, prayed.expectedDamageTaken(), "prayer active on the launch tick blocks the hit");

        // Praying one tick late: damage was locked in at launch and still lands.
        ColoState late = ColoTestArenas.state(frame, ColoCoords.pack(10, 10), ColoCoords.pack(15, 10));
        ColoScratch scratch2 = new ColoScratch();
        ColoTick.advance(late, PlayerCommand.NONE, scratch2);
        ColoTick.advance(late, PlayerCommand.withOverhead(PlayerCommand.NONE, PlayerCommand.OVERHEAD_MAGIC), scratch2);
        advanceTimes(late, scratch2, 3);
        assertTrue(late.expectedDamageTaken() > 0, "late prayer cannot stop an already-launched hit");
    }

    @Test
    void eatingHealsAndStacksDelays() {
        ColoGrid grid = ColoTestArenas.openArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.SERPENT_SHAMAN);
        ColoState state = ColoTestArenas.state(frame, ColoCoords.pack(5, 5), ColoCoords.pack(30, 30));
        state.setPlayer(ColoCoords.pack(5, 5), 40, 99, 10_000, 100, ColoState.OVERHEAD_NONE, 0);
        state.setSupplies(4, 2, 0, 0);
        ColoScratch scratch = new ColoScratch();

        // Food + karambwan combo on the same tick heals both amounts.
        long cmd = PlayerCommand.withConsumables(PlayerCommand.NONE, true, true, false, false);
        ColoTick.advance(state, cmd, scratch);
        assertEquals(40 + 22 + 18, state.playerHp());
        assertEquals(3, state.foodCount());
        assertEquals(1, state.comboCount());
        // 3 (food) + 2 (karambwan) attack delay, minus this tick's decrement.
        assertEquals(4, state.attackDelay());

        // Immediately eating again is blocked by the food delay.
        int hpBefore = state.playerHp();
        ColoTick.advance(state, PlayerCommand.withConsumables(PlayerCommand.NONE, true, false, false, false), scratch);
        assertEquals(hpBefore, state.playerHp(), "food delay blocks a second food this tick");
        assertEquals(3, state.foodCount());
    }

    @Test
    void skyJavelinIsDodgedByMovingOffTheTargetTile() {
        ColoGrid grid = ColoTestArenas.openArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.JAVELIN_COLOSSUS);

        // Standing still: the sky javelin lands on the targeted tile for expected damage.
        ColoState still = ColoTestArenas.state(frame, ColoCoords.pack(10, 10), ColoCoords.pack(16, 10));
        still.setJavelinAutos(0, 4);
        ColoScratch scratch = new ColoScratch();
        ColoTestArenas.AttackRecorder recorder = new ColoTestArenas.AttackRecorder();
        scratch.attackListener = recorder;
        advanceTimes(still, scratch, 1 + ColoConstants.JAVELIN_SKY_LAND_DELAY_TICKS);
        assertEquals(1, recorder.events.get(0)[3], "fifth attack is the special");
        assertTrue(still.expectedDamageTaken() >= 20, "sky javelin hits when standing on the tile");

        // Moving one tile off before it lands: no damage from the special.
        ColoState mover = ColoTestArenas.state(frame, ColoCoords.pack(10, 10), ColoCoords.pack(16, 10));
        mover.setJavelinAutos(0, 4);
        ColoScratch scratch2 = new ColoScratch();
        ColoTick.advance(mover, PlayerCommand.NONE, scratch2);
        ColoTick.advance(mover, PlayerCommand.withOverhead(
                PlayerCommand.moveTo(ColoCoords.pack(10, 12), false), PlayerCommand.OVERHEAD_MISSILES), scratch2);
        advanceTimes(mover, scratch2, ColoConstants.JAVELIN_SKY_LAND_DELAY_TICKS);
        // Regular javelin attacks are blocked by the missiles prayer; the dodged special
        // contributes nothing, so total expected damage stays zero.
        assertEquals(0, mover.expectedDamageTaken(), "moving off the targeted tile dodges the special");
    }

    @Test
    void minotaurHealsDamagedNpcWithinRangeAndLineOfSight() {
        ColoGrid grid = ColoTestArenas.openArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.MINOTAUR, ColoNpcType.JAVELIN_COLOSSUS);
        ColoState state = ColoTestArenas.state(frame, ColoCoords.pack(2, 2), ColoCoords.pack(20, 20), ColoCoords.pack(24, 20));
        state.activateNpc(1, ColoCoords.pack(24, 20), 100, 10);
        ColoScratch scratch = new ColoScratch();

        ColoTick.advance(state, PlayerCommand.NONE, scratch);
        assertEquals(100 + ColoConstants.MINOTAUR_HEAL_PER_CYCLE, state.npcHp(1),
                "minotaur heals the damaged colossus instead of chasing a distant player");
    }

    @Test
    void minotaurDoesNotHealHealthyOrOutOfRangeNpcs() {
        ColoGrid grid = ColoTestArenas.openArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.MINOTAUR, ColoNpcType.JAVELIN_COLOSSUS);
        // Healthy target: above the 75% threshold, no heal.
        ColoState healthy = ColoTestArenas.state(frame, ColoCoords.pack(2, 2), ColoCoords.pack(20, 20), ColoCoords.pack(24, 20));
        healthy.activateNpc(1, ColoCoords.pack(24, 20), 200, 10);
        ColoScratch scratch = new ColoScratch();
        ColoTick.advance(healthy, PlayerCommand.NONE, scratch);
        assertEquals(200, healthy.npcHp(1));

        // Damaged but 12 tiles away: outside the 7-tile heal range, no heal.
        ColoState far = ColoTestArenas.state(frame, ColoCoords.pack(2, 2), ColoCoords.pack(20, 20), ColoCoords.pack(33, 20));
        far.activateNpc(1, ColoCoords.pack(33, 20), 100, 10);
        ColoScratch scratch2 = new ColoScratch();
        ColoTick.advance(far, PlayerCommand.NONE, scratch2);
        assertEquals(100, far.npcHp(1));
    }

    @Test
    void warbandRouteFindsAroundPillarWhileDumbNpcHugsIt() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.FREMENNIK_BERSERKER, ColoNpcType.JAGUAR_WARRIOR);
        // Pillar occupies colosim (8..10, 8..10); stand the player west of it, both NPCs east.
        ColoState state = ColoTestArenas.state(frame, up(6, 9), up(12, 9), up(14, 9));
        ColoScratch scratch = new ColoScratch();
        ColoTestArenas.AttackRecorder recorder = new ColoTestArenas.AttackRecorder();
        scratch.attackListener = recorder;

        for (int i = 0; i < 14; i++) {
            ColoTick.advance(state, PlayerCommand.NONE, scratch);
        }
        assertFalse(recorder.attackTicks(0).isEmpty(), "berserker routes around the pillar and reaches melee");
        for (int tick : recorder.attackTicks(0)) {
            assertEquals(ColoConstants.DEFAULT_WARBAND_CYCLE_PHASE, tick % ColoConstants.WARBAND_CYCLE_TICKS,
                    "warband attacks ride the fixed 6-tick cycle");
        }
        // The jaguar's naive chase hugs the pillar and never gains melee reach.
        assertTrue(recorder.attackTicks(1).isEmpty(), "dumb-pathing jaguar stays stuck behind the pillar");
    }

    @Test
    void playerRunsTwoTilesPerTickAndDrainsEnergy() {
        ColoGrid grid = ColoTestArenas.openArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.SERPENT_SHAMAN);
        ColoState state = ColoTestArenas.state(frame, ColoCoords.pack(5, 5), ColoCoords.pack(30, 30));
        ColoScratch scratch = new ColoScratch();

        ColoTick.advance(state, PlayerCommand.moveTo(ColoCoords.pack(5, 11), true), scratch);
        assertEquals(ColoCoords.pack(5, 7), state.playerPos(), "run covers two tiles per tick");
        assertTrue(state.runEnergyUnits() < 10_000, "running drains energy");

        advanceTimes(state, scratch, 2);
        assertEquals(ColoCoords.pack(5, 11), state.playerPos(), "arrives after three run ticks");
        assertEquals(ColoCoords.NONE, state.moveTarget(), "movement target clears on arrival");
    }

    @Test
    void playerKillsNpcAndDamageLandsWithDelay() {
        ColoGrid grid = ColoTestArenas.openArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.SERPENT_SHAMAN);
        ColoState state = ColoTestArenas.state(frame, ColoCoords.pack(10, 10), ColoCoords.pack(14, 10));
        state.activateNpc(0, ColoCoords.pack(14, 10), 15, 30);
        // Magic gear set (index 2) has range 10 and 20 expected damage vs the shaman.
        state.setPlayer(ColoCoords.pack(10, 10), 99, 99, 10_000, 100, ColoState.OVERHEAD_NONE, 2);
        ColoScratch scratch = new ColoScratch();

        ColoTick.advance(state, PlayerCommand.withAttack(PlayerCommand.NONE, 0), scratch);
        assertTrue(state.npcActive(0), "hit is still in flight");

        advanceTimes(state, scratch, 4);
        assertFalse(state.npcActive(0), "shaman dies when the delayed hit lands");
        assertEquals(1, state.kills());
        assertTrue(state.damageDealt() > 0);
    }

    @Test
    void manticoreOrbPrayersMustFollowThePattern() {
        ColoGrid grid = ColoTestArenas.openArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.MANTICORE);
        ColoState state = ColoTestArenas.state(frame, ColoCoords.pack(10, 10), ColoCoords.pack(14, 10));
        state.setManticoreState(0, ColoTick.PATTERN_RANGED_FIRST, true, 0);
        ColoScratch scratch = new ColoScratch();

        // Flick ranged -> magic -> melee across the three orb ticks: nothing lands.
        ColoTick.advance(state, PlayerCommand.withOverhead(PlayerCommand.NONE, PlayerCommand.OVERHEAD_MISSILES), scratch);
        ColoTick.advance(state, PlayerCommand.withOverhead(PlayerCommand.NONE, PlayerCommand.OVERHEAD_MAGIC), scratch);
        ColoTick.advance(state, PlayerCommand.withOverhead(PlayerCommand.NONE, PlayerCommand.OVERHEAD_MELEE), scratch);
        assertEquals(0, state.expectedDamageTaken(), "correct per-orb flicks block the whole triple");

        // Holding one prayer through the triple eats the other two orbs.
        ColoState held = ColoTestArenas.state(frame, ColoCoords.pack(10, 10), ColoCoords.pack(14, 10));
        held.setManticoreState(0, ColoTick.PATTERN_RANGED_FIRST, true, 0);
        ColoScratch scratch2 = new ColoScratch();
        ColoTick.advance(held, PlayerCommand.withOverhead(PlayerCommand.NONE, PlayerCommand.OVERHEAD_MISSILES), scratch2);
        ColoTick.advance(held, PlayerCommand.NONE, scratch2);
        ColoTick.advance(held, PlayerCommand.NONE, scratch2);
        assertTrue(held.expectedDamageTaken() > 0, "holding missiles eats the magic and melee orbs");
    }
}
