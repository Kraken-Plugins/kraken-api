package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Footprints;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.move.MoveNaive;
import com.kraken.api.simulation.npc.PackRules;
import com.kraken.api.simulation.tick.TickContext;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

/**
 * The Colosseum's world scope mechanics: one manticore volley per tick, the Solarflare flares, bee stings, Reentry
 * sand, Totemic totem spawns, Volatility blasts and the reinforcement schedule. All hazard damage is typeless: no
 * prayer reduces it.
 */
public final class ColosseumRules extends PackRules {
    /** {@code TickContext.tickScratch} bit: a manticore started a volley this tick. */
    public static final int TICK_VOLLEY_STARTED = 1;

    private final ColosseumData col;

    ColosseumRules(ColosseumData col) {
        this.col = col;
    }

    @Override
    public void onTickStart(TickContext c) {
        SimState st = c.state();
        int solar = Lanes.solar(st);
        if (solar >= 0) {
            Lanes.setSolar(st, Solar.step(solar, ColosseumFrame.mods(st.frame)));
        }
    }

    /** After a volley start, every other ready charged manticore is pushed back its stagger. */
    @Override
    public void afterNpcTurns(TickContext c) {
        if ((c.tickScratch & TICK_VOLLEY_STARTED) == 0) {
            return;
        }
        SimState st = c.state();
        Frame f = st.frame;
        for (int oi = 0; oi < f.npcOrderCount; oi++) {
            int i = f.npcOrder[oi];
            if (!st.alive(i) || st.npcCooldown[i] > 0) {
                continue;
            }
            NpcDef def = f.def(i);
            if (def.behaviorImpl instanceof ManticoreBehavior && (st.npcFlags[i] & Lanes.FLAG_CHARGED) != 0) {
                st.npcCooldown[i] = (byte) ((ManticoreBehavior) def.behaviorImpl).cfg.staggerTicks;
            }
        }
    }

    /** Reentry sand at a toss's impact tile: I the tile, II and the SW adjacent tile, III and the west tile. */
    @Override
    public void onTileHitLanded(TickContext c, int tile) {
        Mods mods = ColosseumFrame.mods(c.frame());
        if (mods.reentryTier <= 0) {
            return;
        }
        SimState st = c.state();
        int x = Coords.x(tile);
        int y = Coords.y(tile);
        Grid.set(st.moltenSand, x, y);
        if (mods.reentryTier >= 2 && x > 0 && y > 0) {
            Grid.set(st.moltenSand, x - 1, y - 1);
        }
        if (mods.reentryTier >= 3 && x > 0) {
            Grid.set(st.moltenSand, x - 1, y);
        }
    }

    /** Bee swarms whose 2x2 covers the player, and the Solarflare flares (tier III also knocks the overhead off). */
    @Override
    public void contact(TickContext c) {
        SimState st = c.state();
        Frame f = st.frame;
        Mods mods = ColosseumFrame.mods(f);
        int px = Coords.x(st.playerPos);
        int py = Coords.y(st.playerPos);
        if (mods.beeSwarms > 0) {
            for (int oi = 0; oi < f.npcOrderCount; oi++) {
                int i = f.npcOrder[oi];
                if (!st.alive(i) || !(f.def(i).behaviorImpl instanceof BeeBehavior)) {
                    continue;
                }
                int bx = Coords.x(st.npcPos[i]);
                int by = Coords.y(st.npcPos[i]);
                int size = f.def(i).size;
                if (px >= bx && px < bx + size && py >= by && py < by + size) {
                    c.damagePlayer(mods.beeExpectedDamage, mods.beeDamage, 1);
                }
            }
        }
        int solar = Lanes.solar(st);
        if (solar >= 0 && Solar.on(col, solar, st.playerPos)) {
            c.damagePlayer(mods.solarDamage, mods.solarDamage, 1);
            if (mods.solarDisablesPrayer) {
                st.overhead = 0;
            }
        }
    }

