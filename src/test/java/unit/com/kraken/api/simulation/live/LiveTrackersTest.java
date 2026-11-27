package com.kraken.api.simulation.live;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.data.PipelineFlags;
import com.kraken.api.simulation.data.PlayerData;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The live layer's pure trackers (ported from Auto Colosseum V2's adapter tests): the region anchor, the slot
 * indexer, cooldown arithmetic against the engine's conventions, the hp ledger and corpse capture.
 */
class LiveTrackersTest {
    private static final int BASE_X = 1792;
    private static final int BASE_Y = 3072;
    private static final int REGION = 7216;

    @Test
    void anchorsOnlyInsideTheRequiredRegion() {
        RegionAnchor anchor = new RegionAnchor(REGION);
        assertFalse(anchor.anchor(new WorldPoint(3200, 3200, 0)));
        assertFalse(anchor.anchored());
        assertTrue(anchor.anchor(new WorldPoint(BASE_X + 17, BASE_Y + 20, 0)));
        assertTrue(anchor.anchored());
        assertEquals(BASE_X, anchor.baseX());
        assertEquals(BASE_Y, anchor.baseY());
    }

    @Test
    void anAnyRegionAnchorTakesTheFirstPointsRegion() {
        RegionAnchor anchor = new RegionAnchor();
        assertTrue(anchor.anchor(new WorldPoint(3200, 3210, 0)));
        assertEquals(3200 >> 6 << 6, anchor.baseX());
        assertEquals(3210 >> 6 << 6, anchor.baseY());
        anchor.reset();
        assertFalse(anchor.anchored());
    }

    @Test
    void localCoordinatesAreRegionRelativeAndRoundTrip() {
        RegionAnchor anchor = new RegionAnchor(REGION);
        assertTrue(anchor.anchor(new WorldPoint(BASE_X + 30, BASE_Y + 3, 0)));
        assertEquals(10 | 20 << 6, anchor.local(new WorldPoint(BASE_X + 10, BASE_Y + 20, 0), 0));
        assertEquals(0, anchor.local(new WorldPoint(BASE_X, BASE_Y, 0), 0));
        assertEquals(63 | 63 << 6, anchor.local(new WorldPoint(BASE_X + 63, BASE_Y + 63, 0), 0));
        assertEquals(new WorldPoint(BASE_X + 10, BASE_Y + 20, 0), anchor.template(10 | 20 << 6));
        // A size 3 NPC centred on local (12, 12) anchors SW at (11, 11).
        assertEquals(11 | 11 << 6, anchor.local(new WorldPoint(BASE_X + 12, BASE_Y + 12, 0), 1));
        assertEquals(-1, anchor.local(new WorldPoint(BASE_X - 1, BASE_Y + 5, 0), 0));
        assertEquals(-1, anchor.local(new WorldPoint(BASE_X + 64, BASE_Y + 5, 0), 0));
        assertEquals(-1, anchor.local(new WorldPoint(BASE_X + 5, BASE_Y + 64, 0), 0));
    }

    @Test
    void slotsAreStableAndRecycled() {
        NpcIndexer idx = new NpcIndexer();
        assertEquals(0, idx.slotForOrAssign(500));
        assertEquals(1, idx.slotForOrAssign(501));
        assertEquals(0, idx.slotForOrAssign(500));
        idx.slotForOrAssign(502);
        assertEquals(0b10, idx.releaseUnseen(0b101), "slot 1 freed");
        assertEquals(-1, idx.slotOf(501));
        assertEquals(1, idx.slotForOrAssign(503), "a new spawn recycles the freed slot");
        assertEquals(2, idx.slotOf(502));
        for (int i = 4; i < 17; i++) {
            idx.slotForOrAssign(1000 + i);
        }
        assertEquals(-1, idx.slotForOrAssign(2000), "a full table rejects new assignments");
    }

    @Test
    void npcCooldownCountsFromTheLaunchStampAndTheLag() {
        CooldownTracker tracker = new CooldownTracker();
        tracker.stamp(2, 100, 0, 0, 2, false);
        assertEquals(5, tracker.cooldownAt(2, 5, 100));
        assertEquals(2, tracker.cooldownAt(2, 5, 103));
        assertEquals(-1, tracker.cooldownAt(2, 5, 110));
        tracker.stamp(2, 100, 1, 0, 2, false);
        assertEquals(4, tracker.cooldownAt(2, 5, 100), "the real launch was the tick before the animation");
        assertEquals(0, tracker.cooldownAt(7, 5, 1000), "an unstamped slot reads ready");
    }

