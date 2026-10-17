package com.kraken.api.core.hooks;

import lombok.Value;

import java.util.List;

/**
 * Security-related obfuscation hooks parsed from {@code hooks.json}. Gson allocates instances via
 * {@code Unsafe} without running any field initializers, so every value here comes from the JSON
 * resource — including {@code mouseHookDllClassName} and {@code callStackClassName}, which must be
 * present in the resource rather than defaulted in code.
 */
@Value
public class SecurityHooks {
    String mouseHookDllClassName;
    String mouseHookDllMethodName;
    String clientLogFieldName;
    String cleanCallStackValue;
    String platformInfoClassName;
    String platformInfoMethodName;
    boolean isPlatformInfoMethodStatic;
    int platformInfoMethodArgCount;

    /**
     * Every field the PlatformInfo write method sends, in send order, or {@code null} when {@code hooks.json} was
     * mapped before kraken-updater listed them.
     */
    List<PlatformInfoField> platformInfoFields;

    /**
     * Finds the obfuscated PlatformInfo field sent under a protocol name.
     *
     * @param name the protocol name, such as {@link PlatformInfoField#DEVICE_NAME}
     * @return the field's obfuscated name, or {@code null} when {@code hooks.json} does not list it
     */
    public String getPlatformInfoFieldName(String name) {
        if (platformInfoFields == null) {
            return null;
        }
        return platformInfoFields.stream()
                .filter(field -> name.equals(field.getName()))
                .map(PlatformInfoField::getFieldName)
                .findFirst()
                .orElse(null);
    }
}

