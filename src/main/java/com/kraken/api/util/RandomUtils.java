package com.kraken.api.util;

import java.util.Random;
import java.util.Set;

/**
 * Random number helpers that share one {@link Random} source.
 */
public class RandomUtils {

    /** The shared random source used by the helpers in this class. */
    public static final Random random = new Random();

    /**
     * Returns a random integer between two bounds, both inclusive.
     * @param min The lowest value that can be returned.
     * @param max The highest value that can be returned.
     * @return A random integer from min to max inclusive.
     */
    public static int randomIntBetween(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Returns a random element of a set.
     * @param set The set to pick from. Must not be empty.
     * @return A randomly chosen element of the set.
     * @throws IllegalArgumentException If the set is empty.
     */
    public static int randomFromSet(Set<Integer> set) {
        if (set.isEmpty()) {
            throw new IllegalArgumentException("Set cannot be empty");
        }

        int randomIndex = random.nextInt(set.size());
        int currentIndex = 0;

        for (Integer value : set) {
            if (currentIndex == randomIndex) {
                return value;
            }
            currentIndex++;
        }

        // This should never be reached, but just in case
        throw new RuntimeException("Failed to select random element from set");
    }

    /**
     * Produces a randomised idle-tick threshold in the range [1, 13000].
     *
     * <p>The magnitude of a gaussian draw is used so the result is a half-normal with scale 8000:
     * roughly a 5400-tick median, a spread across the whole band, and about a tenth of draws sitting at
     * the 13000 ceiling. The ceiling stays clear of the client's own idle-logout threshold.</p>
     *
     * @return a threshold in game client idle ticks, between 1 and 13000 inclusive.
     */
    public static long randomDelay() {
        return Math.max(1, Math.min(13000, Math.round(Math.abs(random.nextGaussian()) * 8000)));
    }
}
