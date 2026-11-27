package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.move.MoveSmart;
import com.kraken.api.simulation.move.Routes;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.tick.TickContext;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

/**
 * Fremennik warband member (capture sections 2 and 5e, the seven traces): it may attack only on its tick of a 6 tick
 * cycle counted from its spawn (berserker +3, seer +4, archer +5), only while standing on its own side of the
 * player's start of tick tile (berserker north, archer west, seer east; south when that side is statically blocked)
 * from before this tick's step, and the hit lands a tick later. A cycle tick it is not there for is skipped. Every
 * tick it is not on its side it runs (2 tiles) along the OSRS route to it, through other NPCs (pass through and the
 * run are data).
 */
public final class WarbandBehavior extends NpcBehavior {
    final ColosseumNpc.Warband cfg;

    WarbandBehavior(ColosseumNpc.Warband cfg) {
        this.cfg = cfg;
    }

    @Override
    public boolean turn(TickContext c, int i) {
        SimState st = c.state();
        Frame f = st.frame;
        NpcDef def = f.def(i);
        boolean cycleTick = cycleTick(st, i, def, c.tick());
        int side = side(f.grid, cfg, c.playerX(), c.playerY());
        int pos = st.npcPos[i];
        if (pos == side) {
            if (cycleTick && c.canAttack()) {
                c.launchStyled(i, def.styleCode, def.maxHit, 1, def.hitDelayTicks(1), false);
                c.notifyAttack(i, def.styleCode);
            }
            return true;
        }
        if (!c.canMove()) {
            return true;
        }
        if (side < 0) {
            // Both its side and the south are walls: close in like a smart pather.
            st.clearFootprint(i, def.size);
            MoveSmart.step(st, c.scratch(), i, def.size, c.playerX(), c.playerY());
            st.setFootprint(i, def.size);
            if (st.npcPos[i] != pos) {
                st.npcFlags[i] |= SimState.FLAG_MOVED;
            }
            return true;
        }
        int steps = Routes.toTile(c.scratch(), f.grid, pos, side, def.moveSpeed);
        if (steps > 0) {
            st.clearFootprint(i, def.size);
            st.npcPos[i] = c.scratch().routeOut[steps - 1];
            st.setFootprint(i, def.size);
            st.npcFlags[i] |= SimState.FLAG_MOVED;
        }
        return true;
    }

    /**
     * Whether tick {@code t} is one of the member's cycle ticks, and leaves its cooldown at the ticks until its next
     * one (the same "attacks when the decremented cooldown reaches 0" meaning every other NPC's cooldown has). With
     * no known spawn the captured cooldown carries the phase.
     */
    private boolean cycleTick(SimState st, int i, NpcDef def, int t) {
        int spawn = st.npcSpawnTick[i];
        if (spawn == SimState.SPAWN_OPEN) {
            if (st.npcCooldown[i] <= 0) {
                st.npcCooldown[i] = (byte) def.attackSpeed;
                return true;
            }
            return false;
        }
        int next = ticksToCycle(def, cfg, spawn, t);
        st.npcCooldown[i] = (byte) (next == 0 ? def.attackSpeed : next);
        return next == 0;
    }

    /** Ticks from {@code t} to the member's next cycle tick at or after it. */
    public static int ticksToCycle(NpcDef def, ColosseumNpc.Warband w, int spawnTick, int t) {
        int k = t - spawnTick - w.cycleOffset;
        return k < 0 ? -k : (def.attackSpeed - k % def.attackSpeed) % def.attackSpeed;
    }

    /** The member's attack tile beside the player, its own side or the south fallback when that is statically
     *  blocked; -1 when both are. */
    public static int side(Grid g, ColosseumNpc.Warband w, int px, int py) {
        int x = px + w.slotDx;
        int y = py + w.slotDy;
        if (x >= 0 && y >= 0 && x < Grid.SIZE && y < Grid.SIZE && !g.blockedMove(x, y)) {
            return x | y << 6;
        }
        x = px + w.fallbackDx;
        y = py + w.fallbackDy;
        if (x >= 0 && y >= 0 && x < Grid.SIZE && y < Grid.SIZE && !g.blockedMove(x, y)) {
            return x | y << 6;
        }
        return -1;
    }
}
