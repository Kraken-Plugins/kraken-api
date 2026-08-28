package unit.com.kraken.api.simulation.colosseum;

import colosim.Simulation;
import plugins.colosseum.simulation.Constants;
import plugins.colosseum.simulation.Coords;
import plugins.colosseum.simulation.Frame;
import plugins.colosseum.simulation.Grid;
import plugins.colosseum.simulation.NpcType;
import plugins.colosseum.simulation.State;
import plugins.colosseum.simulation.Tick;
import plugins.colosseum.simulation.LoadoutConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Test fixtures for the colosseum engine.
 *
 * <p>The arena geometry is sourced from the validated community simulator
 * ({@link Simulation}) and flipped from its screen-space (y-down) coordinates into the
 * engine's world-space (y-up) coordinates, so parity tests compare the two simulations on
 * identical geometry: {@code yUp = 33 - yDown}.</p>
 */
public final class TestArenas {
    /** Height of the colosim arena grid, used for the y-axis flip. */
    public static final int MAP_MAX_Y = Simulation.MAP_HEIGHT - 1;

    private TestArenas() {
    }

    /**
     * @return the colosseum arena grid translated from the community simulator's geometry.
     */
    public static Grid colosimArena() {
        Simulation source = new Simulation();
        List<Short> blocked = new ArrayList<>();
        int[][][] ranges = source.getBlockedTileRanges();
        for (int yDown = 0; yDown < ranges.length; yDown++) {
            for (int[] range : ranges[yDown]) {
                for (int x = range[0]; x < range[1] && x < Simulation.MAP_WIDTH; x++) {
                    blocked.add(Coords.pack(x, MAP_MAX_Y - yDown));
                }
            }
        }
        for (int[] pillar : source.getPillars()) {
            // Colosim pillar anchors are top-left-ish in screen space with a 3x3 footprint
            // spanning rows yDown-2..yDown; flipped, the south-west anchor row is
            // MAP_MAX_Y - yDown.
            int anchorY = MAP_MAX_Y - pillar[1];
            for (int dx = 0; dx < 3; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    blocked.add(Coords.pack(pillar[0] + dx, anchorY + dy));
                }
            }
        }
        short[] tiles = new short[blocked.size()];
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = blocked.get(i);
        }
        return Grid.synthetic(Simulation.MAP_WIDTH, Simulation.MAP_HEIGHT, 1808, 3088, 0, tiles);
    }

    /**
     * @return an empty (obstacle-free) grid for isolated mechanics tests.
     */
    public static Grid openArena() {
        return Grid.synthetic(34, 34, 1808, 3088, 0, new short[0]);
    }

    /**
     * Converts colosim screen-space coordinates to a packed engine position.
     *
     * @param xDown colosim x.
     * @param yDown colosim y (screen space, larger = further south... in colosim terms).
     * @return packed y-up position.
     */
    public static short up(int xDown, int yDown) {
        return Coords.pack(xDown, MAP_MAX_Y - yDown);
    }

    /**
     * Builds a frame with one slot per type in order (RuneLite indices 0..n-1).
     *
     * @param grid grid.
     * @param waveStartGates wave-start gating flag.
     * @param types slot types.
     * @return frame.
     */
    public static Frame frame(Grid grid, boolean waveStartGates, NpcType... types) {
        int[] indices = new int[types.length];
        short[] maxHp = new short[types.length];
        for (int i = 0; i < types.length; i++) {
            indices[i] = i;
            maxHp[i] = (short) types[i].getMaxHitpoints();
        }
        return new Frame(
                grid,
                LoadoutConfig.defaults(),
                types,
                indices,
                maxHp,
                99,
                99,
                waveStartGates,
                Constants.DEFAULT_WARBAND_CYCLE_PHASE,
                0
        );
    }

    /**
     * Builds a fresh state with the player at a position and every NPC slot activated at
     * full health with a ready attack timer.
     *
     * @param frame frame.
     * @param playerPos packed player position.
     * @param npcPositions per-slot packed anchors.
     * @return state.
     */
    public static State state(Frame frame, short playerPos, short... npcPositions) {
        State state = new State();
        state.reset(frame);
        state.setPlayer(playerPos, 99, 99, 10_000, 100, State.OVERHEAD_NONE, 0);
        for (int i = 0; i < npcPositions.length; i++) {
            state.activateNpc(i, npcPositions[i], frame.getNpcMaxHp()[i], 0);
        }
        return state;
    }

    /**
     * Attack-launch recorder for asserting attack ticks.
     */
    public static final class AttackRecorder implements Tick.AttackListener {
        /** Recorded events as {slot, styleCode, tick, special ? 1 : 0}. */
        public final List<int[]> events = new ArrayList<>();

        @Override
        public void onAttack(int slot, int styleCode, int tick, boolean special) {
            events.add(new int[]{slot, styleCode, tick, special ? 1 : 0});
        }

        /**
         * @param slot npc slot.
         * @return engine ticks on which the slot launched attacks.
         */
        public List<Integer> attackTicks(int slot) {
            List<Integer> ticks = new ArrayList<>();
            for (int[] event : events) {
                if (event[0] == slot) {
                    ticks.add(event[2]);
                }
            }
            return ticks;
        }

        /**
         * @param slot npc slot.
         * @return style codes launched by the slot in order.
         */
        public List<Integer> styles(int slot) {
            List<Integer> styles = new ArrayList<>();
            for (int[] event : events) {
                if (event[0] == slot) {
                    styles.add(event[1]);
                }
            }
            return styles;
        }
    }
}
