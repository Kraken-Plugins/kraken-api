package com.kraken.api.service.pathfinding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import shortestpath.transport.TransportType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Configuration for the global {@link GlobalPathfinder} based on the RuneLite shortest-path plugin data.
 * This config is transport-focused and intentionally lightweight; it toggles which transport
 * types are eligible during pathfinding and exposes a calculation cutoff to bound search time.
 */
@Getter
@ToString
public class GlobalPathfinderBuilder {
    private final boolean avoidWilderness;
    private final long calculationCutoffMillis;
    private final Set<TransportType> enabledTransportTypes;

    private GlobalPathfinderBuilder(Builder builder) {
        this.avoidWilderness = builder.avoidWilderness;
        this.calculationCutoffMillis = builder.calculationCutoffMillis;
        this.enabledTransportTypes = builder.enabledTransportTypes.isEmpty()
            ? EnumSet.noneOf(TransportType.class)
            : EnumSet.copyOf(builder.enabledTransportTypes);
    }

    /**
     * Returns a new builder pre-populated with default settings.
     *
     * @return builder with default configuration values.
     */
    public static Builder builder() {
        return new Builder().defaults();
    }

    /**
     * Convenience alias to match the requested fluent style.
     *
     * @return builder with default configuration values.
     */
    public static Builder Builder() {
        return builder();
    }

    /**
     * Returns a new builder initialized from an existing config.
     *
     * @param config existing config to copy.
     * @return builder with values copied from {@code config}.
     */
    public static Builder from(GlobalPathfinderBuilder config) {
        return new Builder(config);
    }

    /**
     * Determines whether the given transport type is enabled in this config.
     *
     * @param type transport type.
     * @return true if the type is enabled.
     */
    public boolean isTransportEnabled(TransportType type) {
        return enabledTransportTypes.contains(type);
    }


    /**
     * Fluent builder for {@link GlobalPathfinderBuilder}.
     */
    @NoArgsConstructor
    public static class Builder {
        private boolean avoidWilderness;
        private long calculationCutoffMillis;
        private final EnumSet<TransportType> enabledTransportTypes = EnumSet.noneOf(TransportType.class);

        private Builder(GlobalPathfinderBuilder config) {
            this.avoidWilderness = config.avoidWilderness;
            this.calculationCutoffMillis = config.calculationCutoffMillis;
            this.enabledTransportTypes.addAll(config.enabledTransportTypes);
        }

        /**
         * Resets this builder to default values: all transport types enabled,
         * wilderness avoidance disabled, and a 2000ms cutoff.
         *
         * @return this builder.
         */
        public Builder defaults() {
            avoidWilderness = false;
            calculationCutoffMillis = 2000L;
            enabledTransportTypes.clear();
            enabledTransportTypes.addAll(EnumSet.allOf(TransportType.class));
            return this;
        }

        /**
         * Enables or disables wilderness avoidance.
         *
         * @param avoid true to avoid wilderness when possible.
         * @return this builder.
         */
        public Builder avoidWilderness(boolean avoid) {
            this.avoidWilderness = avoid;
            return this;
        }

        /**
         * Sets the maximum search duration in milliseconds.
         *
         * @param millis cutoff duration in milliseconds.
         * @return this builder.
         */
        public Builder calculationCutoffMillis(long millis) {
            this.calculationCutoffMillis = Math.max(0L, millis);
            return this;
        }

        /**
         * Enables or disables all transport types at once.
         *
         * @param enabled true to enable all types, false to disable all types.
         * @return this builder.
         */
        public Builder enableAllTransports(boolean enabled) {
            enabledTransportTypes.clear();
            if (enabled) {
                enabledTransportTypes.addAll(EnumSet.allOf(TransportType.class));
            }
            return this;
        }

        /**
         * Enables or disables a specific transport type.
         *
         * @param type transport type.
         * @param enabled true to enable, false to disable.
         * @return this builder.
         */
        public Builder enableTransport(TransportType type, boolean enabled) {
            if (enabled) {
                enabledTransportTypes.add(type);
            } else {
                enabledTransportTypes.remove(type);
            }
            return this;
        }

        /**
         * Enables or disables agility shortcuts.
         *
         * @param enabled true to enable agility shortcuts.
         * @return this builder.
         */
        public Builder useAgilityShortcuts(boolean enabled) {
            return enableTransport(TransportType.AGILITY_SHORTCUT, enabled);
        }

        /**
         * Enables or disables grapple shortcuts.
         *
         * @param enabled true to enable grapple shortcuts.
         * @return this builder.
         */
        public Builder useGrappleShortcuts(boolean enabled) {
            return enableTransport(TransportType.GRAPPLE_SHORTCUT, enabled);
        }

        /**
         * Enables or disables boats.
         *
         * @param enabled true to enable boats.
         * @return this builder.
         */
        public Builder useBoats(boolean enabled) {
            return enableTransport(TransportType.BOAT, enabled);
        }

        /**
         * Enables or disables canoes.
         *
         * @param enabled true to enable canoes.
         * @return this builder.
         */
        public Builder useCanoes(boolean enabled) {
            return enableTransport(TransportType.CANOE, enabled);
        }

        /**
         * Enables or disables charter ships.
         *
         * @param enabled true to enable charter ships.
         * @return this builder.
         */
        public Builder useCharterShips(boolean enabled) {
            return enableTransport(TransportType.CHARTER_SHIP, enabled);
        }

