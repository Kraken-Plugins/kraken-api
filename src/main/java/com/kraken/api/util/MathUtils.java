package com.kraken.api.util;

import java.math.BigInteger;

public class MathUtils {

    /**
     * Computes the inverse mod given a value and bits to shift left by
     * @param val BigInteger value
     * @param bits int number of bits to shift left
     * @return Mod inverse of the value passed.
     */
    public static BigInteger modInverse(BigInteger val, int bits) {
        try {
            BigInteger shift = BigInteger.ONE.shiftLeft(bits);
            return val.modInverse(shift);
        } catch (ArithmeticException e) {
            return val;
        }
    }

    /**
     * Computes the inverse mod given a value and bits to shift left by
     * @param val BigInteger value
     * @return Mod inverse of the value passed.
     */
    public static long modInverse(long val) {
        return modInverse(BigInteger.valueOf(val), 64).longValue();
    }

    /**
     * Clamps a value to an inclusive range.
     * @param value The value to clamp
     * @param minInclusive The min value to (inclusive of the value itself) i.e. 10 would include 10 itself.
     * @param maxInclusive The max value (inclusive of the value itself)
     * @return The clamped value between the min and the max
     */
    public static int clamp(int value, int minInclusive, int maxInclusive) {
        return Math.max(minInclusive, Math.min(maxInclusive, value));
    }

    /**
     * Finds the chebyshev distance between two tiles. This is defined as
     * the distance between two vectors as the maximum absolute difference along any single coordinate dimension.
     * @param ax The x coordinate of the first tile.
     * @param ay The y coordinate of the first tile.
     * @param bx The x coordinate of the second tile.
     * @param by The y coordinate of the second tile.
     * @return Chebyshev distance between two tiles.
     */
    public static int chebyshevDistance(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }

    /**
     * Returns whether two square footprints overlap.
     * @param aX The x coordinate of the SW first tile
     * @param aY The y coordinate of the SW first tile
     * @param aSize The size of the set of tiles
     * @param bX The x coordinate of the SW second tile
     * @param bY The y coordinate of the SW second tile
     * @param bSize The size of the second set of tiles
     * @return True if any of the tiles overlap and false otherwise.
     */
    public static boolean overlaps(int aX, int aY, int aSize, int bX, int bY, int bSize) {
        int aMaxX = aX + aSize - 1;
        int aMaxY = aY + aSize - 1;
        int bMaxX = bX + bSize - 1;
        int bMaxY = bY + bSize - 1;

        return !(aMaxX < bX || bMaxX < aX || aMaxY < bY || bMaxY < aY);
    }
}
