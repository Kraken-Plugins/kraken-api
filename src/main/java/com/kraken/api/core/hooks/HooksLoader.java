package com.kraken.api.core.hooks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.util.JsonResourceUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Loads {@code hooks.json} once at class initialization and exposes the parsed hook groups.
 * <p>
 * The static initializer never throws — a throwing initializer would poison the class with
 * {@code NoClassDefFoundError} for the life of the JVM. Instead a load failure is recorded and
 * every accessor throws an {@link IllegalStateException} that names the real cause, so the first
 * code path that needs hooks fails with an actionable message rather than a {@code NullPointerException}
 * deep inside reflection code.
 */
@Slf4j
@Singleton
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HooksLoader {

    private static GameHooks gameHooks;

    private static Map<String, PacketDefinition> packets;

    private static ReflectionHooks reflectionHooks;

    private static LoginHooks loginHooks;

    private static SecurityHooks securityHooks;

    private static Exception loadFailure;

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    static {
        try {
            gameHooks = JsonResourceUtils.loadJsonResource(
                    HooksLoader.class,
                    "/hooks.json",
                    gson,
                    GameHooks.class
            );
            if (gameHooks.getPackets() == null || gameHooks.getLoginHooks() == null || gameHooks.getReflectionHooks() == null || gameHooks.getSecurityHooks() == null) {
                throw new IllegalStateException("Parsed hooks file was null.");
            }

            packets = gameHooks.getPackets();
            reflectionHooks = gameHooks.getReflectionHooks();
            securityHooks = gameHooks.getSecurityHooks();
            loginHooks = gameHooks.getLoginHooks();
            log.info("Loaded packet, reflection, security and login hooks from local resources.");
        } catch (Exception e) {
            loadFailure = e;
            log.error("Exception while trying to load hooks.json.", e);
        }
    }

    /**
     * Verifies that hooks.json loaded successfully.
     *
     * @throws IllegalStateException If loading failed, carrying the original failure as its cause.
     */
    public static void requireLoaded() {
        if (loadFailure != null) {
            throw new IllegalStateException(
                    "hooks.json failed to load, so the Kraken API cannot resolve client internals or send packets. "
                            + "Fix the hooks resource before using the API. Original failure: " + loadFailure.getMessage(),
                    loadFailure);
        }
    }

    /**
     * Returns the complete parsed hooks file.
     *
     * @return The parsed {@link GameHooks}.
     * @throws IllegalStateException If hooks.json failed to load.
     */
    public static GameHooks getGameHooks() {
        requireLoaded();
        return gameHooks;
    }

    /**
     * Returns the packet structure definitions keyed by packet name.
     *
     * @return The parsed packet definitions.
     * @throws IllegalStateException If hooks.json failed to load.
     */
    public static Map<String, PacketDefinition> getPackets() {
        requireLoaded();
        return packets;
    }

    /**
     * Returns the reflection hooks used by the packet and interaction layers.
     *
     * @return The parsed {@link ReflectionHooks}.
     * @throws IllegalStateException If hooks.json failed to load.
     */
    public static ReflectionHooks getReflectionHooks() {
        requireLoaded();
        return reflectionHooks;
    }

    /**
     * Returns the login hooks.
     *
     * @return The parsed {@link LoginHooks}.
     * @throws IllegalStateException If hooks.json failed to load.
     */
    public static LoginHooks getLoginHooks() {
        requireLoaded();
        return loginHooks;
    }

    /**
     * Returns the security hooks.
     *
     * @return The parsed {@link SecurityHooks}.
     * @throws IllegalStateException If hooks.json failed to load.
     */
    public static SecurityHooks getSecurityHooks() {
        requireLoaded();
        return securityHooks;
    }
}
