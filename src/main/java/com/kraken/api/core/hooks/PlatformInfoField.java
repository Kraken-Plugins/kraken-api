package com.kraken.api.core.hooks;

import lombok.Value;

/**
 * One field the client's PlatformInfo write method sends in the login packet, parsed from {@code hooks.json}.
 * {@link SecurityHooks} lists them in {@code platformInfoFields} in the order the method sends them.
 */
@Value
public class PlatformInfoField {
    /**
     * The protocol name of the field RuneLite sends a {@code -javaagent} jar name in, which Kraken-Client's agent patch
     * replaces.
     */
    public static final String DEVICE_NAME = "DeviceName";

    /**
     * The protocol name of the field RuneLite sends the client's call stack in, which Kraken-Client's call stack patch
     * replaces.
     */
    public static final String CLIENT_NAME = "ClientName";

    /**
     * The protocol name of what the field is sent as, such as {@code OsType} or {@link #DEVICE_NAME}.
     */
    String name;

    /**
     * The obfuscated instance field of the PlatformInfo class holding the value.
     */
    String fieldName;

    /**
     * The constant the stored int is multiplied by to get the value sent, or {@code null} for a field that is not
     * multiplied.
     */
    Integer multiplier;
}
