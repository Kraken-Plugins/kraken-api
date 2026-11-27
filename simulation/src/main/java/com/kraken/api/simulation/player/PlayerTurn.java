package com.kraken.api.simulation.player;

import com.kraken.api.simulation.data.EquipSet;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.data.PlayerData;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Footprints;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.hits.Hits;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.move.Routes;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.Scratch;
import com.kraken.api.simulation.world.SimState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Player side tick processing: movement with run energy, and the player's attack. The player walks 1 or runs 2
 * tiles per tick along the cached OSRS path; energy drains only on a full 2 tile running tick and regenerates
 * otherwise, and run toggles itself off when energy cannot cover the tick's drain. An attack click on a target out of
 * reach walks toward it, stopping on the first tile it can attack from, and attacks that same tick. The attack fires
 * when engaged, in range (less the frame's range penalty) with line of sight, and off cooldown; damage comes from the
 * current gear set's per type table and lands after the OSRS projectile delay plus one tick (NPCs process before
 * players within a tick).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerTurn {

    /**
     * The player's movement for this tick.
     * @param st the state
     * @param sc the thread's scratch
     */
    public static void run(SimState st, Scratch sc) {
        Frame f = st.frame;
        PlayerData pd = f.data.player;
        if (st.moveDest >= 0 && st.playerPos == st.moveDest) {
            clearPath(st);
        }
        int consumed = 0;
        if (st.moveDest < 0 && st.targetSlot >= 0) {
            consumed = chase(st, sc, f, pd);
        } else if (st.moveDest >= 0) {
            int pathId = sc.resolvePath(f.grid, st.pathId, st.pathKey);
            st.pathId = (short) pathId;
            int len = sc.pathLen(pathId);
            if (len == 0) {
                clearPath(st);
            } else {
                int steps = 1;
                int drain = 0;
                if (st.running) {
                    drain = pd.runDrainPerTick(f.playerWeight, f.agilityLevel);
                    if (st.runEnergy100 >= drain) {
                        steps = 2;
                    } else {
                        st.running = false;
                    }
                }
                while (steps-- > 0 && st.pathCursor < len) {
                    st.playerPos = sc.pathTile(pathId, st.pathCursor++);
                    consumed++;
                }
                if (consumed == 2) {
                    st.runEnergy100 -= (short) drain;
                }
                if (st.pathCursor >= len) {
                    clearPath(st);
                }
            }
        }
        if (consumed < 2) {
            int e = st.runEnergy100 + pd.runRegenPerTick(f.agilityLevel);
            st.runEnergy100 = (short) Math.min(10000, e);
        }
    }

    /** Walks toward the engaged target when it cannot be attacked from here; returns the tiles consumed. */
    private static int chase(SimState st, Scratch sc, Frame f, PlayerData pd) {
        int slot = st.targetSlot;
        if (f.equipSets == null || !st.alive(slot)) {
            return 0;
        }
        EquipSet set = st.gearSet < f.equipSets.length ? f.equipSets[st.gearSet] : null;
        if (set == null) {
            return 0;
        }
        NpcDef def = f.def(slot);
        int range = Math.max(1, set.attackRange - f.playerRangePenalty);
        int steps = 1;
        int drain = 0;
        if (st.running) {
            drain = pd.runDrainPerTick(f.playerWeight, f.agilityLevel);
            if (st.runEnergy100 >= drain) {
                steps = 2;
            } else {
                st.running = false;
            }
        }
        int n = Routes.toAttackTile(sc, f.grid, st.playerPos & 0xfff, Coords.x(st.npcPos[slot]),
                Coords.y(st.npcPos[slot]), def.size, set.styleCode, range, steps);
        if (n == 0) {
            return 0;
        }
        st.playerPos = sc.routeOut[n - 1];
        if (n == 2) {
            st.runEnergy100 -= (short) drain;
        }
        return n;
    }

    /**
     * Drops the player's click destination and path.
     * @param st the state
     */
    public static void clearPath(SimState st) {
        st.moveDest = Coords.NONE;
        st.pathKey = -1;
        st.pathId = -1;
        st.pathCursor = 0;
    }

    /**
     * The player's attack for this tick, when engaged.
     * @param st the state
     */
    public static void attack(SimState st) {
        if (st.targetSlot < 0) {
            return;
        }
        Frame f = st.frame;
        if (f.equipSets == null) {
            return;
        }
        int slot = st.targetSlot;
        if (!st.alive(slot)) {
            st.targetSlot = -1;
            return;
        }
        EquipSet set = st.gearSet < f.equipSets.length ? f.equipSets[st.gearSet] : null;
        if (set == null || st.attackCooldown > 0) {
            return;
        }
        NpcDef def = f.def(slot);
        int px = Coords.x(st.playerPos);
        int py = Coords.y(st.playerPos);
        int nx = Coords.x(st.npcPos[slot]);
        int ny = Coords.y(st.npcPos[slot]);
        int range = Math.max(1, set.attackRange - f.playerRangePenalty);
        if (!Los.playerCanAttack(f.grid, px, py, nx, ny, def.size, set.styleCode, range)) {
            return;
        }
        int dist = set.styleCode == Style.MELEE ? 1 : Footprints.toTile(nx, ny, def.size, px, py);
        int type = f.npcTypeIdx[slot];
        int expected = set.expectedDamage[type];
        if (st.specArmed && st.specEnergy10 >= set.specCost10) {
            expected = expected * set.specMult1000 / 1000;
            st.specEnergy10 -= (short) set.specCost10;
            st.specArmed = false;
        }
        int delay = hitDelay(set.styleCode, dist);
        Hits.push(st, Hits.vsNpc(Hits.encode(delay, set.styleCode,
                Math.min(511, expected), 0, Hits.SRC_PLAYER, slot, 1)));
        if (set.bounce) {
            bounce(st, f, set, slot, def, delay);
        }
        st.attackCooldown = (byte) set.attackSpeed;
    }

    /** Venator style ricochet: half damage onto the nearest other NPC within 3 tiles of the primary's footprint. */
    private static void bounce(SimState st, Frame f, EquipSet set, int primary, NpcDef primaryDef, int delay) {
        int nx = Coords.x(st.npcPos[primary]);
        int ny = Coords.y(st.npcPos[primary]);
        int best = -1;
        int bestDist = 4;
        for (int oi = 0; oi < f.npcOrderCount; oi++) {
            int i = f.npcOrder[oi];
            if (i == primary || !st.alive(i) || !f.def(i).waveClearing) {
                continue;
            }
            NpcDef od = f.def(i);
            int d = Footprints.distance(nx, ny, primaryDef.size,
                    Coords.x(st.npcPos[i]), Coords.y(st.npcPos[i]), od.size);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        if (best >= 0) {
            int bounceDamage = set.expectedDamage[f.npcTypeIdx[best]] / 2;
            if (bounceDamage > 0) {
                Hits.push(st, Hits.vsNpc(Hits.encode(delay, set.styleCode,
                        Math.min(511, bounceDamage), 0, Hits.SRC_PLAYER, best, 1)));
            }
        }
    }

    /**
     * Player hit delay: melee 0, ranged 1+(3+d)/6, magic 1+(1+d)/3, plus one tick because the receiving NPC
     * processes before the player within a tick.
     */
    private static int hitDelay(int style, int dist) {
        int base = style == Style.RANGED ? 1 + (3 + dist) / 6
                : style == Style.MAGIC ? 1 + (1 + dist) / 3 : 0;
        return base + 1;
    }
}
