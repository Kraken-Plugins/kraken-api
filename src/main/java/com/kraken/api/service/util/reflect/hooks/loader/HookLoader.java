package com.kraken.api.service.util.reflect.hooks.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kraken.api.service.util.reflect.hooks.HookRegistry;
import com.kraken.api.util.JsonResourceUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HookLoader {

    private static final String LOCAL_REFLECTION_HOOKS_PATH = "/reflection_hooks.json";
    private static HookRegistry registryCache = null;

    /**
     * Initializes the packet factory by loading packet definitions from local resources or a remote source.
     * <p>
     * This method attempts to load a JSON file containing packet definitions from a predefined
     * local path. If the local file is unavailable or an exception occurs while processing it,
     * the method falls back to retrieving the packet definitions from a remote URL.
     * </p>
     */
    public static HookRegistry load() {
        if(registryCache != null) {
            return registryCache;
        }

        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(HookRegistry.class, new HookRegistryDeserializer())
                    .create();

            HookRegistry registry = JsonResourceUtils.loadJsonResource(
                    HookLoader.class,
                    LOCAL_REFLECTION_HOOKS_PATH,
                    gson,
                    HookRegistry.class
            );
            registryCache = registry;
            log.info("Loaded reflection_hooks.json.");
            return registry;
        } catch (Exception e) {
            log.error("Exception while trying to load reflection_hooks.json file: ", e);
            return null;
        }
    }
}
