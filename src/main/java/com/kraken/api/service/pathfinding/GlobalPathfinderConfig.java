package com.kraken.api.service.pathfinding;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder(toBuilder = true)
public class GlobalPathfinderConfig {

    @Builder.Default
    private final long calculationCutoffMillis = 3000L;

    @Builder.Default
    private final int sparsePathWaypointDistance = 15;

    @Builder.Default
    private final boolean avoidWilderness = false;

    @Builder.Default
    private final boolean includeBankPath = false;

    @Builder.Default
    private final int currencyThreshold = 100000;

    @Builder.Default
    private final boolean useAgilityShortcuts = true;

    @Builder.Default
    private final boolean useGrappleShortcuts = true;

    @Builder.Default
    private final boolean useBoats = true;

    @Builder.Default
    private final boolean useCanoes = true;

    @Builder.Default
    private final boolean useCharterShips = true;

    @Builder.Default
    private final boolean useShips = true;

    @Builder.Default
    private final boolean useFairyRings = true;

    @Builder.Default
    private final boolean useGnomeGliders = true;

    @Builder.Default
    private final boolean useHotAirBalloons = true;

    @Builder.Default
    private final boolean useMagicCarpets = true;

    @Builder.Default
    private final boolean useMagicMushtrees = true;

    @Builder.Default
    private final boolean useMinecarts = true;

    @Builder.Default
    private final boolean useQuetzals = true;

    @Builder.Default
    private final boolean useSeasonalTransports = true;

    @Builder.Default
    private final boolean useSpiritTrees = true;

    @Builder.Default
    private final boolean useTeleportationItems = true;

    @Builder.Default
    private final boolean useTeleportationBoxes = true;

    @Builder.Default
    private final boolean useTeleportationLevers = true;

    @Builder.Default
    private final boolean useTeleportationMinigames = true;

    @Builder.Default
    private final boolean useTeleportationPortals = true;

    @Builder.Default
    private final boolean useTeleportationPortalsPoh = true;

    @Builder.Default
    private final boolean useTeleportationSpells = true;

    @Builder.Default
    private final boolean useWildernessObelisks = true;
}
