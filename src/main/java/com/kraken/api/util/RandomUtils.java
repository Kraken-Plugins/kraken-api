package com.kraken.api.util;

import java.util.Random;
import java.util.Set;

public class RandomUtils {

    public static final Random random = new Random();

    public static int randomIntBetween(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

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
