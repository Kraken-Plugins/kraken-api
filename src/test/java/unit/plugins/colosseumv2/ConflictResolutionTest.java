package unit.plugins.colosseumv2;

import net.runelite.api.Prayer;
import org.junit.jupiter.api.Test;
import plugins.colosseumv2.engine.EngineConfig;
import plugins.colosseumv2.engine.GridCollisionMap;
import plugins.colosseumv2.engine.PrayerDecision;
import plugins.colosseumv2.engine.PrayerEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unit.plugins.colosseumv2.EngineTestSupport.ANIM_JAVELIN;
import static unit.plugins.colosseumv2.EngineTestSupport.ANIM_SHAMAN;
import static unit.plugins.colosseumv2.EngineTestSupport.input;
import static unit.plugins.colosseumv2.EngineTestSupport.javelin;
import static unit.plugins.colosseumv2.EngineTestSupport.jaguar;
import static unit.plugins.colosseumv2.EngineTestSupport.openMap;
import static unit.plugins.colosseumv2.EngineTestSupport.shaman;

/**
 * Same-tick conflicts: the mob with the greatest max hit wins, the jaguar path-priority
 * option overrides when enabled, and a likely prayer-ignoring javelin artillery attack loses
 * to anything it competes with.
 */
class ConflictResolutionTest {

    private static final int PX = 20;
    private static final int PY = 20;

    @Test
    void higherMaxHitWinsSameTickConflicts() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        // Javelin colossus (max 54, ranged) and serpent shaman (max 28, magic) both attack on
        // tick 96 -> both land again on 101.
        engine.tick(input(96, map, PX, PY)
                .npc(javelin(1, 20, 26).animation(ANIM_JAVELIN).animationChanged(true).build())
                .npc(shaman(2, 26, 20).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());

        PrayerDecision decision = null;
        for (int tick = 97; tick <= 100; tick++) {
            decision = engine.tick(input(tick, map, PX, PY)
                    .npc(javelin(1, 20, 26).build())
                    .npc(shaman(2, 26, 20).build())
                    .build());
        }

        assertEquals(Prayer.PROTECT_FROM_MISSILES, decision.getPrayer(), "javelin hits harder than shaman");
        assertEquals(2, decision.getContested());
    }

    @Test
    void jaguarPathPriorityOverridesHigherMaxHits() {
        GridCollisionMap map = openMap();

        // Javelin chain lands on 101; the adjacent jaguar's pathing also predicts a hit on
        // 101 (arrives + ready). With priority enabled melee wins despite 54 > 47.
        PrayerEngine engine = new PrayerEngine();
        engine.tick(input(96, map, PX, PY)
                .npc(javelin(1, 20, 26).animation(ANIM_JAVELIN).animationChanged(true).build())
                .npc(jaguar(2, 20, 21).build())
                .build());

        PrayerDecision decision = null;
        for (int tick = 97; tick <= 100; tick++) {
            decision = engine.tick(input(tick, map, PX, PY)
                    .npc(javelin(1, 20, 26).build())
                    .npc(jaguar(2, 20, 21).build())
                    .build());
        }
        assertEquals(Prayer.PROTECT_FROM_MELEE, decision.getPrayer(), "jaguar priority must win");

        // Same scenario with the option disabled: the jaguar (which has never attacked) makes
        // no entry at all, so the javelin's ranged prayer wins.
        PrayerEngine noPriority = new PrayerEngine();
        EngineConfig config = EngineConfig.builder().prayJaguarOnPath(false).build();
        noPriority.tick(input(96, map, PX, PY).config(config)
                .npc(javelin(1, 20, 26).animation(ANIM_JAVELIN).animationChanged(true).build())
                .npc(jaguar(2, 20, 21).build())
                .build());

        PrayerDecision withoutPriority = null;
        for (int tick = 97; tick <= 100; tick++) {
            withoutPriority = noPriority.tick(input(tick, map, PX, PY).config(config)
                    .npc(javelin(1, 20, 26).build())
                    .npc(jaguar(2, 20, 21).build())
                    .build());
        }
        assertEquals(Prayer.PROTECT_FROM_MISSILES, withoutPriority.getPrayer());
    }

    @Test
    void likelyArtilleryAttackLosesConflictsButIsStillPrayedAlone() {
        GridCollisionMap map = openMap();
        PrayerEngine engine = new PrayerEngine();

        // Four observed javelin attacks (76, 81, 86, 91): the fifth attack in the cycle (96)
        // is the prayer-ignoring artillery. The shaman also lands on 96.
        for (int tick = 76; tick <= 91; tick++) {
            boolean javelinAttack = tick == 76 || tick == 81 || tick == 86 || tick == 91;
            boolean shamanAttack = tick == 91;
            engine.tick(input(tick, map, PX, PY)
                    .npc(javelin(1, 20, 26).animation(javelinAttack ? ANIM_JAVELIN : -1).animationChanged(javelinAttack).build())
                    .npc(shaman(2, 26, 20).animation(shamanAttack ? ANIM_SHAMAN : -1).animationChanged(shamanAttack).build())
                    .build());
        }

        PrayerDecision decision = null;
        for (int tick = 92; tick <= 95; tick++) {
            decision = engine.tick(input(tick, map, PX, PY)
                    .npc(javelin(1, 20, 26).build())
                    .npc(shaman(2, 26, 20).build())
                    .build());
        }

        assertEquals(Prayer.PROTECT_FROM_MAGIC, decision.getPrayer(),
                "shaman (28) must beat a likely-artillery javelin (54) since artillery ignores prayer");
        assertTrue(decision.getWinner().getNpcIndex() == 2);

        // Alone, the likely-artillery attack is still prayed against (better than nothing —
        // the cycle count is best-effort).
        PrayerEngine aloneEngine = new PrayerEngine();
        for (int tick = 76; tick <= 91; tick++) {
            boolean javelinAttack = tick == 76 || tick == 81 || tick == 86 || tick == 91;
            aloneEngine.tick(input(tick, map, PX, PY)
                    .npc(javelin(1, 20, 26).animation(javelinAttack ? ANIM_JAVELIN : -1).animationChanged(javelinAttack).build())
                    .build());
        }
        PrayerDecision alone = null;
        for (int tick = 92; tick <= 95; tick++) {
            alone = aloneEngine.tick(input(tick, map, PX, PY).npc(javelin(1, 20, 26).build()).build());
        }
        assertEquals(Prayer.PROTECT_FROM_MISSILES, alone.getPrayer());
    }
}
