package com.kraken.api.simulation.colosseum;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kraken.api.simulation.data.MechanicsData;

import java.util.Map;

/**
 * The Colosseum's own tables beside the core's: modifiers, waves, manticore volley patterns and the Solarflare
 * rings.
 */
public final class ColosseumData {
    /**
     * Solarflare: four flares, one per pillar, each circling the 5x5 square round its pillar clockwise from the
     * corner tile nearest the arena centre. All four start on wave tick 0 and keep the same timing, so one ring index
     * drives them: {@code solarRings[pillar][index]} is a packed tile, and every multiple of {@link #SOLAR_SIDE} is a
     * corner.
     */
    public static final int SOLAR_RING = 16;
    public static final int SOLAR_SIDE = 4;

    public final ModifierData modifiers;
    public final WaveData waves;
    public final short[][] solarRings;
    /**
     * Manticore volley patterns in colosim order: name to the three orb styles fired on consecutive ticks (0 ranged,
     * 1 magic, 2 melee). Pattern id = declaration order; melee is always last except under Mantimayhem III.
     */
    public final String[] patternNames;
    public final int[][] patternStyles;

    ColosseumData(Class<?> owner, String root, JsonObject npcFile) {
        modifiers = MechanicsData.read(owner, root + "/modifiers.json", ModifierData.class);
        waves = MechanicsData.read(owner, root + "/waves.json", WaveData.class);
        JsonObject patterns = npcFile.getAsJsonObject("manticorePatterns");
        patternNames = new String[patterns.size()];
        patternStyles = new int[patterns.size()][];
        int i = 0;
        for (Map.Entry<String, JsonElement> e : patterns.entrySet()) {
            JsonArray styles = e.getValue().getAsJsonArray();
            patternNames[i] = e.getKey();
            patternStyles[i] = new int[styles.size()];
            for (int k = 0; k < styles.size(); k++) {
                patternStyles[i][k] = styles.get(k).getAsInt();
            }
            i++;
        }
        solarRings = buildSolarRings(waves.pillars);
    }

    /** The clockwise 16 tile ring round each 3x3 pillar, rotated to start on the corner nearest the centre of the
     *  four pillars. */
    private static short[][] buildSolarRings(WaveData.Gate[] pillars) {
        int cx2 = 0;
        int cy2 = 0;
        for (WaveData.Gate p : pillars) {
            cx2 += 2 * p.x + 2;
            cy2 += 2 * p.y + 2;
        }
        short[][] rings = new short[pillars.length][SOLAR_RING];
        for (int k = 0; k < pillars.length; k++) {
            int lo = pillars[k].x - 1;
            int hi = pillars[k].x + 3;
            int bottom = pillars[k].y - 1;
            int top = pillars[k].y + 3;
            short[] clockwiseFromNe = new short[SOLAR_RING];
            int i = 0;
            for (int y = top; y > bottom; y--) {
                clockwiseFromNe[i++] = (short) (hi | y << 6);
            }
            for (int x = hi; x > lo; x--) {
                clockwiseFromNe[i++] = (short) (x | bottom << 6);
            }
            for (int y = bottom; y < top; y++) {
                clockwiseFromNe[i++] = (short) (lo | y << 6);
            }
            for (int x = lo; x < hi; x++) {
                clockwiseFromNe[i++] = (short) (x | top << 6);
            }
            boolean east = (2 * pillars[k].x + 2) * pillars.length < cx2;
            boolean north = (2 * pillars[k].y + 2) * pillars.length < cy2;
            int startX = east ? hi : lo;
            int startY = north ? top : bottom;
            int start = 0;
            for (int j = 0; j < SOLAR_RING; j++) {
                if (clockwiseFromNe[j] == (short) (startX | startY << 6)) {
                    start = j;
                }
            }
            for (int j = 0; j < SOLAR_RING; j++) {
                rings[k][j] = clockwiseFromNe[(start + j) % SOLAR_RING];
            }
        }
        return rings;
    }

    public int patternId(String name) {
        for (int i = 0; i < patternNames.length; i++) {
            if (patternNames[i].equals(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("unknown manticore pattern " + name);
    }

    /** Ring index of a packed tile on any flare's ring, or -1 when it is on none. */
    public int solarOrbitIndex(int packedTile) {
        for (short[] ring : solarRings) {
            for (int i = 0; i < SOLAR_RING; i++) {
                if (ring[i] == packedTile) {
                    return i;
                }
            }
        }
        return -1;
    }
}
