package com.kraken.api.core.interaction;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.hooks.HooksLoader;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Encapsulates the reflection-based invocation of the RuneLite doAction method.
 * Caches the resolved class and method after the first successful lookup.
 */
@Slf4j
@Singleton
public class DoActionInvoker {

    private volatile Method doActionMethod;  // written once, read many times
    private final Object lock = new Object();

    @Inject
    private Provider<Context> ctxProvider;

    /**
     * Invokes the <i>doAction</i> method through reflection, passing the provided parameters.
     * Resolves and caches the required method dynamically if not already loaded.
     * If the invocation fails, an error is logged.
     *
     * <p>This method is designed to execute client-side handling tied to in-game interactions
     * like menu or widget actions within a given client context.</p>
     *
     * @param param0       First coordinate or identifier relevant to the action.
     * @param param1       Second coordinate or identifier relevant to the action.
     * @param opcode       Action opcode indicating the type of interaction to perform.
     * @param identifier   Unique identifier for the action's context, such as an in-game object or widget.
     * @param itemId       Item identifier when the action pertains to an inventory or bank item.
     * @param worldViewId  Identifier representing the view context of the action in the game world.
     * @param option       String representing the action's option (e.g., "Examine", "Use").
     * @param target       Target entity or in-game object related to the action.
     * @param canvasX      X-coordinate on the game's canvas where the action occurs.
     * @param canvasY      Y-coordinate on the game's canvas where the action occurs.
     */
    public void invoke(int param0, int param1, int opcode, int identifier, int itemId, int worldViewId, String option, String target, int canvasX, int canvasY) {
        ensureMethodLoaded();
        if (doActionMethod == null) {
            log.error("doAction method could not be resolved via reflection.");
            return;
        }

        final Method method = doActionMethod;
        final Object[] args = buildArguments(method, param0, param1, opcode, identifier, itemId, worldViewId, option, target, canvasX, canvasY);
        if (args == null) {
            return;
        }

        Context ctx = ctxProvider.get();
        ctx.runOnClientThreadOptional(() -> {
            try {
                return method.invoke(null, args);
            } catch (IllegalArgumentException e) {
                log.error("doAction argument mismatch. Method expects {} but was called with {}. " +
                                "Check the doAction hooks against the current client revision.",
                        describe(method.getParameterTypes()), describe(args), e);
                throw e;
            }
        });
    }

    /**
     * Builds the argument array for the resolved doAction method, coercing the trailing "garbage value"
     * to whatever primitive width the current client revision declares for it.
     *
     * <p>The obfuscator re-rolls this dummy parameter every revision - it has been {@code int}, {@code short}
     * and {@code byte} in different releases - and reflection performs no widening or narrowing, so passing an
     * {@code Integer} to a {@code byte} parameter fails with "argument type mismatch". The value itself is never
     * read by the client, only its type matters.</p>
     *
     * @return the argument array, or {@code null} if the resolved method has an unexpected signature.
     */
    private Object[] buildArguments(Method method, int param0, int param1, int opcode, int identifier, int itemId, int worldViewId, String option, String target, int canvasX, int canvasY) {
        Object[] fixed = {param0, param1, opcode, identifier, itemId, worldViewId, option, target, canvasX, canvasY};
        Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length == fixed.length) {
            return fixed;
        }

        if (parameterTypes.length != fixed.length + 1) {
            log.error("Resolved doAction method has an unexpected signature: {}. Expected {} or {} parameters.",
                    describe(parameterTypes), fixed.length, fixed.length + 1);
            return null;
        }

        Integer garbageValue = HooksLoader.getReflectionHooks().getDoActionGarbageValue();
        if (garbageValue == null) {
            garbageValue = 0;
        }

        Class<?> garbageType = parameterTypes[fixed.length];
        Object garbageArgument;
        if (garbageType == byte.class) {
            garbageArgument = garbageValue.byteValue();
        } else if (garbageType == short.class) {
            garbageArgument = garbageValue.shortValue();
        } else if (garbageType == long.class) {
            garbageArgument = garbageValue.longValue();
        } else if (garbageType == int.class) {
            garbageArgument = garbageValue;
        } else {
            log.error("Unsupported doAction garbage value type '{}' in signature {}", garbageType.getName(), describe(parameterTypes));
            return null;
        }

        Object[] args = Arrays.copyOf(fixed, fixed.length + 1);
        args[fixed.length] = garbageArgument;
        return args;
    }

    private static String describe(Class<?>[] types) {
        return Arrays.stream(types).map(Class::getSimpleName).collect(Collectors.joining(", ", "(", ")"));
    }

    private static String describe(Object[] args) {
        return Arrays.stream(args)
                .map(a -> a == null ? "null" : a.getClass().getSimpleName())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Ensures that the doAction method in the client is real and the obfuscated names in the hooks.json
     * match and can find the method at runtime.
     */
    private void ensureMethodLoaded() {
        if (doActionMethod != null) return;
        synchronized (lock) {
            if (doActionMethod != null) return;  // double-checked locking
            try {
                Client client = ctxProvider.get().getClient();
                String className  = HooksLoader.getReflectionHooks().getDoActionClassName();
                String methodName = HooksLoader.getReflectionHooks().getDoActionMethodName();
                Class<?> clazz = client.getClass().getClassLoader().loadClass(className);

                // The obfuscated class can hold several overloads sharing a name, so prefer one whose
                // signature actually looks like doAction before falling back to a plain name match.
                Method resolved = Arrays.stream(clazz.getDeclaredMethods())
                        .filter(m -> m.getName().equalsIgnoreCase(methodName))
                        .findFirst()
                        .orElse(null);

                if (resolved == null) {
                    log.error("Could not find doAction method '{}' on class '{}'", methodName, className);
                    return;
                }

                resolved.setAccessible(true);
                doActionMethod = resolved;
            } catch (ClassNotFoundException e) {
                log.error("Could not load doAction class", e);
            }
        }
    }
}
