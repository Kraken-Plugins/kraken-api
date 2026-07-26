package unit.plugins.colosseumv2;

import net.runelite.api.Prayer;
import org.junit.jupiter.api.Test;
import plugins.colosseumv2.engine.GridCollisionMap;
import plugins.colosseumv2.engine.PrayerDecision;
import plugins.colosseumv2.engine.PrayerEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unit.plugins.colosseumv2.EngineTestSupport.ANIM_MANTICORE_THROW;
import static unit.plugins.colosseumv2.EngineTestSupport.input;
import static unit.plugins.colosseumv2.EngineTestSupport.manticore;
import static unit.plugins.colosseumv2.EngineTestSupport.openMap;

/**
 * Manticore ownership handshake: the plugin prays the first volley only when it witnessed the
 * orb graphic appear (uncharged -> charging) while the player was in line of sight, and any
 * line of sight break before the volley resets responsibility to the player. Charging takes
 * 10 ticks, then the three orbs land on consecutive ticks (first style, second style, melee).
 */
class ManticoreWitnessTest {

    private static final int PX = 20;
    private static final int PY = 20;
    private static final int MX = 20;
    private static final int MY = 26;

    @Test
    void witnessedChargeSchedulesTheFullFirstVolley() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        // Tick 100: manticore tracked, uncharged.
        engine.tick(input(100, map, PX, PY).npc(manticore(1, MX, MY).build()).build());

        // Tick 101: the range orb appears while the player is in line of sight — witnessed.
        engine.tick(input(101, map, PX, PY).npc(manticore(1, MX, MY).hasRangeOrb(true).build()).build());

        // Charge takes 10 ticks: orbs land on 111 (range), 112 (mage), 113 (melee).
        assertEquals(3, engine.getQueueView().size());

