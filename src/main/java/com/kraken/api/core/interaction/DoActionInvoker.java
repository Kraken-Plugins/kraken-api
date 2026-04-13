package com.kraken.api.core.interaction;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.model.PacketFactory;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Encapsulates the reflection-based invocation of the RuneLite doAction method.
 * Caches the resolved class and method after the first successful lookup.
 */
@Slf4j
@Singleton
public class DoActionInvoker {

    private volatile Method doActionMethod;  // volatile: written once, read many times
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

        int garbageValue = PacketFactory.getPacketMetadata().getDoActionGarbageValue();
        Context ctx = ctxProvider.get();

        try {
            doActionMethod.setAccessible(true);
            ctx.runOnClientThreadOptional(() ->
                    doActionMethod.invoke(null, param0, param1, opcode, identifier,
                            itemId, worldViewId, option, target, canvasX, canvasY, garbageValue)
            );
        } catch (Exception e) {
            log.error("Failed to invoke doAction via reflection", e);
        } finally {
            doActionMethod.setAccessible(false);
        }
    }

    private void ensureMethodLoaded() {
        if (doActionMethod != null) return;
        synchronized (lock) {
            if (doActionMethod != null) return;  // double-checked locking
            try {
                Client client = ctxProvider.get().getClient();
                String className  = PacketFactory.getPacketMetadata().getDoActionClassName();
                String methodName = PacketFactory.getPacketMetadata().getDoActionMethodName();
                Class<?> clazz = client.getClass().getClassLoader().loadClass(className);

                doActionMethod = Arrays.stream(clazz.getDeclaredMethods())
                        .filter(m -> m.getName().equalsIgnoreCase(methodName))
                        .findFirst()
                        .orElse(null);

                if (doActionMethod == null) {
                    log.error("Could not find doAction method '{}' on class '{}'", methodName, className);
                }
            } catch (ClassNotFoundException e) {
                log.error("Could not load doAction class", e);
            }
        }
    }
}
