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
     */
    public static int clamp(int value, int minInclusive, int maxInclusive) {
        return Math.max(minInclusive, Math.min(maxInclusive, value));
    }

    /**
     * @return Chebyshev distance between two tiles.
     */
    public static int chebyshevDistance(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }

    /**
     * Returns whether two square footprints overlap.
     */
    public static boolean overlaps(int aX, int aY, int aSize, int bX, int bY, int bSize) {
        int aMaxX = aX + aSize - 1;
        int aMaxY = aY + aSize - 1;
        int bMaxX = bX + bSize - 1;
        int bMaxY = bY + bSize - 1;

        return !(aMaxX < bX || bMaxX < aX || aMaxY < bY || bMaxY < aY);
    }
}