    @Test
    void playerCooldownAccountsForTheCheckBeforeDecrement() {
        CooldownTracker tracker = new CooldownTracker();
        assertEquals(0, tracker.playerCooldownAt(4, 1000));
        tracker.stampPlayerAttack(200);
        assertEquals(3, tracker.playerCooldownAt(4, 200));
        assertEquals(0, tracker.playerCooldownAt(4, 203));
        assertEquals(-1, tracker.playerCooldownAt(4, 205));
    }

    @Test
    void inFlightTracksUntilLandingAndBlockedLaunchesNeverReseed() {
        CooldownTracker tracker = new CooldownTracker();
        tracker.stamp(3, 100, 0, 3, 1, false);
        assertEquals(3, tracker.inFlightRemaining(3, 100));
        assertEquals(1, tracker.inFlightRemaining(3, 102));
        assertEquals(-1, tracker.inFlightRemaining(3, 103));
        assertEquals(1, tracker.inFlightStyle(3));
        tracker.stamp(3, 100, 0, 3, 1, true);
        assertEquals(-1, tracker.inFlightRemaining(3, 100));
        assertEquals(3, tracker.inFlightRemaining(3, 100, true));
        tracker.clearSlot(3);
        assertEquals(0, tracker.cooldownAt(3, 5, 101));
        assertEquals(-1, tracker.inFlightRemaining(3, 101));
    }

    @Test
    void hpLedgerTracksSplatsAndReanchorsOnTheBar() {
        HpLedger ledger = new HpLedger();
        ledger.onNpcHitsplat(0, 30, false);
        ledger.onNpcHitsplat(0, 12, false);
        ledger.onNpcHitsplat(0, 30, true);
        assertEquals(125 - 42 + 30, ledger.npcHp(0, 125, -1, -1));
        ledger.onNpcHitsplat(1, 500, false);
        assertEquals(1, ledger.npcHp(1, 125, -1, -1), "clamped above zero while alive");
        assertEquals(100, ledger.npcHp(2, 200, 15, 30), "the bar re anchors a lost hitsplat");
        ledger.onNpcHitsplat(2, 10, false);
        assertEquals(90, ledger.npcHp(2, 200, -1, -1));
        ledger.onNpcHitsplat(3, 60, false);
        assertEquals(65, ledger.npcHp(3, 125, 16, 30), "a bar within one step keeps the ledger");
        assertEquals(0, ledger.npcHp(4, 125, 0, 30), "a zero ratio means dead");
        ledger.clearSlot(0);
        assertEquals(125, ledger.npcHp(0, 125, -1, -1));
    }

    @Test
    void corpsesCountDownFromTheirFirstDeadSighting() {
        NpcDef def = new NpcDef();
        def.key = "brute";
        def.hp = 80;
        def.corpseTicks = 4;
        MechanicsData data = new MechanicsData(new NpcDef[]{def}, new PlayerData(), new PipelineFlags());
        assertTrue(CorpseTracker.leavesBody(def));
        CorpseTracker corpses = new CorpseTracker();
        Frame frame = new Frame();
        frame.data = data;
        SimState state = new SimState();
        state.frame = frame;
        corpses.capture(frame, state, 3, 0, 812, 10 | 12 << 6, -1, SimState.SPAWN_OPEN, 500);
        assertEquals(4, state.npcCorpse[3]);
        assertFalse(state.alive(3));
        assertEquals(812, frame.npcIndex[3]);
        assertEquals(80, frame.npcMaxHp[3]);
        assertEquals(2, corpses.corpseTicks(3, def, 502));
        assertEquals(1, corpses.corpseTicks(3, def, 510), "never below one while the body is seen");
        corpses.clearSlot(3);
        assertEquals(4, corpses.corpseTicks(3, def, 600));
        def.noBody = true;
        assertFalse(CorpseTracker.leavesBody(def));
    }
}
