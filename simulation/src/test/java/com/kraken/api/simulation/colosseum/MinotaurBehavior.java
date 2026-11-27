package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.hits.Hits;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.tick.TickContext;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

/**
 * Minotaur: with the player in melee reach at cooldown expiry it swings, landing a tick later and blocked only when
 * Protect from Melee is up at the swing and at landing (npcs.json prayerCheck). Otherwise it heals the most damaged
 * qualifying NPC to its full bar (below 75% max HP, centre within 7 tiles, line of sight, never another minotaur),
 * and the heal holds it 10 ticks (capture section 5b).
 */
public final class MinotaurBehavior extends NpcBehavior {
    final ColosseumNpc.Minotaur cfg;

    MinotaurBehavior(ColosseumNpc.Minotaur cfg) {
        this.cfg = cfg;
    }

    @Override
    public void attack(TickContext c, int i) {
        SimState st = c.state();
        if (!c.canAttack() || st.npcCooldown[i] > 0) {
            return;
        }
        Frame f = st.frame;
        NpcDef def = f.def(i);
        if (c.los()) {
            st.npcCooldown[i] = (byte) def.attackSpeed;
            int worst = def.maxHit + f.damage.npcMaxHitBonus;
            int expected = worst * Hits.unprayedFactor1000(f) / 1000;
            st.tickStyleMask |= (byte) (1 << Style.MELEE);
            st.tickMeleeWorst += (short) worst;
            long hit = Hits.encode(def.hitDelayTicks(1), Style.MELEE, expected, worst, i, 0, 1);
            boolean prayedAtSwing = st.overhead == Style.overheadFor(Style.MELEE);
            if (!prayedAtSwing) {
                Hits.push(st, hit);
            } else {
                c.missedFlickRisk(worst);
                if (def.prayerCheckCode == NpcDef.PRAYER_LAUNCH_AND_LAND) {
                    Hits.push(st, Hits.checkAtLand(hit));
                }
            }
            c.notifyAttack(i, Style.MELEE);
            return;
        }
        int cx = Coords.x(st.npcPos[i]) + def.size / 2;
        int cy = Coords.y(st.npcPos[i]) + def.size / 2;
        int best = -1;
        int bestMissing = 0;
        for (int oi = 0; oi < f.npcOrderCount; oi++) {
            int j = f.npcOrder[oi];
            if (j == i || !st.alive(j)) {
                continue;
            }
            NpcDef od = f.def(j);
            if (od.behaviorImpl instanceof MinotaurBehavior || !od.waveClearing) {
                continue;
            }
            if (st.npcHp[j] * 100 >= f.npcMaxHp[j] * cfg.healBelowPct) {
                continue;
            }
            int ox = Coords.x(st.npcPos[j]) + od.size / 2;
            int oy = Coords.y(st.npcPos[j]) + od.size / 2;
            if (Math.max(Math.abs(ox - cx), Math.abs(oy - cy)) > cfg.healRange) {
                continue;
            }
            if (!Los.rayClear(f.grid, cx, cy, ox, oy)) {
                continue;
            }
            int missing = f.npcMaxHp[j] - st.npcHp[j];
            if (missing > bestMissing) {
                bestMissing = missing;
                best = j;
            }
        }
        if (best >= 0) {
            st.npcCooldown[i] = (byte) cfg.healHoldTicks;
            c.healNpc(best, bestMissing);
            st.accLane[Lanes.ACC_MINOTAUR_HEALS]++;
            c.notifyHeal(i, best, bestMissing);
        }
    }
}
