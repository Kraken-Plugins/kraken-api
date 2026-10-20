package com.kraken.api.core.script.breakhandler;


import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.time.Duration;
import java.util.List;
import java.util.Random;

/**
 * Timing and condition settings for a {@link BreakManager}: how long a run lasts before a break, how long
 * the break lasts, and whether the client logs out during it.
 */
@Getter
@Builder
public class BreakProfile {
    private final String name;

    private final Duration minRuntime;
    private final Duration maxRuntime;
    private final Duration minBreakDuration;
    private final Duration maxBreakDuration;

    @Singular(value = "addBreakCondition")
    private final List<BreakCondition> breakConditions;

    @Builder.Default
    private final boolean logoutDuringBreak = true;

    @Builder.Default
    private final boolean randomizeTimings = true;

    private static final Random random = new Random();

    /**
     * Calculates the next runtime duration based on profile settings.
     * @return The duration of the next run
     */
    public Duration getNextRunDuration() {
        if (!randomizeTimings) {
            return maxRuntime;
        }
        long minMillis = minRuntime.toMillis();
        long maxMillis = maxRuntime.toMillis();
        long randomMillis = minMillis + (long) (random.nextDouble() * (maxMillis - minMillis));
        return Duration.ofMillis(randomMillis);
    }

    /**
     * Calculates the next break duration based on profile settings.
     * @return The duration of the next break
     */
    public Duration getNextBreakDuration() {
        if (!randomizeTimings) {
            return maxBreakDuration;
        }
        long minMillis = minBreakDuration.toMillis();
        long maxMillis = maxBreakDuration.toMillis();
        long randomMillis = minMillis + (long) (random.nextDouble() * (maxMillis - minMillis));
        return Duration.ofMillis(randomMillis);
    }

    /**
     * Creates a profile with runs of 45 to 90 minutes and breaks of 5 to 15 minutes, logging out during breaks.
     * @return A new profile named Conservative.
     */
    public static BreakProfile createConservative() {
        return BreakProfile.builder()
                .name("Conservative")
                .minRuntime(Duration.ofMinutes(45))
                .maxRuntime(Duration.ofMinutes(90))
                .minBreakDuration(Duration.ofMinutes(5))
                .maxBreakDuration(Duration.ofMinutes(15))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .build();
    }

    /**
     * Creates a profile with runs of 2 to 4 hours and breaks of 2 to 5 minutes, logging out during breaks.
     * @return A new profile named Aggressive.
     */
    public static BreakProfile createAggressive() {
        return BreakProfile.builder()
                .name("Aggressive")
                .minRuntime(Duration.ofHours(2))
                .maxRuntime(Duration.ofHours(4))
                .minBreakDuration(Duration.ofMinutes(2))
                .maxBreakDuration(Duration.ofMinutes(5))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .build();
    }

    /**
     * Creates a profile with runs of 60 to 120 minutes and breaks of 3 to 10 minutes, logging out during breaks.
     * @return A new profile named Balanced.
     */
    public static BreakProfile createBalanced() {
        return BreakProfile.builder()
                .name("Balanced")
                .minRuntime(Duration.ofMinutes(60))
                .maxRuntime(Duration.ofMinutes(120))
                .minBreakDuration(Duration.ofMinutes(3))
                .maxBreakDuration(Duration.ofMinutes(10))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .build();
    }
}