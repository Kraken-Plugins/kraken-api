package com.kraken.api.simulation.colosseum.plan;

import com.kraken.api.simulation.colosseum.ColoCoords;
import com.kraken.api.simulation.colosseum.ColoFrame;
import com.kraken.api.simulation.colosseum.ColoGrid;
import com.kraken.api.simulation.colosseum.ColoNpcType;
import com.kraken.api.simulation.colosseum.ColoState;
import com.kraken.api.simulation.colosseum.ColoTick;

/**
 * Per-tile threat analysis around the player: for every walkable tile in a radius, which
 * NPCs would have line of sight to it from where they stand now, and how much expected
 * damage per tick that exposure represents.
 *
 * <p>Used both to seed the planner's candidate destinations (nearest zero-exposure tiles,
 * lowest-exposure attack positions) and by the debug overlay as a heat map.</p>
 */
public final class DangerMap {
    private static final int FIELD_SIZE = ColoCoords.MAX_DIMENSION * ColoCoords.MAX_DIMENSION;

    private final byte[] losCount = new byte[FIELD_SIZE];
    /** Expected damage per tick x100 if standing on the tile unprotected. */
    private final short[] expectedDamagePerTick = new short[FIELD_SIZE];
    private final boolean[] computed = new boolean[FIELD_SIZE];
    private int radius;
    private short center = ColoCoords.NONE;

    /**
     * Recomputes the map around the player.
     *
     * @param state state to analyse.
     * @param radius tile radius around the player to cover.
     */
    public void compute(ColoState state, int radius) {
        this.radius = radius;
        this.center = state.playerPos();
        ColoFrame frame = state.frame();
        ColoGrid grid = frame.getGrid();
        int px = ColoCoords.x(center);
        int py = ColoCoords.y(center);
        double factor = frame.getLoadout().getNpcExpectedDamageFactor();

        java.util.Arrays.fill(computed, false);
        for (int y = Math.max(0, py - radius); y <= Math.min(grid.height() - 1, py + radius); y++) {
            for (int x = Math.max(0, px - radius); x <= Math.min(grid.width() - 1, px + radius); x++) {
                int idx = (y << 6) | x;
                computed[idx] = true;
                losCount[idx] = 0;
                expectedDamagePerTick[idx] = 0;
                if (grid.isMoveBlocked(x, y)) {
                    continue;
                }
                short tile = ColoCoords.pack(x, y);
                int count = 0;
                double damage = 0;
                for (int slot = 0; slot < frame.getNpcSlotCount(); slot++) {
                    if (!state.npcActive(slot)) {
                        continue;
                    }
                    if (!ColoTick.npcHasLosToTile(state, slot, tile)) {
                        continue;
                    }
                    count++;
                    ColoNpcType type = frame.type(slot);
                    int maxHit = type.getMaxHit();
                    if (type == ColoNpcType.JAGUAR_WARRIOR) {
                        maxHit *= ColoTick.JAGUAR_HITS_PER_ATTACK;
                    }
                    damage += maxHit * factor / Math.max(1, type.getAttackSpeedTicks());
                }
                losCount[idx] = (byte) Math.min(127, count);
                expectedDamagePerTick[idx] = (short) Math.min(Short.MAX_VALUE, Math.round(damage * 100));
            }
        }
    }

    /** @return radius covered by the last {@link #compute}. */
    public int radius() {
        return radius;
    }

    /** @return packed player position the map was computed around. */
    public short center() {
        return center;
    }

    /**
     * @param tile packed tile.
     * @return true when the tile was inside the last computed radius.
     */
    public boolean covers(short tile) {
        return ColoCoords.isPresent(tile) && computed[tile & 0xFFF];
    }

    /**
     * @param tile packed tile.
     * @return NPCs with line of sight to the tile, or 0 when uncovered.
     */
    public int losCount(short tile) {
        return covers(tile) ? losCount[tile & 0xFFF] : 0;
    }

    /**
     * @param tile packed tile.
     * @return expected unprotected damage per tick x100 while standing on the tile.
     */
    public int expectedDamagePerTickX100(short tile) {
        return covers(tile) ? expectedDamagePerTick[tile & 0xFFF] : 0;
    }
}
