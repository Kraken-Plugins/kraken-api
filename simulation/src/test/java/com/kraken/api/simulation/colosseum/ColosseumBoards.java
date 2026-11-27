package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.util.Arrays;
import java.util.Random;

/**
 * Deterministic synthetic wave construction from waves.json (V2's SyntheticWaves): spawn locations are randomized
 * per wave in the real game; a seed fixes them. Player side frame fields (gear, hp bases, supplies) are the caller's
 * to fill.
 */
public final class ColosseumBoards {
    private ColosseumBoards() {
    }

    /**
     * Populates {@code frame} and {@code state} with the wave's spawn set, reserved reinforcement slots, modifier
     * spawns (bees, totem pool), and wave start gates. Manticores start uncharged with a seeded pattern.
     */
    public static void build(ColosseumPack pack, Grid grid, int wave, Mods mods, long seed,
                             int playerPos, Frame frame, SimState state) {
        MechanicsData data = pack.data();
        WaveData waves = pack.colosseum().waves;
        frame.grid = grid;
        frame.data = data;
        mods.apply(frame);
        ColosseumFrame cf = ColosseumFrame.of(frame);
        frame.waveNumber = (byte) wave;
        frame.waveStartGates = true;
        frame.npcSmartPathBits = 0;
        cf.reinforcementBits = 0;
        cf.totemPoolBits = 0;
        for (int i = 0; i < SimState.MAX_NPCS; i++) {
            frame.npcTypeIdx[i] = -1;
            frame.npcIndex[i] = -1;
            frame.npcHomePos[i] = Coords.NONE;
            state.npcFlags[i] = 0;
            state.npcPos[i] = Coords.NONE;
            state.npcHp[i] = 0;
            state.npcCooldown[i] = 0;
            for (int lane = 0; lane < SimState.NPC_LANES; lane++) {
                state.npcLane[lane][i] = 0;
            }
            state.npcSpawnTick[i] = SimState.SPAWN_OPEN;
            state.npcCorpse[i] = 0;
        }
        state.frame = frame;
        state.playerPos = (short) playerPos;
        state.tick = 0;
        state.waveClearTick = -1;
        state.pendingCount = 0;
        Arrays.fill(state.packLane, 0);
        if (mods.solarTier > 0) {
            Lanes.setSolar(state, Solar.start(mods));
        }
        Arrays.fill(state.moltenSand, 0L);
        // Wave transitions clear the player's transient interaction and combat state: interactions drop with the
        // despawn, Doom stacks reset (A16), timers restart.
        state.targetSlot = -1;
        state.moveDest = Coords.NONE;
        state.pathKey = -1;
        state.pathId = -1;
        state.pathCursor = 0;
        state.attackCooldown = 0;
        state.foodDelay = 0;
        state.potDelay = 0;
        state.karamDelay = 0;
        state.overhead = 0;
        state.specArmed = false;
        state.hitStacksWorst = 0;
        state.venomState = 0;
        state.prayerDrainAcc = 0;

        Random rng = new Random(seed);
        int slot = 0;
        WaveData.Wave waveDef = waves.forWave(wave);
        if (waveDef == null) {
            throw new IllegalArgumentException("no wave " + wave);
        }
        short[] trio = warbandFormation(waves, grid, mods.quartet, rng);
        boolean[] pointUsed = new boolean[waves.spawnPoints.length];
        for (String key : waveDef.spawns) {
            NpcDef def = data.npcs[data.typeIndex(key)];
            int forced = ColosseumPack.warband(def) != null
                    ? trioTile(trio, key) : spawnPoint(waves, grid, def, rng, playerPos, pointUsed);
            slot = spawn(pack, grid, frame, state, slot, key, rng, playerPos, true, forced);
        }
        if (mods.quartet) {
            slot = spawn(pack, grid, frame, state, slot, waves.quartetExtra, rng, playerPos,
                    true, trio != null ? trio[3] : Coords.NONE);
        }
        if (mods.dynamicDuo) {
            for (int i = 0; i < slot; i++) {
                if ("shockwave_colossus".equals(frame.def(i).key)) {
                    int partner = ringTwoTile(grid, state, frame.def(i), state.npcPos[i]);
                    slot = spawn(pack, grid, frame, state, slot, "shockwave_colossus", rng, playerPos, true, partner);
                    break;
                }
            }
        }
        shareFirstManticorePattern(frame, state, slot);
        for (String key : waveDef.reinforcements) {
            int s = slot;
            slot = spawn(pack, grid, frame, state, slot, key, rng, playerPos, false, Coords.NONE);
            if (slot > s) {
                cf.reinforcementBits |= (short) (1 << s);
            }
        }
        for (int b = 0; b < mods.beeSwarms && slot < SimState.MAX_NPCS; b++) {
            slot = spawn(pack, grid, frame, state, slot, "bee_swarm", rng, playerPos, true, Coords.NONE);
        }
        if (mods.totemic) {
            for (int t = 0; t < 2 && slot < SimState.MAX_NPCS; t++) {
                int s = slot;
                slot = spawn(pack, grid, frame, state, slot, "healing_totem", rng, playerPos, false, Coords.NONE);
                if (slot > s) {
                    cf.totemPoolBits |= (short) (1 << s);
                }
            }
        }
        frame.finishOrder();
        state.rebuildOccupancy();
        state.resetAccumulators();
    }