        Prayer[] expected = {Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MELEE};
        for (int tick = 102; tick <= 112; tick++) {
            PrayerDecision decision = engine.tick(input(tick, map, PX, PY)
                    .npc(manticore(1, MX, MY).hasRangeOrb(true).hasMageOrb(true).hasMeleeOrb(true).build())
                    .build());

            if (tick < 110) {
                assertNull(decision.getPrayer(), "no prayer expected on tick " + tick);
            } else {
                assertEquals(expected[tick - 110], decision.getPrayer(), "wrong prayer on tick " + tick);
            }
        }
    }

    @Test
    void mageFirstChargeOrdersOrbsMageRangeMelee() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        engine.tick(input(100, map, PX, PY).npc(manticore(1, MX, MY).build()).build());
        engine.tick(input(101, map, PX, PY).npc(manticore(1, MX, MY).hasMageOrb(true).build()).build());

        Prayer[] expected = {Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MELEE};
        for (int tick = 102; tick <= 112; tick++) {
            PrayerDecision decision = engine.tick(input(tick, map, PX, PY)
                    .npc(manticore(1, MX, MY).hasMageOrb(true).hasRangeOrb(true).hasMeleeOrb(true).build())
                    .build());
            if (tick >= 110) {
                assertEquals(expected[tick - 110], decision.getPrayer(), "wrong prayer on tick " + tick);
            }
        }
    }

    @Test
    void losBreakDuringChargeResetsOwnershipUntilAVolleyIsObserved() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();
        // Pillar the player can hide behind: x 16-18, y 22-24.
        map.block(16, 22, 3, 3);

        engine.tick(input(100, map, PX, PY).npc(manticore(1, MX, MY).build()).build());
        engine.tick(input(101, map, PX, PY).npc(manticore(1, MX, MY).hasRangeOrb(true).build()).build());
        assertEquals(3, engine.getQueueView().size(), "witnessed charge should queue the volley");

        // Tick 103: player hides behind the pillar mid-charge -> ownership resets.
        engine.tick(input(103, map, 17, 20).npc(manticore(1, MX, MY).hasRangeOrb(true).build()).build());
        assertTrue(engine.getQueueView().isEmpty(), "line of sight break must clear the witnessed volley");

        // Player returns; the plugin must NOT resume responsibility for the first volley.
        for (int tick = 104; tick <= 114; tick++) {
            PrayerDecision decision = engine.tick(input(tick, map, PX, PY)
                    .npc(manticore(1, MX, MY).hasRangeOrb(true).hasMageOrb(true).hasMeleeOrb(true).build())
                    .build());
            assertNull(decision.getPrayer(), "player owns the whole first volley after a LoS break (tick " + tick + ")");
        }

        // Tick 115: the manticore fires (player prayed it manually); the plugin picks up the
        // NEXT volley, 10 ticks later: 125 range, 126 mage, 127 melee.
        engine.tick(input(115, map, PX, PY)
                .npc(manticore(1, MX, MY)
                        .hasRangeOrb(true).hasMageOrb(true).hasMeleeOrb(true)
                        .animation(ANIM_MANTICORE_THROW).animationChanged(true)
                        .build())
                .build());

        // No partial-volley handling: orbs 2 and 3 of the observed volley are the player's.
        for (int tick = 116; tick <= 123; tick++) {
            PrayerDecision decision = engine.tick(input(tick, map, PX, PY)
                    .npc(manticore(1, MX, MY).hasRangeOrb(true).hasMageOrb(true).hasMeleeOrb(true).build())
                    .build());
            assertNull(decision.getPrayer(), "no partial volley prayers (tick " + tick + ")");
        }

        Prayer[] expected = {Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MELEE};
        for (int tick = 124; tick <= 126; tick++) {
            PrayerDecision decision = engine.tick(input(tick, map, PX, PY)
                    .npc(manticore(1, MX, MY).hasRangeOrb(true).hasMageOrb(true).hasMeleeOrb(true).build())
                    .build());
            assertEquals(expected[tick - 124], decision.getPrayer(), "wrong prayer on tick " + tick);
        }
    }

    @Test
    void alreadyChargedManticoreIsNeverOwnedByThePlugin() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        // First snapshot already shows orbs (plugin enabled mid-charge): no clean transition
        // was witnessed, so the player owns the first volley entirely.
        for (int tick = 100; tick <= 115; tick++) {
            PrayerDecision decision = engine.tick(input(tick, map, PX, PY)
                    .npc(manticore(1, MX, MY).hasRangeOrb(true).hasMageOrb(true).hasMeleeOrb(true).build())
                    .build());
            assertNull(decision.getPrayer(), "unwitnessed charge must never be plugin-owned (tick " + tick + ")");
        }
    }

    @Test
    void secondManticoreIsStaggeredWhenTheFirstFires() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        // Both manticores tracked uncharged, then both charge on tick 81 (witnessed).
        engine.tick(input(80, map, PX, PY)
                .npc(manticore(1, 20, 26).build())
                .npc(manticore(2, 26, 20).build())
                .build());
        engine.tick(input(81, map, PX, PY)
                .npc(manticore(1, 20, 26).hasRangeOrb(true).build())
                .npc(manticore(2, 26, 20).hasRangeOrb(true).build())
                .build());
        assertEquals(6, engine.getQueueView().size(), "both witnessed volleys queued");

        for (int tick = 82; tick <= 90; tick++) {
            engine.tick(input(tick, map, PX, PY)
                    .npc(manticore(1, 20, 26).hasRangeOrb(true).build())
                    .npc(manticore(2, 26, 20).hasRangeOrb(true).build())
                    .build());
        }

        // Tick 91: manticore #1 fires. Only one manticore can fire per tick; #2 was ready on
        // the same tick, so it is delayed by 5 ticks -> its volley starts on 96.
        engine.tick(input(91, map, PX, PY)
                .npc(manticore(1, 20, 26).hasRangeOrb(true)
                        .animation(ANIM_MANTICORE_THROW).animationChanged(true).build())
                .npc(manticore(2, 26, 20).hasRangeOrb(true).build())
                .build());

        boolean foundStaggered = engine.getQueueView().stream()
                .anyMatch(attack -> attack.getNpcIndex() == 2 && attack.getLandTick() == 96);
        assertTrue(foundStaggered, "second manticore's volley must shift to tick 96");

        // Decision on tick 95 = one tick before manticore #2's first orb.
        for (int tick = 92; tick <= 95; tick++) {
            PrayerDecision decision = engine.tick(input(tick, map, PX, PY)
                    .npc(manticore(1, 20, 26).hasRangeOrb(true).build())
                    .npc(manticore(2, 26, 20).hasRangeOrb(true).build())
                    .build());
            if (tick == 92) {
                // Orb 3 (melee) of manticore #1's volley lands on 93.
                assertEquals(Prayer.PROTECT_FROM_MELEE, decision.getPrayer());
            }
            if (tick == 95) {
                assertEquals(Prayer.PROTECT_FROM_MISSILES, decision.getPrayer());
            }
        }
    }
}
