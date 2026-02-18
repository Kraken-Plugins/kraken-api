package com.kraken.api.sim.colosim;

import java.util.ArrayList;
import java.util.List;

public final class Venator {
    private static final int RANGE = 2;

    private Venator() {
    }

    private static boolean isInRange(Tile from, Tile to) {
        int dxAbs = Math.abs(from.x - to.x);
        int dyAbs = Math.abs(from.y - to.y);
        return dxAbs <= RANGE && dyAbs <= RANGE;
    }

    private static boolean anyInRange(List<Tile> from, List<Tile> to) {
        for (Tile f : from) {
            boolean all = true;
            for (Tile t : to) {
                if (!isInRange(f, t)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return true;
            }
        }
        return false;
    }

    public static boolean canBounce(int x, int y, int size, int x2, int y2, int size2) {
        Tile[] scan1 = getScanTiles(x, y, size);
        Tile[] scan2 = getScanTiles(x2, y2, size2);
        Tile center = scan1[2];
        Tile sw2 = scan2[0];
        Tile centerSw2 = scan2[1];
        Tile center2 = scan2[2];

        switch (size) {
            case 1:
            case 3:
            case 5:
                return anyInRange(single(center), pair(sw2, center2));
            case 2:
                List<Tile> all2x2 = getAllTiles(x, y, size);
                if (size2 <= 3) {
                    return anyInRange(all2x2, single(center2));
                } else if (size2 <= 5) {
                    return anyInRange(all2x2, pair(centerSw2, center2));
                }
                break;
            case 4:
                List<Tile> middle2x2 = getAllTiles(x + 1, y - 1, 2);
                return anyInRange(middle2x2, pair(sw2, centerSw2));
            default:
                break;
        }
        throw new IllegalArgumentException("Unsupported bounce check (sizes " + size + " vs " + size2 + ")");
    }

    private static Tile[] getScanTiles(int x, int y, int size) {
        switch (size) {
            case 1:
            case 2:
                return new Tile[]{new Tile(x, y), new Tile(x, y), new Tile(x, y)};
            case 3:
                return new Tile[]{new Tile(x, y), new Tile(x, y), new Tile(x + 1, y - 1)};
            case 4:
            case 5:
                return new Tile[]{new Tile(x, y), new Tile(x + 1, y - 1), new Tile(x + 2, y - 2)};
            default:
                throw new IllegalArgumentException("Unsupported NPC size " + size);
        }
    }

    private static List<Tile> getAllTiles(int x, int y, int size) {
        ArrayList<Tile> out = new ArrayList<Tile>();
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                out.add(new Tile(x + dx, y - dy));
            }
        }
        return out;
    }

    private static List<Tile> single(Tile t) {
        ArrayList<Tile> out = new ArrayList<Tile>(1);
        out.add(t);
        return out;
    }

    private static List<Tile> pair(Tile a, Tile b) {
        ArrayList<Tile> out = new ArrayList<Tile>(2);
        out.add(a);
        out.add(b);
        return out;
    }
}