    /**
     * Totemic: a wave holding NPC at half health or less spawns a totem on the tile SW of its SW tile (NE of its NE
     * tile when that is blocked), first healing it 7 ticks later and every 6 after (capture section 11). Totem slots
     * run after every live NPC, so the countdown starts one higher to land the first heal on spawn + 7.
     */
    @Override
    public void onNpcDamaged(TickContext c, int slot, int damage) {
        SimState st = c.state();
        Frame f = st.frame;
        Mods mods = ColosseumFrame.mods(f);
        if (!mods.totemic) {
            return;
        }
        NpcDef def = f.def(slot);
        if (!def.waveClearing || (st.npcFlags[slot] & Lanes.FLAG_TOTEM_SPAWNED) != 0
                || st.npcHp[slot] * 100 > f.npcMaxHp[slot] * mods.totemSpawnBelowPct) {
            return;
        }
        ColosseumFrame cf = ColosseumFrame.of(f);
        for (int k = 0; k < SimState.MAX_NPCS; k++) {
            if (!cf.totemPool(k) || st.alive(k) || (st.npcFlags[k] & SimState.FLAG_SPAWNED) != 0) {
                continue;
            }
            int x = Coords.x(st.npcPos[slot]);
            int y = Coords.y(st.npcPos[slot]);
            int anchor = totemTile(st, x - 1, y - 1);
            if (anchor < 0) {
                anchor = totemTile(st, x + def.size, y + def.size);
            }
            if (anchor < 0) {
                return;
            }
            st.npcPos[k] = (short) anchor;
            st.npcHp[k] = (short) mods.totemHp;
            st.npcFlags[k] |= SimState.FLAG_ALIVE | SimState.FLAG_SPAWNED;
            st.npcSpawnTick[k] = (short) st.tick;
            st.npcLane[Lanes.TOTEM_BENEFICIARY][k] = (byte) slot;
            st.npcLane[Lanes.TOTEM_HEAL][k] = (byte) (mods.totemFirstHeal + 1);
            st.setFootprint(k, 1);
            st.packLane[Lanes.ALIVE_TOTEMS]++;
            st.npcFlags[slot] |= Lanes.FLAG_TOTEM_SPAWNED;
            return;
        }
    }

    private static int totemTile(SimState st, int x, int y) {
        if (!MoveNaive.legal(st, st.frame.grid, x, y, 1) || (x | y << 6) == st.playerPos) {
            return -1;
        }
        return x | y << 6;
    }

    /**
     * Volatility blast as a wave holding NPC's body is deleted (capture: the gib and explosion come on the dying
     * NPC's deletion tick, centred on its centre tile): damages the player and (per data flag) other NPCs whose
     * footprint sits within {@code size + radiusDelta} of the dying footprint; tier III leaves molten sand on the
     * centre tile. Returns the slots the blast killed so the engine can chain explosions.
     */
    @Override
    public int onBodyDeleted(TickContext c, int slot) {
        SimState st = c.state();
        Frame f = st.frame;
        Mods mods = ColosseumFrame.mods(f);
        NpcDef def = f.def(slot);
        if (!def.waveClearing || mods.volatilityTier <= 0) {
            return 0;
        }
        int radius = def.size + mods.volatilityRadiusDelta;
        int nx = Coords.x(st.npcPos[slot]);
        int ny = Coords.y(st.npcPos[slot]);
        int px = Coords.x(st.playerPos);
        int py = Coords.y(st.playerPos);
        if (Footprints.distance(nx, ny, def.size, px, py, 1) <= radius) {
            c.damagePlayer(mods.volatilityMaxHit / 2, mods.volatilityMaxHit, 1);
        }
        int killed = 0;
        if (mods.volatilityHitsNpcs) {
            for (int oi = 0; oi < f.npcOrderCount; oi++) {
                int i = f.npcOrder[oi];
                if (i == slot || !st.alive(i)) {
                    continue;
                }
                NpcDef od = f.def(i);
                int ox = Coords.x(st.npcPos[i]);
                int oy = Coords.y(st.npcPos[i]);
                if (Footprints.distance(nx, ny, def.size, ox, oy, od.size) <= radius) {
                    st.npcHp[i] -= (short) (mods.volatilityMaxHit / 2);
                    if (st.npcHp[i] <= 0) {
                        killed |= 1 << i;
                    }
                }
            }
        }
        if (mods.volatilitySand) {
            Grid.set(st.moltenSand, nx + def.size / 2, ny + def.size / 2);
        }
        return killed;
    }

