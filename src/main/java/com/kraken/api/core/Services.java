package com.kraken.api.core;

import com.google.inject.Injector;
import com.kraken.api.Context;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.client.RuneLite;

/**
 * Lazy access to injector-provided singletons for the API's static utility classes.
 *
 * <p>Resolution happens on first use rather than at class-initialization time, and a failed lookup is
 * not cached. A class that asks for a service before RuneLite's injector exists therefore gets an
 * {@link IllegalStateException} it can recover from on a later call, instead of an
 * {@code ExceptionInInitializerError} that leaves the class unusable for the rest of the JVM's life.</p>
 *
 * <p>This resolves against the root injector. Types that plugins inject normally are per-plugin, so a
 * static utility reached through here and an injected field of the same type are not guaranteed to be
 * the same object. Prefer constructor or field injection wherever a class can accept it.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Services {

    private static volatile Context context;

    /**
     * Returns the root-injector {@link Context}, resolving it on first use.
     *
     * @return the shared Context instance.
     * @throws IllegalStateException if RuneLite's injector is not available yet.
     */
    public static Context context() {
        Context local = context;
        if (local == null) {
            synchronized (Services.class) {
                local = context;
                if (local == null) {
                    context = local = get(Context.class);
                }
            }
        }
        return local;
    }

    /**
     * Resolves any injector-provided type on demand.
     *
     * @param type The class to resolve.
     * @param <T> The type to resolve.
     * @return the instance supplied by RuneLite's injector.
     * @throws IllegalStateException if RuneLite's injector is not available yet.
     */
    public static <T> T get(Class<T> type) {
        Injector injector = RuneLite.getInjector();
        if (injector == null) {
            throw new IllegalStateException("RuneLite's injector is not available yet; "
                    + type.getSimpleName() + " cannot be resolved before the client has started");
        }
        return injector.getInstance(type);
    }
}
