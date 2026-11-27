package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.hits.Hits;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.tick.TickContext;
import com.kraken.api.simulation.world.SimState;

/**
 * Javelin colossus: four autos, then a toss (capture: 404/404). The toss picks the tile the player stands on 3 ticks
 * after it (shown by map animations 1446 and 2674, 2677 under Reentry) and blasts it 3 ticks after that, ignoring
 * prayer; it is dodged by not being there. Reentry sand appears at the impact tile when it lands
 * ({@link ColosseumRules}).
 */
public final class JavelinBehavior extends NpcBehavior {
    final ColosseumNpc.Javelin cfg;

    JavelinBehavior(ColosseumNpc.Javelin cfg) {
        this.cfg = cfg;
    }

    @Override
    public void attack(TickContext c, int i) {
        SimState st = c.state();
        if (!c.canAttack() || !c.los() || st.npcCooldown[i] > 0) {
            return;
        }
        NpcDef def = st.frame.def(i);
        st.npcCooldown[i] = (byte) def.attackSpeed;
        byte[] count = st.npcLane[Lanes.JAVELIN_COUNT];
        int since = Math.max(0, count[i]);
        boolean special = since + 1 >= cfg.specialEvery;
        if (special) {
            count[i] = 0;
            int expected = cfg.skyMaxHit * cfg.skyExpectedFactor1000 / 1000;
            Hits.push(st, Hits.tilePending(Hits.encode(cfg.landDelayTicks, Style.TYPELESS, expected, cfg.skyMaxHit,
                    i, 0, 1), cfg.shadowToLandTicks));
            c.notifyTileAttack(i, st.playerPos & 0xfff, cfg.landDelayTicks);
        } else {
            count[i] = (byte) (since + 1);
            c.launchStyled(i, def.styleCode, def.maxHit, 1, c.hitDelay(i), false);
        }
        c.notifyAttack(i, special ? Style.TYPELESS : def.styleCode);
    }
}
