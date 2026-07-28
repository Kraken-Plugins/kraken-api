package unit.plugins.colosseumv2;

import net.runelite.api.Prayer;
import org.junit.jupiter.api.Test;
import plugins.colosseumv2.engine.EngineConfig;
import plugins.colosseumv2.engine.GridCollisionMap;
import plugins.colosseumv2.engine.PrayerDecision;
import plugins.colosseumv2.engine.PrayerEngine;
import plugins.colosseumv2.engine.QueuedAttack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unit.plugins.colosseumv2.EngineTestSupport.ANIM_SHAMAN;
import static unit.plugins.colosseumv2.EngineTestSupport.input;
import static unit.plugins.colosseumv2.EngineTestSupport.openMap;
import static unit.plugins.colosseumv2.EngineTestSupport.shaman;

/**
 * Validates the core timing contract: an attack observed on tick T with a 5-tick attack speed
 * lands again on T+5, and the engine requests the prayer while exactly ONE tick remains
 * (during tick T+4) — never on the land tick itself, which would be too late because the
 * damage is already rolled when the animation starts.
 */
class PrayerEngineChainTimingTest {

    private static final int PLAYER_X = 20;
    private static final int PLAYER_Y = 20;

    @Test
    void prayerIsRequestedExactlyOneTickBeforeThePredictedAttack() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        // Tick 100: shaman attack animation observed.
        PrayerDecision atAttack = engine.tick(input(100, map, PLAYER_X, PLAYER_Y)
                .npc(shaman(1, 20, 26).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());
        assertNull(atAttack.getPrayer(), "nothing lands on tick 101, no prayer should be up yet");

        // The next attack is predicted for tick 105 and must appear in the queue immediately.
        assertEquals(1, engine.getQueueView().size());
        QueuedAttack queued = engine.getQueueView().get(0);
        assertEquals(105, queued.getLandTick());
        assertEquals(Prayer.PROTECT_FROM_MAGIC, queued.getPrayer());

        // Ticks 101-103: still too early — activating now would waste nothing but the contract
        // is that the prayer is needed for tick T+1 only.
        for (int tick = 101; tick <= 103; tick++) {
            PrayerDecision decision = engine.tick(input(tick, map, PLAYER_X, PLAYER_Y)
                    .npc(shaman(1, 20, 26).build())
                    .build());
            assertNull(decision.getPrayer(), "tick " + tick + " should not require a prayer");
        }

        // Tick 104: one tick remains before the 105 attack — the prayer must be requested NOW.
        PrayerDecision oneTickBefore = engine.tick(input(104, map, PLAYER_X, PLAYER_Y)
                .npc(shaman(1, 20, 26).build())
                .build());
        assertEquals(Prayer.PROTECT_FROM_MAGIC, oneTickBefore.getPrayer());
        assertEquals(PrayerDecision.Reason.QUEUED_ATTACK, oneTickBefore.getReason());
        assertEquals(105, oneTickBefore.getWinner().getLandTick());
    }

    @Test
    void observedFollowUpAttackKeepsTheChainRolling() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        engine.tick(input(100, map, PLAYER_X, PLAYER_Y)
                .npc(shaman(1, 20, 26).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());

        for (int tick = 101; tick <= 104; tick++) {
            engine.tick(input(tick, map, PLAYER_X, PLAYER_Y).npc(shaman(1, 20, 26).build()).build());
        }

        // Tick 105: the shaman attacks again as predicted; the chain extends to 110.
        engine.tick(input(105, map, PLAYER_X, PLAYER_Y)
                .npc(shaman(1, 20, 26).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());
        assertEquals(110, engine.getQueueView().get(0).getLandTick());

        for (int tick = 106; tick <= 108; tick++) {
            PrayerDecision decision = engine.tick(input(tick, map, PLAYER_X, PLAYER_Y)
                    .npc(shaman(1, 20, 26).build())
                    .build());
            assertNull(decision.getPrayer());
        }

        PrayerDecision beforeSecond = engine.tick(input(109, map, PLAYER_X, PLAYER_Y)
                .npc(shaman(1, 20, 26).build())
                .build());
        assertEquals(Prayer.PROTECT_FROM_MAGIC, beforeSecond.getPrayer());
    }

    @Test
    void unobservedLandingExtendsTheChainWhileLineOfSightHolds() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        engine.tick(input(100, map, PLAYER_X, PLAYER_Y)
                .npc(shaman(1, 20, 26).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());

        // The 105 attack animation is missed (no AnimationChanged), but line of sight held, so
        // the server necessarily attacked: the chain must extend to 110 on its own.
        for (int tick = 101; tick <= 105; tick++) {
            engine.tick(input(tick, map, PLAYER_X, PLAYER_Y).npc(shaman(1, 20, 26).build()).build());
        }
        assertEquals(110, engine.getQueueView().get(0).getLandTick());
    }

    @Test
    void losBreakCancelsQueuedAttacksWhenConfigured() {
        GridCollisionMap map = openMap();
        // Pillar occupying x 19-21, y 22-24. The shaman sits north of it at (20,26): a player
        // at (26,20) has a clear diagonal sight line, a player at (20,20) is fully blocked.
        map.block(19, 22, 3, 3);

        PrayerEngine engine = new PrayerEngine();
        engine.tick(input(100, map, 26, 20)
                .npc(shaman(1, 20, 26).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());

        // Player steps directly south of the pillar, breaking line of sight.
        PrayerDecision hidden = null;
        for (int tick = 101; tick <= 104; tick++) {
            hidden = engine.tick(input(tick, map, 20, 20).npc(shaman(1, 20, 26).build()).build());
        }
        assertNull(hidden.getPrayer(), "queued attack must be cancelled once line of sight broke");
        assertTrue(engine.getQueueView().isEmpty());

        // Same scenario with cancellation disabled keeps the queued entry alive.
        PrayerEngine keepingEngine = new PrayerEngine();
        EngineConfig keep = EngineConfig.builder().cancelQueuedOnLosBreak(false).build();
        keepingEngine.tick(input(100, map, 26, 20).config(keep)
                .npc(shaman(1, 20, 26).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());
        PrayerDecision kept = null;
        for (int tick = 101; tick <= 104; tick++) {
            kept = keepingEngine.tick(input(tick, map, 20, 20).config(keep).npc(shaman(1, 20, 26).build()).build());
        }
        assertEquals(Prayer.PROTECT_FROM_MAGIC, kept.getPrayer());
    }
}