    /**
     * The trio's fixed formation (capture section 1.2, 321/321): the berserker at B, drawn uniformly from the anchor
     * box with no regard for the player, the archer at B+(-1,-1), the seer at B+(+1,-1), and under Quartet a fourth
     * member at B+(0,-2). Returns berserker, archer, seer, quartet tiles, or null when no anchor fits the grid.
     */
    private static short[] warbandFormation(WaveData w, Grid grid, boolean quartet, Random rng) {
        for (int attempt = 0; attempt < 200; attempt++) {
            int x = w.trioAnchor.minX + rng.nextInt(w.trioAnchor.maxX - w.trioAnchor.minX + 1);
            int y = w.trioAnchor.minY + rng.nextInt(w.trioAnchor.maxY - w.trioAnchor.minY + 1);
            short[] tiles = {
                    Coords.pack(x, y),
                    Coords.pack(x + w.trioArcher.x, y + w.trioArcher.y),
                    Coords.pack(x + w.trioSeer.x, y + w.trioSeer.y),
                    Coords.pack(x + w.trioQuartet.x, y + w.trioQuartet.y)};
            boolean legal = true;
            for (int i = 0; i < (quartet ? 4 : 3) && legal; i++) {
                legal = !grid.illegalStatic(1, Coords.x(tiles[i]), Coords.y(tiles[i]));
            }
            if (legal) {
                return tiles;
            }
        }
        return null;
    }

    private static int trioTile(short[] trio, String key) {
        if (trio == null) {
            return Coords.NONE;
        }
        switch (key) {
            case "warband_berserker":
                return trio[0];
            case "warband_archer":
                return trio[1];
            case "warband_seer":
                return trio[2];
            default:
                return Coords.NONE;
        }
    }

    /** One of the spawn points at least spawnMinPlayerDistance from the player, uniformly, never one already taken;
     *  NONE when every eligible point is taken or blocked. */
    private static int spawnPoint(WaveData waves, Grid grid, NpcDef def, Random rng, int playerPos, boolean[] used) {
        int[][] points = waves.spawnPoints;
        int px = Coords.x(playerPos);
        int py = Coords.y(playerPos);
        int eligible = 0;
        for (int i = 0; i < points.length; i++) {
            if (pointOpen(waves, grid, def, points[i], px, py) && !used[i]) {
                eligible++;
            }
        }
        if (eligible == 0) {
            return Coords.NONE;
        }
        int pick = rng.nextInt(eligible);
        for (int i = 0; i < points.length; i++) {
            if (pointOpen(waves, grid, def, points[i], px, py) && !used[i] && pick-- == 0) {
                used[i] = true;
                return Coords.pack(points[i][0], points[i][1]);
            }
        }
        return Coords.NONE;
    }

    private static boolean pointOpen(WaveData waves, Grid grid, NpcDef def, int[] p, int px, int py) {
        return !grid.illegalStatic(def.size, p[0], p[1])
                && Math.max(Math.abs(p[0] - px), Math.abs(p[1] - py)) >= waves.spawnMinPlayerDistance;
    }

    /** Dynamic Duo's partner: the first legal anchor on the ring two tiles round the first. */
    private static int ringTwoTile(Grid grid, SimState state, NpcDef def, int pos) {
        int ax = Coords.x(pos);
        int ay = Coords.y(pos);
        int lo = -2 - def.size + 1;
        int hi = def.size + 1;
        for (int dy = lo; dy <= hi; dy++) {
            for (int dx = lo; dx <= hi; dx++) {
                if (dx != lo && dx != hi && dy != lo && dy != hi) {
                    continue;
                }
                int x = ax + dx;
                int y = ay + dy;
                if (x >= 0 && y >= 0 && x < Grid.SIZE && y < Grid.SIZE && !grid.illegalStatic(def.size, x, y)
                        && !overlapsAny(state, def, x, y)) {
                    return Coords.pack(x, y);
                }
            }
        }
        return Coords.NONE;
    }

