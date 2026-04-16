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
     * Loads the {@code HookRegistry} from the predefined JSON resource file.
     * <p>
     * If the {@code HookRegistry} has already been loaded and cached, this method
     * returns the cached instance. Otherwise, it deserializes the resource file
     * located at {@code /reflection_hooks.json} into a {@code HookRegistry} instance
     * using a {@link Gson} object configured with a custom {@link HookRegistryDeserializer}.
     * <p>
     * In case of an error during the loading or deserialization process, {@code null} is returned, and
     * an appropriate error message is logged.
     *
     * @return the loaded {@link HookRegistry} instance, or {@code null} if an error occurs
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
