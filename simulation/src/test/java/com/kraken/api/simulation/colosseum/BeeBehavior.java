package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.tick.TickContext;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

/**
 * Bee swarm (the Bees! modifier): crawls a tile toward covering the player every 2 ticks, through collision (capture
 * section 11: steps on add+1, then every 2), and stings through {@link ColosseumRules#contact}. Killed, it comes back
 * on its add tile 55 ticks after the kill and crawls 5 ticks later.
 */
public final class BeeBehavior extends NpcBehavior {

    @Override
    public boolean onTurnStart(TickContext c, int i) {
        SimState st = c.state();
        Mods mods = ColosseumFrame.mods(st.frame);
        if (mods.beeMoveEvery <= 0) {
            return true;
        }
        byte[] crawl = st.npcLane[Lanes.BEE_CRAWL];
        if (++crawl[i] >= mods.beeMoveEvery) {
            crawl[i] = 0;
            NpcDef def = st.frame.def(i);
            int px = c.playerX();
            int py = c.playerY();
            int x = Coords.x(st.npcPos[i]);
            int y = Coords.y(st.npcPos[i]);
            int tx = Math.max(px - def.size + 1, Math.min(px, x));
            int ty = Math.max(py - def.size + 1, Math.min(py, y));
            st.npcPos[i] = Coords.pack(x + Integer.signum(tx - x), y + Integer.signum(ty - y));
        }
        return true;
    }

    @Override
    public void onDeadTurn(TickContext c, int i) {
        SimState st = c.state();
        byte[] respawn = st.npcLane[Lanes.BEE_RESPAWN];
        if (respawn[i] > 0 && --respawn[i] == 0) {
            Frame f = st.frame;
            if (f.npcHomePos[i] < 0) {
                return;
            }
            st.npcPos[i] = f.npcHomePos[i];
            st.npcHp[i] = 1;
            st.npcFlags[i] |= SimState.FLAG_ALIVE;
            st.npcSpawnTick[i] = (short) st.tick;
            st.npcLane[Lanes.BEE_CRAWL][i] = (byte) (ColosseumFrame.mods(f).beeMoveEvery - 5);
        }
    }

    @Override
    public void onDeath(TickContext c, int i) {
        SimState st = c.state();
        st.accLane[Lanes.ACC_BEE_KILLS]++;
        // Counted down on its own turn from this tick: back respawnTicks after the kill.
        st.npcLane[Lanes.BEE_RESPAWN][i] = (byte) Math.min(127, ColosseumFrame.mods(st.frame).beeRespawnTicks + 1);
    }
}
