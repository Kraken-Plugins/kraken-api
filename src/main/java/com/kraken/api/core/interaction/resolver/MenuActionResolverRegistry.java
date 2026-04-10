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

    @SuppressWarnings("unchecked")
    public <T> Optional<MenuActionResolver<T>> getResolver(Class<T> entityType) {
        return Optional.ofNullable((MenuActionResolver<T>) resolvers.get(entityType));
    }
}