    /** The totem tick accumulator, then the reinforcement schedule while the wave is open. */
    @Override
    public void endOfTick(TickContext c) {
        SimState st = c.state();
        if (st.packLane[Lanes.ALIVE_TOTEMS] > 0) {
            st.accLane[Lanes.ACC_TOTEM_TICKS]++;
        }
        if (st.waveClearTick < 0) {
            spawnReinforcements(c, st);
        }
    }

    /**
     * Reinforcements spawn on wave tick 66, all from one gate: the north gate when the player's y at the start of the
     * tick is 34 or more, else the south gate, each on its own x of 31 to 33 (capture section 1.2, 69/69 waves;
     * region local tiles). A spawn that cannot be placed retries next tick. Reinforcements arriving exactly as the
     * wave clears never spawn (they die automatically in the real game).
     */
    private void spawnReinforcements(TickContext c, SimState st) {
        Frame f = st.frame;
        ColosseumFrame cf = f.pack instanceof ColosseumFrame ? (ColosseumFrame) f.pack : null;
        int t = c.tick();
        if (cf == null || cf.reinforcementBits == 0 || t + 1 < col.waves.reinforcementTick) {
            return;
        }
        WaveData wd = col.waves;
        boolean north = c.playerY() >= wd.northWhenPlayerYAtLeast;
        WaveData.Gate gate = north ? wd.northGate : wd.southGate;
        int order = 0;
        for (int i = 0; i < SimState.MAX_NPCS; i++) {
            if (!cf.reinforcement(i)) {
                continue;
            }
            int xIdx = order++;
            if (st.alive(i) || (st.npcFlags[i] & SimState.FLAG_SPAWNED) != 0) {
                continue;
            }
            NpcDef def = f.def(i);
            int x = wd.reinforcementXs[Math.min(xIdx, wd.reinforcementXs.length - 1)];
            int anchor = gateAnchor(st, def.size, x, gate.y, north);
            if (anchor < 0) {
                continue;
            }
            st.npcPos[i] = (short) anchor;
            st.npcHp[i] = f.npcMaxHp[i];
            st.npcFlags[i] |= SimState.FLAG_ALIVE | SimState.FLAG_SPAWNED;
            st.npcCooldown[i] = 0;
            for (int lane = 0; lane < SimState.NPC_LANES; lane++) {
                st.npcLane[lane][i] = 0;
            }
            st.npcCorpse[i] = 0;
            st.npcSpawnTick[i] = (short) t;
            if (!def.noBody) {
                st.setFootprint(i, def.size);
            }
        }
    }

    /** The gate tile, else the nearest legal anchor scanning inward from the gate row. */
    private static int gateAnchor(SimState st, int size, int gateX, int gateY, boolean north) {
        Grid g = st.frame.grid;
        int ppx = Coords.x(st.playerPos);
        int ppy = Coords.y(st.playerPos);
        for (int row = 0; row < 6; row++) {
            int y = north ? gateY - row : gateY + row;
            if (y < 0 || y + size > Grid.SIZE) {
                continue;
            }
            for (int spread = 0; spread <= 4; spread++) {
                for (int sign = 0; sign < (spread == 0 ? 1 : 2); sign++) {
                    int x = gateX + (sign == 0 ? spread : -spread);
                    if (x < 0 || x + size > Grid.SIZE || !MoveNaive.legal(st, g, x, y, size)) {
                        continue;
                    }
                    if (ppx >= x && ppx < x + size && ppy >= y && ppy < y + size) {
                        continue;
                    }
                    return x | y << 6;
                }
            }
        }
        return -1;
    }
}
