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
 * Wave pre-pray window behavior and queued-attack removal on NPC death/despawn.
 */
class PrePrayAndDeathHandlingTest {

    private static final int PX = 20;
    private static final int PY = 20;

    @Test
    void prePrayHoldsFromWaveStartUntilFirstObservedAttack() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        // Wave started on tick 100; nothing has attacked yet.
        PrayerDecision decision = engine.tick(input(100, map, PX, PY)
                .waveStartTick(100)
                .prePrayPrayer(Prayer.PROTECT_FROM_MISSILES)
                .build());
        assertEquals(Prayer.PROTECT_FROM_MISSILES, decision.getPrayer());
        assertEquals(PrayerDecision.Reason.PRE_PRAY, decision.getReason());

        // Mobs appear but have not attacked: pre-pray continues.
        decision = engine.tick(input(101, map, PX, PY)
                .waveStartTick(100)
                .prePrayPrayer(Prayer.PROTECT_FROM_MISSILES)
                .npc(shaman(1, 20, 26).build())
                .build());
        assertEquals(PrayerDecision.Reason.PRE_PRAY, decision.getReason());

        // Tick 102: shaman attacks — the pre-pray window closes. The queued follow-up (107)
        // takes over normally at 106.
        decision = engine.tick(input(102, map, PX, PY)
                .waveStartTick(100)
                .prePrayPrayer(Prayer.PROTECT_FROM_MISSILES)
                .npc(shaman(1, 20, 26).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());
        assertNull(decision.getPrayer(), "pre-pray must end once a real attack was observed");

        for (int tick = 103; tick <= 106; tick++) {
            decision = engine.tick(input(tick, map, PX, PY)
                    .waveStartTick(100)
                    .prePrayPrayer(Prayer.PROTECT_FROM_MISSILES)
                    .npc(shaman(1, 20, 26).build())
                    .build());
        }
        assertEquals(Prayer.PROTECT_FROM_MAGIC, decision.getPrayer(), "queued shaman attack takes over");
    }

    @Test
    void prePrayWindowExpires() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        PrayerDecision inWindow = engine.tick(input(111, map, PX, PY)
                .waveStartTick(100)
                .prePrayPrayer(Prayer.PROTECT_FROM_MAGIC)
                .build());
        assertEquals(PrayerDecision.Reason.PRE_PRAY, inWindow.getReason());

        PrayerDecision expired = engine.tick(input(113, map, PX, PY)
                .waveStartTick(100)
                .prePrayPrayer(Prayer.PROTECT_FROM_MAGIC)
                .build());
        assertNull(expired.getPrayer(), "window (12 ticks) has passed");
    }

    @Test
    void intermissionPrePraysOnlyWhileArenaIsClear() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        PrayerDecision clear = engine.tick(input(200, map, PX, PY)
                .waveStarted(false)
                .waveStartTick(-1)
                .prePrayPrayer(Prayer.PROTECT_FROM_MAGIC)
                .build());
        assertEquals(PrayerDecision.Reason.PRE_PRAY, clear.getReason());

        PrayerDecision occupied = engine.tick(input(201, map, PX, PY)
                .waveStarted(false)
                .waveStartTick(-1)
                .prePrayPrayer(Prayer.PROTECT_FROM_MAGIC)
                .npc(shaman(1, 20, 26).build())
                .build());
        assertNull(occupied.getPrayer(), "live threats mean wave state is stale, no blind pre-pray");
    }

    @Test
    void deathRemovesQueuedAttacksByDefault() {
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        engine.tick(input(100, map, PX, PY)
                .npc(shaman(1, 20, 26).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());
        assertEquals(1, engine.getQueueView().size());

        engine.tick(input(101, map, PX, PY).npc(shaman(1, 20, 26).build()).build());
        engine.tick(input(102, map, PX, PY).npc(shaman(1, 20, 26).dead(true).build()).build());
        assertTrue(engine.getQueueView().isEmpty(), "death must clear the queued attack");

        PrayerDecision decision = null;
        for (int tick = 103; tick <= 104; tick++) {
            decision = engine.tick(input(tick, map, PX, PY).npc(shaman(1, 20, 26).dead(true).build()).build());
        }
        assertNull(decision.getPrayer());
    }

    @Test
    void deathKeepsQueuedAttacksWhenRemovalDisabled() {
        EngineConfig keep = EngineConfig.builder().removeQueuedOnNpcDeath(false).build();
        PrayerEngine engine = new PrayerEngine();
        GridCollisionMap map = openMap();

        engine.tick(input(100, map, PX, PY).config(keep)
                .npc(shaman(1, 20, 26).animation(ANIM_SHAMAN).animationChanged(true).build())
                .build());

        engine.tick(input(101, map, PX, PY).config(keep).npc(shaman(1, 20, 26).build()).build());
        engine.tick(input(102, map, PX, PY).config(keep).npc(shaman(1, 20, 26).dead(true).build()).build());
        assertEquals(1, engine.getQueueView().size(), "entry must survive the death");

        // The shaman despawns entirely on 103: the entry lives on as a ghost.
        engine.tick(input(103, map, PX, PY).config(keep).build());
        assertEquals(1, engine.getQueueView().size());
        assertEquals(QueuedAttack.Source.GHOST, engine.getQueueView().get(0).getSource());

        PrayerDecision decision = engine.tick(input(104, map, PX, PY).config(keep).build());
        assertEquals(Prayer.PROTECT_FROM_MAGIC, decision.getPrayer(), "ghost entry still prays");
    }
}