        /**
         * Enables or disables ships.
         *
         * @param enabled true to enable ships.
         * @return this builder.
         */
        public Builder useShips(boolean enabled) {
            return enableTransport(TransportType.SHIP, enabled);
        }

        /**
         * Enables or disables fairy rings.
         *
         * @param enabled true to enable fairy rings.
         * @return this builder.
         */
        public Builder useFairyRings(boolean enabled) {
            return enableTransport(TransportType.FAIRY_RING, enabled);
        }

        /**
         * Enables or disables gnome gliders.
         *
         * @param enabled true to enable gnome gliders.
         * @return this builder.
         */
        public Builder useGnomeGliders(boolean enabled) {
            return enableTransport(TransportType.GNOME_GLIDER, enabled);
        }

        /**
         * Enables or disables hot air balloons.
         *
         * @param enabled true to enable hot air balloons.
         * @return this builder.
         */
        public Builder useHotAirBalloons(boolean enabled) {
            return enableTransport(TransportType.HOT_AIR_BALLOON, enabled);
        }

        /**
         * Enables or disables magic carpets.
         *
         * @param enabled true to enable magic carpets.
         * @return this builder.
         */
        public Builder useMagicCarpets(boolean enabled) {
            return enableTransport(TransportType.MAGIC_CARPET, enabled);
        }

        /**
         * Enables or disables magic mushtrees.
         *
         * @param enabled true to enable magic mushtrees.
         * @return this builder.
         */
        public Builder useMagicMushtrees(boolean enabled) {
            return enableTransport(TransportType.MAGIC_MUSHTREE, enabled);
        }

        /**
         * Enables or disables minecarts.
         *
         * @param enabled true to enable minecarts.
         * @return this builder.
         */
        public Builder useMinecarts(boolean enabled) {
            return enableTransport(TransportType.MINECART, enabled);
        }

        /**
         * Enables or disables quetzals.
         *
         * @param enabled true to enable quetzals.
         * @return this builder.
         */
        public Builder useQuetzals(boolean enabled) {
            return enableTransport(TransportType.QUETZAL, enabled);
        }

        /**
         * Enables or disables seasonal transports.
         *
         * @param enabled true to enable seasonal transports.
         * @return this builder.
         */
        public Builder useSeasonalTransports(boolean enabled) {
            return enableTransport(TransportType.SEASONAL_TRANSPORTS, enabled);
        }

        /**
         * Enables or disables spirit trees.
         *
         * @param enabled true to enable spirit trees.
         * @return this builder.
         */
        public Builder useSpiritTrees(boolean enabled) {
            return enableTransport(TransportType.SPIRIT_TREE, enabled);
        }

        /**
         * Enables or disables teleportation items.
         *
         * @param enabled true to enable teleportation items.
         * @return this builder.
         */
        public Builder useTeleportationItems(boolean enabled) {
            return enableTransport(TransportType.TELEPORTATION_ITEM, enabled);
        }

        /**
         * Enables or disables teleportation boxes (e.g., jewellery boxes).
         *
         * @param enabled true to enable teleportation boxes.
         * @return this builder.
         */
        public Builder useTeleportationBoxes(boolean enabled) {
            return enableTransport(TransportType.TELEPORTATION_BOX, enabled);
        }

        /**
         * Enables or disables teleportation levers.
         *
         * @param enabled true to enable teleportation levers.
         * @return this builder.
         */
        public Builder useTeleportationLevers(boolean enabled) {
            return enableTransport(TransportType.TELEPORTATION_LEVER, enabled);
        }

        /**
         * Enables or disables teleportation minigames.
         *
         * @param enabled true to enable teleportation minigames.
         * @return this builder.
         */
        public Builder useTeleportationMinigames(boolean enabled) {
            return enableTransport(TransportType.TELEPORTATION_MINIGAME, enabled);
        }

        /**
         * Enables or disables teleportation portals.
         *
         * @param enabled true to enable teleportation portals.
         * @return this builder.
         */
        public Builder useTeleportationPortals(boolean enabled) {
            return enableTransport(TransportType.TELEPORTATION_PORTAL, enabled);
        }

        /**
         * Enables or disables POH teleportation portals.
         *
         * @param enabled true to enable POH teleportation portals.
         * @return this builder.
         */
        public Builder useTeleportationPortalsPoh(boolean enabled) {
            return enableTransport(TransportType.TELEPORTATION_PORTAL_POH, enabled);
        }

        /**
         * Enables or disables teleportation spells.
         *
         * @param enabled true to enable teleportation spells.
         * @return this builder.
         */
        public Builder useTeleportationSpells(boolean enabled) {
            return enableTransport(TransportType.TELEPORTATION_SPELL, enabled);
        }

        /**
         * Enables or disables wilderness obelisks.
         *
         * @param enabled true to enable wilderness obelisks.
         * @return this builder.
         */
        public Builder useWildernessObelisks(boolean enabled) {
            return enableTransport(TransportType.WILDERNESS_OBELISK, enabled);
        }

        /**
         * Builds a new immutable {@link GlobalPathfinderBuilder}.
         *
         * @return new configuration instance.
         */
        public GlobalPathfinderBuilder build() {
            return new GlobalPathfinderBuilder(this);
        }
    }
}