    /** A second manticore copies the first one's orb order (capture section 12c, 61/62). */
    private static void shareFirstManticorePattern(Frame frame, SimState state, int slots) {
        int first = -1;
        for (int i = 0; i < slots; i++) {
            if (ColosseumPack.manticore(frame.def(i)) == null || (state.npcFlags[i] & SimState.FLAG_ALIVE) == 0) {
                continue;
            }
            if (first < 0) {
                first = state.npcLane[Lanes.PATTERN][i];
            } else {
                state.npcLane[Lanes.PATTERN][i] = (byte) first;
            }
        }
    }

    private static int spawn(ColosseumPack pack, Grid grid, Frame frame, SimState state, int slot,
                             String key, Random rng, int playerPos, boolean alive, int forcedPos) {
        if (slot >= SimState.MAX_NPCS) {
            return slot;
        }
        MechanicsData data = pack.data();
        int type = data.typeIndex(key);
        NpcDef def = data.npcs[type];
        Mods mods = ColosseumFrame.mods(frame);
        frame.npcTypeIdx[slot] = (byte) type;
        frame.npcMaxHp[slot] = (short) def.hp;
        frame.npcIndex[slot] = slot;
        if (def.smartPath) {
            frame.setSmartPath(slot);
        }
        if (ColosseumPack.minotaur(def) != null && mods.redFlag) {
            frame.setSmartPath(slot);
        }
        if (alive) {
            int pos = forcedPos >= 0 ? forcedPos : placement(pack.colosseum().waves, grid, state, def, rng, playerPos);
            state.npcPos[slot] = (short) pos;
            frame.npcHomePos[slot] = (short) pos;
            state.npcHp[slot] = (short) def.hp;
            state.npcFlags[slot] = SimState.FLAG_ALIVE;
            state.npcSpawnTick[slot] = 0;
            if (ColosseumPack.manticore(def) != null) {
                // Live play reads the volley pattern from the charge spot anim, so a synthetic world seeds a known
                // pattern.
                state.npcLane[Lanes.PATTERN][slot] = (byte) (rng.nextBoolean() ? 0 : 1);
            }
            if (ColosseumPack.javelin(def) != null) {
                state.npcLane[Lanes.JAVELIN_COUNT][slot] = 0;
            }
            if (def.behaviorImpl instanceof TotemBehavior) {
                state.npcLane[Lanes.TOTEM_BENEFICIARY][slot] = -1;
                state.npcLane[Lanes.TOTEM_HEAL][slot] = (byte) Math.max(1, mods.totemHealEvery);
            }
        }
        return slot + 1;
    }

    /** A random legal anchor inside the arena box at least 6 tiles from the player and other spawns, for anything
     *  without a recorded spawn rule (bee swarms) or no free point. */
    private static int placement(WaveData waves, Grid grid, SimState state, NpcDef def, Random rng, int playerPos) {
        WaveData.Box box = waves.arena;
        int px = Coords.x(playerPos);
        int py = Coords.y(playerPos);
        for (int attempt = 0; attempt < 400; attempt++) {
            int x = box.minX + rng.nextInt(box.maxX - box.minX + 1);
            int y = box.minY + rng.nextInt(box.maxY - box.minY + 1);
            if (grid.illegalStatic(def.size, x, y)) {
                continue;
            }
            if (Math.max(Math.abs(x - px), Math.abs(y - py)) < 6) {
                continue;
            }
            if (!overlapsAny(state, def, x, y)) {
                return x | y << 6;
            }
        }
        throw new IllegalStateException("could not place " + def.key);
    }

    private static boolean overlapsAny(SimState state, NpcDef def, int x, int y) {
        for (int i = 0; i < SimState.MAX_NPCS; i++) {
            if ((state.npcFlags[i] & SimState.FLAG_ALIVE) == 0 || state.npcPos[i] < 0) {
                continue;
            }
            int ox = Coords.x(state.npcPos[i]);
            int oy = Coords.y(state.npcPos[i]);
            int os = state.frame.npcTypeIdx[i] >= 0 ? state.frame.def(i).size : 1;
            if (!(x > ox + os + 1 || x + def.size + 1 < ox || y > oy + os + 1 || y + def.size + 1 < oy)) {
                return true;
            }
        }
        return false;
    }
}
