package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.hits.Hits;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.tick.TickContext;
import com.kraken.api.simulation.world.SimState;

/**
 * Healing totem (the Totemic modifier): casts a heal on its beneficiary on its cadence; the heal flies and fizzles if
 * the totem dies first. Spawned by {@link ColosseumRules#onNpcDamaged}.
 */
public final class TotemBehavior extends NpcBehavior {

    @Override
    public boolean onTurnStart(TickContext c, int i) {
        SimState st = c.state();
        byte[] heal = st.npcLane[Lanes.TOTEM_HEAL];
        if (--heal[i] > 0) {
            return true;
        }
        Mods mods = ColosseumFrame.mods(st.frame);
        heal[i] = (byte) mods.totemHealEvery;
        int ben = st.npcLane[Lanes.TOTEM_BENEFICIARY][i];
        if (ben >= 0 && st.alive(ben)) {
            int amount = Math.min(Math.min(511, mods.totemHealMax), st.frame.npcMaxHp[ben] * mods.totemHealPct / 100);
            Hits.push(st, Hits.heal(Hits.encode(mods.totemHealFlight, 0, amount, 0, i, ben, 1)));
            c.notifyHeal(i, ben, amount);
        }
        return true;
    }

    @Override
    public void onDeath(TickContext c, int i) {
        SimState st = c.state();
        st.accLane[Lanes.ACC_TOTEM_KILLS]++;
        st.packLane[Lanes.ALIVE_TOTEMS]--;
        st.npcLane[Lanes.TOTEM_HEAL][i] = 0;
    }
}
