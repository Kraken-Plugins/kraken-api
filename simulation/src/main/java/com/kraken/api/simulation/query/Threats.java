package com.kraken.api.simulation.query;

import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Read only threat queries over a state, for planners' danger maps and for prayer plugins. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Threats {

    /**
     * The NPCs that could attack a tile from where they stand now: living NPCs that hold the wave open, with an
     * attack range, that have reach and sight of the tile ({@link Los#npcCanAttack}) and whose cooldown runs out
     * within {@code ticksAhead} ticks. Cooldowns tick down at the start of an NPC's turn and the attack fires at zero
     * or less, so an NPC with cooldown 1 or less can launch on the next advance. Spawn gates and the pack's own
     * attack rules (volley charges, fixed cycles) are not modeled: this is the reach and readiness superset.
     * @param st the state
     * @param tile the packed tile
     * @param ticksAhead how many advances to look ahead; {@code Integer.MAX_VALUE} ignores cooldowns
     * @return a bit per slot
     */
    public static int whoCanAttack(SimState st, int tile, int ticksAhead) {
        Frame f = st.frame;
        int px = Coords.x(tile);
        int py = Coords.y(tile);
        int mask = 0;
        for (int oi = 0; oi < f.npcOrderCount; oi++) {
            int i = f.npcOrder[oi];
            if (!st.alive(i)) {
                continue;
            }
            NpcDef def = f.def(i);
            if (!def.waveClearing || def.attackRange <= 0) {
                continue;
            }
            if (ticksAhead != Integer.MAX_VALUE && st.npcCooldown[i] > ticksAhead) {
                continue;
            }
            if (Los.npcCanAttack(f.grid, Coords.x(st.npcPos[i]), Coords.y(st.npcPos[i]), def.size,
                    def.attackRange, px, py)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }
}
