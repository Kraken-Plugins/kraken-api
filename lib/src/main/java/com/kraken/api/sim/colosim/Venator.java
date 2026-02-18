package com.kraken.api.sim.colosim;

import java.util.ArrayList;
import java.util.List;

public class Venator {
    private static final int RANGE = 2;

    private static boolean isInRange(int x, int y, int x2, int y2) {
        int dxAbs = Math.abs(x - x2);
        int dyAbs = Math.abs(y - y2);
        return dxAbs <= RANGE && dyAbs <= RANGE;
    }

    private static boolean anyInRange(List<int[]> from, List<int[]> to) {
        for (int[] f : from) {
            boolean allInRange = true;
            for (int[] t : to) {
                if (!isInRange(f[0], f[1], t[0], t[1])) {
                    allInRange = false;
                    break;
                }
            }
            if (allInRange) {
                return true;
            }
        }
        return false;
    }

    public static boolean canBounce(int x, int y, int size, int x2, int y2, int size2) {
        List<int[]> scanTiles = getScanTiles(x, y, size);
        int[] center = scanTiles.get(2);

        List<int[]> scanTiles2 = getScanTiles(x2, y2, size2);
        int[] sw2 = scanTiles2.get(0);
        int[] centerSw2 = scanTiles2.get(1);
        int[] center2 = scanTiles2.get(2);

        switch (size) {
            case 1:
            case 3:
            case 5:
                List<int[]> from = new ArrayList<>();
                from.add(center);
                List<int[]> to = new ArrayList<>();
                to.add(sw2);
                to.add(center2);
                return anyInRange(from, to);
            case 2:
                List<int[]> all2x2 = getAllTiles(x, y, size);
                if (size2 <= 3) {
                    List<int[]> to2 = new ArrayList<>();
                    to2.add(center2);
                    return anyInRange(all2x2, to2);
                } else if (size2 <= 5) {
                    List<int[]> to2 = new ArrayList<>();
                    to2.add(centerSw2);
                    to2.add(center2);
                    return anyInRange(all2x2, to2);
                }
                break;
            case 4:
                List<int[]> middle2x2 = getAllTiles(x + 1, y - 1, 2);
                List<int[]> to4 = new ArrayList<>();
                to4.add(sw2);
                to4.add(centerSw2);
                return anyInRange(middle2x2, to4);
        }
        throw new RuntimeException("Unsupported bounce check (sizes " + size + " vs " + size2 + ")");
    }

    private static List<int[]> getScanTiles(int x, int y, int size) {
        List<int[]> tiles = new ArrayList<>();
        switch (size) {
            case 1:
            case 2:
                tiles.add(new int[]{x, y});
                tiles.add(new int[]{x, y});
                tiles.add(new int[]{x, y});
                return tiles;
            case 3:
                tiles.add(new int[]{x, y});
                tiles.add(new int[]{x, y});
                tiles.add(new int[]{x + 1, y - 1});
                return tiles;
            case 4:
            case 5:
                tiles.add(new int[]{x, y});
                tiles.add(new int[]{x + 1, y - 1});
                tiles.add(new int[]{x + 2, y - 2});
                return tiles;
            default:
                throw new RuntimeException("Unsupported NPC size " + size);
        }
    }

    private static List<int[]> getAllTiles(int x, int y, int size) {
        List<int[]> res = new ArrayList<>();
        for (int dx = 0; dx < size; ++dx) {
            for (int dy = 0; dy < size; ++dy) {
                res.add(new int[]{x + dx, y - dy});
            }
        }
        return res;
    }
}
