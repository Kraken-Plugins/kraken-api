package com.kraken.api.core.hooks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.util.JsonResourceUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HooksLoader {

    @Getter
    private static GameHooks gameHooks = null;

    @Getter
    private static Map<String, PacketDefinition> packets = new HashMap<>();

    @Getter
    private static ReflectionHooks reflectionHooks = null;

    @Getter
    private static LoginHooks loginHooks = null;

    @Getter
    private static SecurityHooks securityHooks = null;

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
            log.error("Exception while trying to load hooks.json.", e);
        }
    }
}
