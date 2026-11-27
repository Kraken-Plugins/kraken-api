package com.kraken.api.simulation.colosseum;

/**
 * Wave spawn and reinforcement composition from colosseum/waves.json, plus the arena's fixed tiles. Every coordinate
 * is region local (template world tile minus 1792, 3072), the frame live capture and the traces use. Live spawns
 * always come from observation; these tables feed the reinforcement clock and the synthetic boards.
 */
public final class WaveData {
    public Wave[] waves;
    public String quartetExtra;
    public Gate northGate;
    public Gate southGate;
    /** Reinforcements spawn in the housekeeping of the tick before this wave tick (they appear on tick 66). */
    public int reinforcementTick = 67;
    /** Reinforcements come from the north gate when the player's y at the start of the reinforcement tick is at
     *  least this, else from the south gate. */
    public int northWhenPlayerYAtLeast = 34;
    /** Spawn tile x for successive reinforcements, distinct for each. */
    public int[] reinforcementXs = {31, 33, 32};
    /** The playable floor's bounding box: an unknown NPC inside it stops the plugin. */
    public Box arena;
    /** The four 3x3 pillars by SW tile. */
    public Gate[] pillars;
    /** Wave start spawn points (SW tiles) for every non warband monster, each used once. */
    public int[][] spawnPoints;
    /** A spawn point is drawn only this far (Chebyshev) from the player's tile or more. */
    public int spawnMinPlayerDistance = 5;
    /** The berserker's tile is drawn from this box; the others sit at fixed offsets. */
    public Box trioAnchor;
    public Gate trioArcher;
    public Gate trioSeer;
    public Gate trioQuartet;

    public static final class Wave {
        public int wave;
        public String[] spawns;
        public String[] reinforcements;
    }

    public static final class Gate {
        public int x;
        public int y;
    }

    public static final class Box {
        public int minX;
        public int minY;
        public int maxX;
        public int maxY;

        public boolean contains(int x, int y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
    }

    /** Composition for a wave number 1 to 11, or null when out of range (wave 12 = disarm). */
    public Wave forWave(int number) {
        for (Wave w : waves) {
            if (w.wave == number) {
                return w;
            }
        }
        return null;
    }
}
