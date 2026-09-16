package com.kraken.api.core.hooks;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Resolves obfuscated client methods by exact name and structural signature.
 * <p>
 * Obfuscated classes routinely reuse one short name across several overloads, and reflection
 * enumerates declared methods in no defined order, so a name match alone cannot identify the
 * hooked method. Callers describe the expected shape and the resolver refuses to guess whenever
 * zero or several candidates fit it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HookResolver {

    /**
     * Returns the one declared method on {@code owner} whose name equals {@code name} exactly
     * and whose declaration satisfies {@code signature}, made accessible for invocation.
     *
     * @param owner     The class to search. Only methods declared directly on it are considered.
     * @param name      The exact, case-sensitive obfuscated method name from the hooks.
     * @param signature Structural predicate on each candidate, such as parameter types, modifiers
     *                  or return type.
     * @return The single matching method with its accessible flag set.
     * @throws IllegalStateException If no method matches or more than one does. The message lists
     *                               every candidate that passed the predicate.
     */
    public static Method requireUnique(Class<?> owner, String name, Predicate<Method> signature) {
        List<Method> matches = Arrays.stream(owner.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .filter(signature)
                .collect(Collectors.toList());

        if (matches.size() != 1) {
            throw new IllegalStateException(String.format(
                    "Expected exactly one %s.%s matching the hooked signature but found %d: %s",
                    owner.getName(), name, matches.size(), matches));
        }

        Method method = matches.get(0);
        method.setAccessible(true);
        return method;
    }
}
