package com.kraken.api.core.interaction.resolver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.interaction.MenuActionResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry that maps entity types to their {@link MenuActionResolver} implementations.
 * Resolvers are injected as a Set, so adding a new entity type means adding a new
 * resolver class — zero changes to existing code (Open/Closed Principle).
 */
@Singleton
public class MenuActionResolverRegistry {

    private final Map<Class<?>, MenuActionResolver<?>> resolvers;

    /**
     * Creates the registry, indexing each resolver by the entity type it reports.
     * @param npcResolver Resolver for NPCs.
     * @param playerResolver Resolver for players.
     * @param tileObjectResolver Resolver for tile objects.
     * @param widgetResolver Resolver for interface widgets.
     * @param groundItemResolver Resolver for ground items.
     * @param bankItemResolver Resolver for bank item widgets.
     */
    @Inject
    public MenuActionResolverRegistry(
            NpcMenuActionResolver npcResolver,
            PlayerMenuActionResolver playerResolver,
            TileObjectMenuActionResolver tileObjectResolver,
            WidgetMenuActionResolver widgetResolver,
            GroundItemMenuActionResolver groundItemResolver,
            BankItemMenuActionResolver bankItemResolver
    ) {
        List<MenuActionResolver<?>> injectedResolvers = List.of(
                npcResolver, playerResolver, tileObjectResolver,
                widgetResolver, groundItemResolver, bankItemResolver
        );

        this.resolvers = injectedResolvers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MenuActionResolver::getEntityType,
                        Function.identity()
                ));
    }

    /**
     * Looks up the resolver registered for an entity type.
     * @param entityType The entity class to resolve actions for.
     * @param <T> The entity type.
     * @return The resolver for that type, or empty when none is registered for it.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<MenuActionResolver<T>> getResolver(Class<T> entityType) {
        return Optional.ofNullable((MenuActionResolver<T>) resolvers.get(entityType));
    }
}
