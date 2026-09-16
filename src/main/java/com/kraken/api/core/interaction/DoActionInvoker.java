package com.kraken.api.core.interaction;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.hooks.HookResolver;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.ReflectionHooks;
import com.kraken.api.util.GarbageValueUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Encapsulates the reflection-based invocation of the RuneLite doAction method.
 * Caches the resolved method after the first successful lookup; failed lookups are retried on the next call.
 */
@Slf4j
@Singleton
public class DoActionInvoker {

    /** The engine's ten fixed parameters; a revision may append one primitive garbage parameter. */
    private static final Class<?>[] FIXED_PARAMETER_TYPES = {
            int.class, int.class, int.class, int.class, int.class, int.class,
            String.class, String.class, int.class, int.class};

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
     * @return true if the engine call was made, false if the hooks could not be resolved or the
     *         invocation failed.
     */
    public boolean invoke(int param0, int param1, int opcode, int identifier, int itemId, int worldViewId, String option, String target, int canvasX, int canvasY) {
        ensureMethodLoaded();
        if (doActionMethod == null) {
            log.error("doAction method could not be resolved via reflection.");
            return false;
        }

        final Method method = doActionMethod;
        final Object[] args = buildArguments(method, param0, param1, opcode, identifier, itemId, worldViewId, option, target, canvasX, canvasY);

        Context ctx = ctxProvider.get();
        // doAction is void, so its own return value cannot signal success — the sentinel below is
        // only reached when the reflective call completed without throwing.
        return ctx.runOnClientThreadOptional(() -> {
            try {
                method.invoke(null, args);
                return Boolean.TRUE;
            } catch (IllegalArgumentException e) {
                log.error("doAction argument mismatch. Method expects {} but was called with {}. " +
                                "Check the doAction hooks against the current client revision.",
                        describe(method.getParameterTypes()), describe(args), e);
                throw e;
            }
        }).orElse(Boolean.FALSE);
    }

    /**
     * Builds the argument array for the resolved doAction method, appending the configured garbage
     * value at the primitive width the method declares for it.
     *
     * <p>The obfuscator re-rolls this dummy parameter every revision - it has been {@code int}, {@code short}
     * and {@code byte} in different releases - and reflection performs no widening or narrowing, so the value
     * is boxed at the declared width. Obfuscated methods may compare the value against constants, so it is
     * passed verbatim from the hooks and never substituted.</p>
     *
     * @return the argument array matching the resolved method's parameter count.
     */
    private Object[] buildArguments(Method method, int param0, int param1, int opcode, int identifier, int itemId, int worldViewId, String option, String target, int canvasX, int canvasY) {
        Object[] fixed = {param0, param1, opcode, identifier, itemId, worldViewId, option, target, canvasX, canvasY};
        Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length == fixed.length) {
            return fixed;
        }

        Object[] args = Arrays.copyOf(fixed, fixed.length + 1);
        args[fixed.length] = GarbageValueUtils.coerceToParameterType(parameterTypes[fixed.length], HooksLoader.getReflectionHooks().getDoActionGarbageValue());
        return args;
    }

    /**
     * Reports whether a declared method has the engine's shape: static, the ten fixed parameter
     * types in order, and optionally one trailing primitive garbage parameter.
     *
     * @param method The candidate declared on the hooked class.
     * @return True if the method can be invoked as doAction.
     */
    static boolean isDoActionSignature(Method method) {
        Class<?>[] types = method.getParameterTypes();
        int fixed = FIXED_PARAMETER_TYPES.length;
        return Modifier.isStatic(method.getModifiers())
                && (types.length == fixed || types.length == fixed + 1)
                && Arrays.equals(Arrays.copyOf(types, fixed), FIXED_PARAMETER_TYPES)
                && (types.length == fixed || GarbageValueUtils.isSupportedParameterType(types[fixed]));
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
     * Resolves the hooked doAction method once, requiring exactly one declared method with the
     * engine's signature and a configured garbage value whenever the signature declares one.
     * Nothing is cached when resolution fails, so a later call retries.
     */
    private void ensureMethodLoaded() {
        if (doActionMethod != null) return;
        synchronized (lock) {
            if (doActionMethod != null) return;  // double-checked locking
            ReflectionHooks hooks = HooksLoader.getReflectionHooks();
            try {
                Client client = ctxProvider.get().getClient();
                Class<?> clazz = client.getClass().getClassLoader().loadClass(hooks.getDoActionClassName());
                Method resolved = HookResolver.requireUnique(clazz, hooks.getDoActionMethodName(), DoActionInvoker::isDoActionSignature);

                if (resolved.getParameterCount() > FIXED_PARAMETER_TYPES.length && hooks.getDoActionGarbageValue() == null) {
                    throw new IllegalStateException("doAction declares a garbage parameter but hooks.json has no doActionGarbageValue");
                }

                doActionMethod = resolved;
            } catch (ClassNotFoundException | IllegalStateException e) {
                log.error("Could not resolve doAction hook {}.{}", hooks.getDoActionClassName(), hooks.getDoActionMethodName(), e);
            }
        }
    }
}
