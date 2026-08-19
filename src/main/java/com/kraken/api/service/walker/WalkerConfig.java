package com.kraken.api.service.walker;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import lombok.Builder;
import lombok.Getter;

/**
 * Tuning for a walk.
 *
 * <p>The pathfinder config is carried here rather than passed separately because the two have to
 * agree: turning off a transport type stops the planner proposing it, which is how a transport the
 * walker cannot operate is kept out of a route in the first place.</p>
 */
@Getter
@Builder(toBuilder = true)
public class WalkerConfig {

    /** How close to the destination counts as arrived. Cannot usefully go below 2. */
    @Builder.Default
    private final int tolerance = 3;

    /** Give up after this many plan and walk rounds. */
    @Builder.Default
    private final int maxRounds = 25;

    /** Overall budget for a single walk. */
    @Builder.Default
    private final long timeoutMillis = 180_000;

    /** Net tiles that must be gained in a round for it to count as progress. */
    @Builder.Default
    private final int minProgressTiles = 3;

    /** Consecutive rounds without progress before declaring the player stuck. */
    @Builder.Default
    private final int maxStalledRounds = 3;

    /** How close to a transport's entrance the player must be before it is operated. */
    @Builder.Default
    private final int transportFireRadius = 2;

    /**
     * Planning options, including which transport types may be used.
     *
     * <p>Canoes and minigame teleports default off because the walker cannot operate them yet.
     * Turning them off here keeps the planner from proposing a route that would then fail with
     * {@code TRANSPORT_UNSUPPORTED}. Plan-only callers can still enable them on a
     * {@link GlobalPathfinderConfig} of their own.</p>
     */
    @Builder.Default
    private final GlobalPathfinderConfig pathfinderConfig = GlobalPathfinderConfig.builder()
            .useCanoes(false)
            .useTeleportationMinigames(false)
            .build();
}
